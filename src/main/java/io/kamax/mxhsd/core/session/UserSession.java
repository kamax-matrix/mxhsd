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
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomCreateOptions;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.session.IUserSession;
import io.kamax.mxhsd.api.sync.ISyncData;
import io.kamax.mxhsd.api.sync.ISyncOptions;
import io.kamax.mxhsd.api.sync.ISyncRoomData;
import io.kamax.mxhsd.api.user.IHomeserverUser;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import io.kamax.mxhsd.core.sync.SyncData;
import io.kamax.mxhsd.core.sync.SyncRoomData;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class UserSession implements IUserSession {

    private HomeserverState global;
    private IDevice device;
    private IHomeserverUser user;

    public UserSession(HomeserverState global, IHomeserverUser user, IDevice dev) {
        this.global = global;
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

    private SyncRoomData buildInviteState(IRoom room, IRoomState state) {
        return SyncRoomData.build()
                .setRoomId(room.getId())
                .setState(state.getMemberships().stream() // we process every membership
                        .map(context -> global.getEvMgr().get(context.getEventId()).get()) // we fetch the event to include it
                        .collect(Collectors.toList()))
                .get();
    }

    private SyncRoomData buildJoinState(IRoom room, IRoomState state) {
        return SyncRoomData.build()
                .setRoomId(room.getId())
                .setState(state.getMemberships().stream() // we process every membership
                        .map(context -> global.getEvMgr().get(context.getEventId()).get()) // we fetch the event to include it
                        .collect(Collectors.toList()))
                .setTimeline(state.getExtremities())
                .get();
    }

    private ISyncData fetchInitial(ISyncOptions options) {
        String mxId = user.getId().getId();
        int streamIndex = global.getEvMgr().getStreamIndex();
        List<ISyncRoomData> invited = new CopyOnWriteArrayList<>();
        List<ISyncRoomData> joined = new CopyOnWriteArrayList<>();
        SyncData.Builder b = SyncData.build();

        // We fetch room states
        global.getRoomMgr().listRooms().parallelStream().forEach(room -> {
            IRoomState state = room.getCurrentState();
            state.getMembershipValue(mxId).ifPresent(m -> {
                if (RoomMembership.Invite.is(m)) {
                    invited.add(buildInviteState(room, state));
                }

                if (RoomMembership.Join.is(m)) {
                    joined.add(buildJoinState(room, state));
                }
            });
        });

        b.setInvited(invited).setJoined(joined).setToken("u" + streamIndex);
        return b.get();
    }

    private ISyncData fetchNext(ISyncOptions options) {
        try {
            Thread.sleep(options.getTimeout());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return SyncData.build().get();
    }

    @Override
    public ISyncData fetchData(ISyncOptions options) {
        if (options.getSince().map(StringUtils::isBlank).orElse(true) || options.isFullState()) {
            return fetchInitial(options);
        } else {
            return fetchNext(options);
        }
    }

    @Override
    public IRoom createRoom(IRoomCreateOptions options) {
        RoomCreateOptions validOptions = new RoomCreateOptions();
        validOptions.setCreator(user.getId());
        options.getInvitees().forEach(validOptions::addInvitee);
        options.getPreset().ifPresent(validOptions::setPreset);

        return global.getRoomMgr().createRoom(validOptions);
    }

    @Override
    public void logout() {
        // FIXME do something
    }

}
