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
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.event.ISignedEventStream;
import io.kamax.mxhsd.api.event.ISignedEventStreamEntry;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomCreateOptions;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.api.session.IUserSession;
import io.kamax.mxhsd.api.sync.ISyncData;
import io.kamax.mxhsd.api.sync.ISyncOptions;
import io.kamax.mxhsd.api.user.IHomeserverUser;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import io.kamax.mxhsd.core.sync.SyncData;
import io.kamax.mxhsd.core.sync.SyncRoomData;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserSession implements IUserSession {

    private class State {

        private Map<String, SyncRoomData.Builder> invited = new ConcurrentHashMap<>();
        private Map<String, SyncRoomData.Builder> joined = new ConcurrentHashMap<>();
        private Map<String, SyncRoomData.Builder> left = new ConcurrentHashMap<>();

    }

    private final Logger log = LoggerFactory.getLogger(UserSession.class);

    private HomeserverState global;
    private IDevice device;
    private IHomeserverUser user;

    // FIXME This is such a big hack! but it's ok until we implement cache management
    private Map<String, State> statesCache = new WeakHashMap<>();

    public UserSession(HomeserverState global, IHomeserverUser user, IDevice dev) {
        this.global = global;
        this.user = user;
        this.device = dev;

        init();
    }

    private void init() {
        global.getEvMgr().addListener(this);
    }

    // TODO consider refactoring this into a consumer (functional interface)
    @Handler
    private void getEvent(ISignedEventStreamEntry ev) {
        synchronized (this) {
            notifyAll(); // let's wake up waiting threads
        }
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

    private SyncRoomData.Builder buildInviteState(IRoom room, IRoomState state) {
        return SyncRoomData.build()
                .setRoomId(room.getId())
                .addState(state.getMemberships().stream() // we process every membership
                        .filter(e -> StringUtils.equals(user.getId().getId(), e.getStateKey())) // only our own join
                        .map(e -> global.getEvMgr().get(e.getEventId()).get())
                        .collect(Collectors.toList()));
    }

    private SyncRoomData.Builder buildJoinState(IRoom room, IRoomState state) {
        return SyncRoomData.build()

                // we set the room ID
                .setRoomId(room.getId())

                // we process every membership
                .addState(state.getMemberships().stream()
                        // we fetch the event to include it
                        .map(context -> global.getEvMgr().get(context.getEventId()).get())
                        // we collect them back into a list
                        .collect(Collectors.toList()))

                // we process the room creation
                .addState(state.getCreation())

                // we process power levels
                .addState(global.getEvMgr().get(state.getPowerLevelsEventId()).get())


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
                                .collect(Collectors.toList()));
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

    private ISyncData fetchNext(ISyncOptions options, int fromIndex, int toIndex) {
        SyncData.Builder syncData = SyncData.build().setToken(Integer.toString(toIndex));
        String mxid = user.getId().getId();
        int amount = toIndex - fromIndex;
        log.info("User {}: Fetching events from {} to {} ({})", mxid, fromIndex, toIndex, amount);


        ISignedEventStream stream = global.getEvMgr().getBackwardStreamFrom(toIndex);
        List<ISignedEventStreamEntry> entries = stream.getNext(amount);

        entries.stream()
                .sorted(Comparator.comparing(ISignedEventStreamEntry::streamIndex)).forEach(streamEv -> {
            ISignedEvent ev = streamEv.get();
            log.info("Processing event {} in room {} at stream index {}", ev.getId(), ev.getRoomId(), streamEv.streamIndex());
            RoomEventType evType = RoomEventType.from(ev.getType());
            boolean isAboutUs = evType.isState() && StringUtils.equals(mxid, EventKey.StateKey.getString(ev.getJson()));
            global.getRoomMgr().findRoom(ev.getRoomId()).ifPresent(r -> {
                IRoomState state = r.getStateFor(ev.getId());
                log.info("Processing state of room {}", ev.getRoomId());
                state.getMembershipValue(mxid).ifPresent(membership -> {
                    log.info("We are in the room");
                    if (RoomMembership.Invite.is(membership)) {
                        if (RoomEventType.Membership.equals(evType) && isAboutUs) {
                            syncData.getInvited(r.getId()).addState(ev);
                        } else {
                            // not about us, we ignore
                        }
                    }

                    if (RoomMembership.Leave.is(membership)) {
                        if (RoomEventType.Membership.equals(evType) && isAboutUs) {
                            syncData.getInvited(r.getId()).addState(ev);
                        } else {
                            // not about us, we ignore
                        }
                    }

                    if (RoomMembership.Join.is(membership)) {
                        SyncRoomData.Builder roomBuilder = syncData.getJoined(r.getId());
                        roomBuilder.addTimeline(ev);

                        if (evType.isState()) { // FIXME and valid
                            roomBuilder.addState(ev);
                        }
                    }
                });
            });
        });

        log.info("Done fetching");
        SyncData d = syncData.get();
        log.info("Sync data: \n{}", GsonUtil.getPrettyForLog(d));
        return d;
    }

    private ISyncData fetchNextOrWait(ISyncOptions options, String since) {
        // FIXME catch exception and throw appropriate error in case of parse error
        // TODO SPEC - Possible errors
        int sinceIndex = Integer.parseInt(since);
        Instant endTs = Instant.now().plus(options.getTimeout(), ChronoUnit.MILLIS);
        SyncData.Builder syncBuild = SyncData.build();

        do { // at least one time
            int currentIndex = global.getEvMgr().getStreamIndex();
            int amount = currentIndex - sinceIndex;

            if (amount < 0) { // something went wrong, we send initial data instead
                return fetchInitial(options);
            }

            if (amount > 0) { // we got new data
                return fetchNext(options, sinceIndex, currentIndex);
            } else { // no new data, let's wait
                synchronized (this) {
                    try {
                        // we wait at least 1ms and at most 500ms
                        wait(Math.max(Math.min(endTs.toEpochMilli() - Instant.now().toEpochMilli(), 500), 1));
                    } catch (InterruptedException e) {
                        // we got interrupted, let's try to fetch again
                    }
                }
            }
        } while (Instant.now().isBefore(endTs));

        return syncBuild.setToken(since).get(); // no new data, we just send the same token
    }

    // FIXME refactor into some kind of per-user stream handler using the provided filter
    @Override
    public ISyncData fetchData(ISyncOptions options) {
        String since = options.getSince().orElse("");
        if (StringUtils.isBlank(since) || options.isFullState()) {
            return fetchInitial(options);
        } else {
            return fetchNextOrWait(options, since);
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
