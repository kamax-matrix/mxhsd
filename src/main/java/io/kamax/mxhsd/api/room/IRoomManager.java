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

package io.kamax.mxhsd.api.room;

import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.exception.NotFoundException;
import io.kamax.mxhsd.api.room.directory.IFederatedRoomAliasLookup;

import java.util.List;
import java.util.Optional;

public interface IRoomManager {

    IRoom createRoom(IRoomCreateOptions options);

    IRoom discoverRoom(String roomId, List<IEvent> initialState, List<IEvent> authChain, IEvent seed);

    IRoom getRoom(String id);

    IAliasRoom getRoom(IFederatedRoomAliasLookup lookup);

    default Optional<IRoom> findRoom(String id) {
        try {
            return Optional.ofNullable(getRoom(id));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    List<String> listRooms();

    IAllRoomsHandler forAllRooms();

}
