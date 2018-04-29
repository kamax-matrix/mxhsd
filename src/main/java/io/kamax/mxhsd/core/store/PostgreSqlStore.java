/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxhsd.core.store;

import com.google.gson.JsonArray;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IProcessedEvent;
import io.kamax.mxhsd.api.exception.NotFoundException;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.store.IStore;
import io.kamax.mxhsd.core.event.ProcessedEvent;
import io.kamax.mxhsd.core.room.RoomState;
import io.kamax.mxhsd.core.store.dao.RoomDao;
import io.kamax.mxhsd.spring.common.config.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class PostgreSqlStore implements IStore {

    private static final String putEventSql = "INSERT INTO pdus (" +
            EventKey.RoomId + "," +
            EventKey.Id + "," +
            "json) VALUES (?,?,?::jsonb) ON CONFLICT DO NOTHING";
    private static final String getEventSql = "SELECT * FROM pdus WHERE " + EventKey.Id + " = ?";
    private static final String listRoomsSql = "SELECT DISTINCT " + EventKey.RoomId + " from pdus";
    private static final String putRoomSql = "INSERT INTO rooms ( " + EventKey.RoomId + ") VALUES (?)";
    private static final String getRoomSql = listRoomsSql + " WHERE " + EventKey.RoomId + " = ?";
    private static final String getRoomExtremitiesSql = "select * from pdus where " + EventKey.RoomId + " = ? order by json->'depth' desc limit 1";
    private static final String putRoomStateSql = "INSERT INTO room_states (" +
            EventKey.RoomId + "," +
            EventKey.Id + ",json) VALUES (?,?,?::jsonb) ON CONFLICT DO NOTHING";
    private static final String getRoomStateSql = "SELECT * FROM room_states WHERE " + EventKey.RoomId + " = ? AND " + EventKey.Id + " = ?";

    private static final String getLastEventSidSql = "SELECT MAX(id) as id FROM pdus";
    private static final String getEventBySidSql = "SELECT * from pdus WHERE id = ?";

    private static final String putFilterSql = "INSERT INTO user_filters (user_id, json) VALUES (?,?::jsonb) RETURNING id";
    private static final String getFilterSql = "SELECT * FROM user_filters WHERE user_id = ? and id = ?";

    public interface SqlFunction<T, R> {

        R run(T connection) throws SQLException;

    }

    public interface SqlConsumer<T> {

        void run(T conn) throws SQLException;

    }

    private final Logger log = LoggerFactory.getLogger(PostgreSqlStore.class);

    private SqlConnectionPool pool;

    public PostgreSqlStore(StorageConfig cfg) {
        this.pool = new SqlConnectionPool(cfg);
        log.info("Connecting...");
        withConnConsumer(conn -> conn.isValid(1000));
        log.info("Connected");

    }

    private <T> T withConnFunction(SqlFunction<Connection, T> function) {
        try (Connection conn = pool.get()) {
            return function.run(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void withConnConsumer(SqlConsumer<Connection> consumer) {
        try (Connection conn = pool.get()) {
            consumer.run(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ProcessedEvent makeEvent(ResultSet rSet) throws SQLException {
        return new ProcessedEvent(rSet.getLong("id"), rSet.getString("json"));
    }

    @Override
    public Long getCurrentStreamId() {
        return withConnFunction(conn -> {
            ResultSet rSet = conn.prepareStatement(getLastEventSidSql).executeQuery();
            if (!rSet.next()) {
                return 0L;
            } else {
                return rSet.getLong("id");
            }
        });
    }

    @Override
    public IProcessedEvent getEventAtStreamId(long streamId) {
        return withConnFunction(conn -> {
            PreparedStatement stmt = conn.prepareStatement(getEventBySidSql);
            stmt.setLong(1, streamId);
            ResultSet rSet = stmt.executeQuery();
            if (!rSet.next()) {
                throw new NotFoundException("Stream ID " + streamId);
            }

            return makeEvent(rSet);
        });
    }

    @Override
    public Optional<IProcessedEvent> findEvent(String id) {
        return withConnFunction(conn -> {
            PreparedStatement stmt = conn.prepareStatement(getEventSql);
            stmt.setString(1, id);
            ResultSet rSet = stmt.executeQuery();
            if (!rSet.next()) {
                return Optional.empty();
            }

            return Optional.of(makeEvent(rSet));
        });
    }

    @Override
    public IProcessedEvent putEvent(IEvent event) {
        return withConnFunction(conn -> {
            PreparedStatement stmt = conn.prepareStatement(putEventSql);
            stmt.setString(1, event.getRoomId());
            stmt.setString(2, event.getId());
            stmt.setString(3, GsonUtil.get().toJson(event.getJson()));

            int rows = stmt.executeUpdate();
            log.debug("Inserted rows: {}", rows);

            return findEvent(event.getId()).orElseThrow(RuntimeException::new);
        });
    }

    private RoomDao makeRoom(Connection conn, ResultSet rSet) throws SQLException {
        RoomDao room = new RoomDao();
        room.setId(rSet.getString(EventKey.RoomId.get()));

        PreparedStatement stmtEx = conn.prepareStatement(getRoomExtremitiesSql);
        stmtEx.setString(1, room.getId());

        ResultSet rSetExtremities = stmtEx.executeQuery();
        if (rSetExtremities.next()) {
            room.setExtremities(Collections.singletonList(rSetExtremities.getString(EventKey.Id.get())));
        }

        return room;
    }

    @Override
    public List<RoomDao> listRooms() {
        return withConnFunction(conn -> {
            List<RoomDao> rooms = new ArrayList<>();

            PreparedStatement stmt = conn.prepareStatement(listRoomsSql);
            ResultSet rSet = stmt.executeQuery();
            while (rSet.next()) {
                rooms.add(makeRoom(conn, rSet));
            }

            return rooms;
        });
    }

    @Override
    public Optional<RoomDao> findRoom(String roomId) {
        return withConnFunction(conn -> {
            PreparedStatement stmt = conn.prepareStatement(getRoomSql);
            stmt.setString(1, roomId);
            ResultSet rSet = stmt.executeQuery();
            if (!rSet.next()) {
                return Optional.empty();
            }

            return Optional.of(makeRoom(conn, rSet));
        });
    }

    @Override
    public void putRoom(RoomDao room) {
        withConnConsumer(conn -> {
            PreparedStatement stmt = conn.prepareStatement(putRoomSql);
            stmt.setString(1, room.getId());
            int rows = stmt.executeUpdate();
            if (rows != 1) {
                throw new RuntimeException("Unexpected row count: " + rows);
            }
        });
    }

    @Override
    public Optional<IRoomState> findRoomState(String roomId, String eventId) {
        return withConnFunction(conn -> {
            PreparedStatement stmt = conn.prepareStatement(getRoomStateSql);
            stmt.setString(1, roomId);
            stmt.setString(2, eventId);

            ResultSet rSet = stmt.executeQuery();
            if (!rSet.next()) {
                return Optional.empty();
            }

            RoomState.Builder builder = RoomState.build();
            JsonArray events = GsonUtil.parse(rSet.getString("json")).getAsJsonArray();
            try {
                StreamSupport.stream(events.spliterator(), false)
                        .map(id -> findEvent(id.getAsString()).orElseThrow(() -> new NotFoundException("Event " + id)))
                        .forEach(builder::addEvent);
            } catch (NotFoundException e) {
                log.warn("Unable to rebuild state for {}-{}: {}", roomId, eventId, e.getMessage());
                return Optional.empty();
            }

            return Optional.of(builder.get());
        });
    }

    @Override
    public void putRoomState(String roomId, String eventId, IRoomState state) {
        withConnConsumer(conn -> {
            JsonArray list = new JsonArray();
            state.getEvents().values().forEach(ev -> list.add(ev.getId()));
            PreparedStatement stmt = conn.prepareStatement(putRoomStateSql);
            stmt.setString(1, roomId);
            stmt.setString(2, eventId);
            stmt.setString(3, GsonUtil.get().toJson(list));

            int rows = stmt.executeUpdate();
            if (rows != 1) {
                log.warn("Unexpected row count when inserting room state {}-{}: {}", roomId, eventId, rows);
            }
        });
    }

    @Override
    public String putFilter(String userId, String filter) {
        return withConnFunction(conn -> {
            PreparedStatement stmt = conn.prepareStatement(putFilterSql);
            stmt.setString(1, userId);
            stmt.setString(2, filter);

            ResultSet rSet = stmt.executeQuery();
            if (!rSet.next()) {
                throw new RuntimeException("Unable to insert filter of user " + userId);
            }

            return Long.toString(rSet.getLong(1));
        });
    }

    @Override
    public Optional<String> findFilter(String userId, String filterId) {
        long fId = Long.parseLong(filterId);
        return withConnFunction(conn -> {
            PreparedStatement stmt = conn.prepareStatement(getFilterSql);
            stmt.setString(1, userId);
            stmt.setLong(2, fId);

            ResultSet rSet = stmt.executeQuery();
            if (!rSet.next()) {
                return Optional.empty();
            }

            String json = rSet.getString("json");
            return Optional.of(json);
        });
    }

}
