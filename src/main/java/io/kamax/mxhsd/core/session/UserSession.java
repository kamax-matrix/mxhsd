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
import io.kamax.mxhsd.api.event.ISignedEventStreamEntry;
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

import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserSession implements IUserSession {

    private class State {

        private Map<String, ISyncRoomData> invited = new ConcurrentHashMap<>();
        private Map<String, ISyncRoomData> joined = new ConcurrentHashMap<>();
        private Map<String, ISyncRoomData> left = new ConcurrentHashMap<>();

    }

    private HomeserverState global;
    private IDevice device;
    private IHomeserverUser user;

    // FIXME This is such a big hack! but it's ok until we implement cache management
    private Map<String, State> statesCache = new WeakHashMap<>();

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

                // we process every membership
                .setState(state.getMemberships().stream()
                        // we fetch the event to include it
                        .map(context -> global.getEvMgr().get(context.getEventId()).get())
                        // we collect them back into a list
                        .collect(Collectors.toList()))

                // we create the timeline for the most recent events - FIXME missing previous events token
                .setTimeline(
                        global.getEvMgr()
                                // latest 10 events - FIXME make it configurable / use client filters
                                .getBackwardStreamFrom(state.getStreamIndex()).getNext(10).stream()
                                // We sort by oldest to newest
                                .sorted(Comparator.comparing(ISignedEventStreamEntry::streamIndex))
                                // We get the actual event
                                .map(ISignedEventStreamEntry::get)
                                // we collect them back into a list
                                .collect(Collectors.toList()))
                .get();
    }

    private ISyncData fetchInitial(ISyncOptions options) {
        String mxId = user.getId().getId();
        int streamIndex = global.getEvMgr().getStreamIndex();
        State syncState = new State();
        SyncData.Builder b = SyncData.build();

        // We fetch room states
        global.getRoomMgr().listRooms().parallelStream().forEach(room -> {
            IRoomState state = room.getCurrentState();
            state.getMembershipValue(mxId).ifPresent(m -> {
                if (RoomMembership.Invite.is(m)) {
                    syncState.invited.put(room.getId(), buildInviteState(room, state));
                }

                if (RoomMembership.Join.is(m)) {
                    syncState.joined.put(room.getId(), buildJoinState(room, state));
                }
            });
        });

        b.setInvited(syncState.invited.values())
                .setJoined(syncState.joined.values())
                .setToken(Integer.toString(streamIndex));

        SyncData syncData = b.get();
        statesCache.put(getDevice().getId() + syncData.getNextBatchToken(), syncState);

        return syncData;
    }

    private ISyncData fetchNext(ISyncOptions options, String since) {
        SyncData.Builder syncBuild = SyncData.build();
        try {
            // FIXME catch exception and throw appropriate error in case of parse error
            // TODO SPEC - Possible errors
            int sinceIndex = Integer.parseInt(since);
            int currentIndex = global.getEvMgr().getStreamIndex();
            int amount = currentIndex - sinceIndex;
            if (amount < 0) { // something went wrong, we send initial data instead
                return fetchInitial(options);
            }

            if (amount > 0) { // we get the new data
                return fetchInitial(options);
            } else { // we just wait for new data
                // FIXME use timeout, hardcoded for testing purposes until a trigger is created
                Thread.sleep(1000L);
                return syncBuild.get();
            }
        } catch (InterruptedException e) {
            // TODO do better?
            return syncBuild.get();
        }
    }

    @Override
    public ISyncData fetchData(ISyncOptions options) {
        String since = options.getSince().orElse("");
        if (StringUtils.isBlank(since) || options.isFullState()) {
            return fetchInitial(options);
        } else {
            return fetchNext(options, since);
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
    public IRoom getRoom(String id) {
        return global.getRoomMgr().findRoom(id).orElseThrow(() -> new IllegalArgumentException("Unknown room " + id));
    }

    @Override
    public void logout() {
        // FIXME do something
    }

}
