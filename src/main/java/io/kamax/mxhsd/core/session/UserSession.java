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

package io.kamax.mxhsd.core.session;

import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomCreateOptions;
import io.kamax.mxhsd.api.session.IUserSession;
import io.kamax.mxhsd.api.sync.ISyncData;
import io.kamax.mxhsd.api.sync.ISyncOptions;
import io.kamax.mxhsd.api.user.IHomeserverUser;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import io.kamax.mxhsd.core.sync.SyncData;

public class UserSession implements IUserSession {

    private HomeserverState stateHs;
    private IDevice device;
    private IHomeserverUser user;

    public UserSession(HomeserverState stateHs, IHomeserverUser user, IDevice dev) {
        this.stateHs = stateHs;
        this.user = user;
        this.device = dev;
    }

    @Override
    public IUserSession getForUser(_MatrixID mxId) {
        return this;
    }

    @Override
    public IHomeserverUser getUser() {
        return user;
    }

    @Override
    public IDevice getDevice() {
        return device;
    }

    @Override
    public void setPresence(String presence) {
        // FIXME do something
    }

    @Override
    public ISyncData fetchData(ISyncOptions options) {
        try {
            Thread.sleep(options.getTimeout());
        } catch (InterruptedException e) {
            // TODO is there anything to do here?
        }

        return new SyncData(Long.toString(System.currentTimeMillis()));
    }

    @Override
    public IRoom createRoom(IRoomCreateOptions options) {
        RoomCreateOptions validOptions = new RoomCreateOptions();
        validOptions.setCreator(user.getId());
        options.getInvitees().forEach(validOptions::addInvitee);
        options.getPreset().ifPresent(validOptions::setPreset);

        return stateHs.getRoomMgr().createRoom(validOptions);
    }

    @Override
    public void logout() {
        // FIXME do something
    }

}
