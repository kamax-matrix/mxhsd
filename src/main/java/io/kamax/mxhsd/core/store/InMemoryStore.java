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

import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IProcessedEvent;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.store.IStore;
import io.kamax.mxhsd.core.event.ProcessedEvent;
import io.kamax.mxhsd.core.store.dao.RoomDao;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryStore implements IStore {

    private Map<String, String> rooms = new ConcurrentHashMap<>();
    private Map<String, IProcessedEvent> events = new ConcurrentHashMap<>();
    private Map<String, List<String>> extremities = new ConcurrentHashMap<>();
    private Map<String, IRoomState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<IProcessedEvent> findEvent(String id) {
        return Optional.ofNullable(events.get(id));
    }

    @Override
    public synchronized IProcessedEvent putEvent(IEvent event) {
        IProcessedEvent pEv = new ProcessedEvent((long) events.size(), event);
        events.put(pEv.getId(), pEv);
        return pEv;
    }

    private RoomDao makeRoom(String id) {
        RoomDao dao = new RoomDao();
        dao.setId(id);
        dao.setExtremities(extremities.getOrDefault(id, Collections.emptyList()));
        return dao;
    }

    @Override
    public List<RoomDao> listRooms() {
        return rooms.keySet().stream().map(this::makeRoom).collect(Collectors.toList());
    }

    @Override
    public Optional<RoomDao> findRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId)).map(this::makeRoom);
    }

    @Override
    public void putRoom(RoomDao room) {
        if (rooms.containsKey(room.getId())) {
            throw new RuntimeException();
        }

        rooms.put(room.getId(), room.getId());
        extremities.put(room.getId(), new ArrayList<>(room.getExtremities()));
    }

    @Override
    public void addExtremityOfRoom(String roomId, List<String> eventIds) {
        throw new NotImplementedException(InMemoryStore.class.getName());
    }

    @Override
    public void removeExtremityOfRoom(String roomId, List<String> eventIds) {
        throw new NotImplementedException(InMemoryStore.class.getName());
    }

    @Override
    public Optional<IRoomState> findRoomState(String roomId, String eventId) {
        return Optional.ofNullable(states.get(roomId + eventId));
    }

    @Override
    public void putRoomState(IRoomState state, IProcessedEvent event) {
        states.put(event.getRoomId() + event.getId(), state);
    }

}
