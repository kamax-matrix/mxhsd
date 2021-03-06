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

package io.kamax.mxhsd.api.session.user;

import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.room.IRoomCreateOptions;
import io.kamax.mxhsd.api.room.IUserRoom;
import io.kamax.mxhsd.api.sync.ISyncData;
import io.kamax.mxhsd.api.sync.ISyncOptions;
import io.kamax.mxhsd.api.user.IUser;

public interface IUserSession {

    IUserSession getForUser(_MatrixID mxId);

    IUser getUser();

    IDevice getDevice();

    void setPresence(String presence);

    ISyncData fetchData(ISyncOptions options);

    IUserRoom createRoom(IRoomCreateOptions options);

    IUserRoom getRoom(String id);

    IUserRoom joinRoom(String idOrAlias); // TODO move into obj representing the user view of a room

    void leaveRoom(String id); // TODO move into obj representing the user view of a room

    void setReadMarker(String roomId, String type, String eventId); // TODO move into obj representing the user view of a room

    IUserRoomDirectory getRoomDirectory();

    void logout();

}
