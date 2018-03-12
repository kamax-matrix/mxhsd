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

package io.kamax.mxhsd.core.session.user;

import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.RoomID;
import io.kamax.mxhsd.api.room.directory.IFederatedRoomAliasLookup;
import io.kamax.mxhsd.api.room.event.RoomAliasEvent;
import io.kamax.mxhsd.api.session.user.IUserRoomDirectory;
import io.kamax.mxhsd.core.HomeserverState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRoomDirectory implements IUserRoomDirectory {

    private HomeserverState global;
    private _MatrixID mxId;

    public UserRoomDirectory(HomeserverState global, _MatrixID mxId) {
        this.global = global;
        this.mxId = mxId;
    }

    private IRoom getRoom(String roomId) {
        return global.getRoomMgr().getRoom(roomId);
    }

    @Override
    public Optional<IFederatedRoomAliasLookup> lookup(String alias) {
        return global.getRoomDir().lookup(alias);
    }

    @Override
    public void add(String alias, String roomId) {
        if (!RoomID.from(roomId).isLocal(global.getDomain())) {
            throw new IllegalArgumentException("Room alias " + alias + " is not local for " + global.getDomain());
        }

        if (lookup(alias).isPresent()) {
            throw new IllegalStateException("Room alias " + alias + " is already in use");
        }

        getRoom(roomId).inject(new RoomAliasEvent(mxId.getId(), alias));
    }

    @Override
    public void remove(String alias) {
        String roomId = lookup(alias)
                .orElseThrow(() -> new IllegalArgumentException("Room alias " + alias + " does not exist"))
                .getId();

        List<String> aliases = new ArrayList<>(global.getRoomDir().getAliases(roomId));
        if (aliases.contains(alias)) {
            aliases.remove(alias);
            getRoom(roomId).inject(new RoomAliasEvent(mxId.getId(), aliases));
        }
    }

}
