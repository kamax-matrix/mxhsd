/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Maxime Dor
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

package io.kamax.mxhsd.core.room;

import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomCreateOptions;
import io.kamax.mxhsd.api.room.IRoomManager;
import io.kamax.mxhsd.core.HomeserverState;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RoomManager implements IRoomManager {

    private Logger log = LoggerFactory.getLogger(RoomManager.class);

    private HomeserverState state;
    private Map<String, IRoom> rooms;

    public RoomManager(HomeserverState state) {
        this.state = state;
        rooms = new HashMap<>();
    }

    private boolean hasRoom(String id) {
        return rooms.containsKey(id);
    }

    private String getId() {
        String id;
        do {
            id = "!" + RandomStringUtils.randomAlphanumeric(16) + ":" + state.getDomain();
        } while (hasRoom(id));

        log.info("Generated Room ID {}", id);
        return id;
    }

    @Override
    public synchronized IRoom createRoom(IRoomCreateOptions options) { // FIXME use RWLock
        String id = getId();
        Room room = new Room(state, id);
        room.setCreator(options.getCreator());
        rooms.put(id, room);

        log.info("Room {} created", id);
        return room;
    }

    @Override
    public synchronized Optional<IRoom> findRoom(String id) { // FIXME use RWLock
        return Optional.ofNullable(rooms.get(id));
    }

    @Override
    public synchronized List<IRoom> listRooms() { // FIXME use RWLock
        return new ArrayList<>(rooms.values());
    }

}
