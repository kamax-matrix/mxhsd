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

import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IProcessedEvent;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.store.IStore;
import io.kamax.mxhsd.core.event.ProcessedEvent;
import io.kamax.mxhsd.spring.common.config.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PostgreSqlStore implements IStore {

    private static final String putEventSql = "INSERT INTO pdus (" +
            EventKey.RoomId + "," +
            EventKey.Id + "," +
            "json) VALUES (?,?,?::jsonb)";

    public interface SqlFunction<T, R> {

        R run(T connection) throws SQLException;

    }

    private final Logger log = LoggerFactory.getLogger(PostgreSqlStore.class);

    private SqlConnectionPool pool;

    public PostgreSqlStore(StorageConfig cfg) {
        this.pool = new SqlConnectionPool(cfg);
        log.info("Connecting...");
        withConn(conn -> conn.isValid(1000));
        log.info("Connected");

    }

    private <T> T withConn(SqlFunction<Connection, T> function) {
        try (Connection conn = pool.get()) {
            return function.run(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<IProcessedEvent> findEvent(String id) {
        return withConn(conn -> {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM pdus WHERE " + EventKey.Id + " = ?");
            stmt.setString(1, id);
            try (ResultSet rSet = stmt.executeQuery()) {
                if (!rSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(new ProcessedEvent(rSet.getString("id"), rSet.getString("json")));
            }
        });
    }

    @Override
    public IProcessedEvent putEvent(IEvent event) {
        return withConn(conn -> {
            PreparedStatement stmt = conn.prepareStatement(putEventSql);
            stmt.setString(1, event.getRoomId());
            stmt.setString(2, event.getId());
            stmt.setString(3, GsonUtil.get().toJson(event.getJson()));

            int rows = stmt.executeUpdate();
            if (rows != 1) {
                throw new RuntimeException("Unexpected row count: " + rows);
            }

            return findEvent(event.getId()).orElseThrow(RuntimeException::new);
        });
    }

    @Override
    public void findRoomState(String eventId) {

    }

    @Override
    public void putRoomState(IRoomState state, IProcessedEvent event) {

    }

}
