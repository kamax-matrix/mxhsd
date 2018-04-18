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

package io.kamax.mxhsd.api.store;

import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IProcessedEvent;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.core.store.dao.RoomDao;

import java.util.List;
import java.util.Optional;

public interface IStore {

    Optional<IProcessedEvent> findEvent(String id);

    IProcessedEvent putEvent(IEvent event);

    List<RoomDao> listRooms();

    Optional<RoomDao> findRoom(String roomId);

    void putRoom(RoomDao room);

    void addExtremityOfRoom(String roomId, List<String> eventIds);

    void removeExtremityOfRoom(String roomId, List<String> eventIds);

    default Optional<IRoomState> findRoomState(IProcessedEvent ev) {
        return findRoomState(ev.getRoomId(), ev.getId());
    }

    Optional<IRoomState> findRoomState(String roomId, String eventId);

    void putRoomState(IRoomState state, IProcessedEvent event);

}
