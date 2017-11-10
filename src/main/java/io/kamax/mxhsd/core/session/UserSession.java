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

import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.ABuilder;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.event.ISignedEventStream;
import io.kamax.mxhsd.api.event.ISignedEventStreamEntry;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomCreateOptions;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
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
import java.util.*;
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

    private Map<String, JsonObject> readMarkers = new HashMap<>();

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
                .addState(state.getCreation())
                .addState(state.getMemberships().stream() // we process every membership
                        .filter(e -> StringUtils.equals(user.getId().getId(), e.getStateKey())) // only our own join
                        .map(e -> global.getEvMgr().get(e.getEventId()).get())
                        .collect(Collectors.toList()));
    }

    // FIXME this doesn't filter on what the user can see. Need to do better
    private SyncRoomData.Builder buildJoinState(IRoom room, IRoomState state) {
        List<ISignedEventStreamEntry> entries = global.getEvMgr()
                // latest 10 events - FIXME make it configurable / use client filters
                .getBackwardStreamFrom(state.getStreamIndex()).getNext(10).stream()
                // We sort by oldest to newest
                .sorted(Comparator.comparing(ISignedEventStreamEntry::streamIndex))
                // we collect them back into a list
                .collect(Collectors.toList());

        int timelineStart = entries.stream().mapToInt(ISignedEventStreamEntry::streamIndex).min().orElse(state.getStreamIndex());

        return SyncRoomData.build()

                // we set the room ID
                .setRoomId(room.getId())

                // we create the timeline for the most recent events
                // we do this first so state events can be excluded
                .setTimeline(entries.stream().map(ISignedEventStreamEntry::get).collect(Collectors.toList()))

                // we set the previous token
                .setPreviousBatchToken(Integer.toString(timelineStart))

                // we process the room creation
                .addState(state.getCreation())

                // we process every membership
                .addState(state.getMemberships().stream()
                        // we fetch the event to include it
                        .map(context -> global.getEvMgr().get(context.getEventId()).get())
                        // we collect them back into a list
                        .collect(Collectors.toList()))

                // we process power levels
                .addState(global.getEvMgr().get(state.getPowerLevelsEventId()).get());
    }

    private SyncRoomData buildSingleTimeline(IRoom room, int timelineIndex, List<ISignedEventStreamEntry> entries) {
        String mxid = user.getId().getId();
        SyncRoomData.Builder builder = new SyncRoomData.Builder().setRoomId(room.getId());

        entries.stream().sorted(Comparator.comparing(ISignedEventStreamEntry::streamIndex)).forEach(ev -> {
            String evId = ev.get().getId();
            ISignedEvent evFull = ev.get();
            RoomEventType evType = RoomEventType.from(evFull.getType());
            IRoomState roomState = room.getStateFor(evId);

            Optional<String> opt = roomState.getMembershipValue(mxid);
            opt.ifPresent(builder::setMembership);

            if (!roomState.isAccessibleAs(mxid)) {
                log.info("Event {} at position {} is not accessible to {}, skipping", evId, ev.streamIndex(), mxid);
            } else {
                if (RoomEventType.Membership.equals(evType)) {
                    RoomMembershipEvent eventDetails = new RoomMembershipEvent(evFull.getJson());
                    boolean isAboutUs = StringUtils.equals(mxid, eventDetails.getStateKey());
                    if (isAboutUs) {
                        builder.setMembership(eventDetails.getMembership());

                        if (RoomMembership.Join.is(eventDetails.getMembership())) {
                            ISignedEventStreamEntry rCreate = global.getEvMgr().get(roomState.getCreation().getId());
                            if (evFull.getParents().contains(rCreate.get().getId())) {
                                builder.addTimeline(rCreate.get());
                            } else {
                                if (rCreate.streamIndex() < timelineIndex) {
                                    builder.addState(rCreate.get());
                                }
                            }

                            roomState.getMemberships().stream().map(ref -> global.getEvMgr().get(ref.getEventId()).get())
                                    .forEach(builder::addState);
                        }

                        // TODO if invite, we should include data about membership of the sender

                        // TODO if leave, find out if we need to include something?
                    }
                }

                builder.addTimeline(evFull);
            }
        });

        builder.setLimited(false);
        builder.setPreviousBatchToken(Integer.toString(timelineIndex)); // TODO have a separate generator?
        Optional.ofNullable(readMarkers.get(room.getId())).ifPresent(builder::addAccountData); // ugly, but effective

        return builder.get();
    }

    private void buildTimelines(SyncData.Builder b, int timelineIndex, List<ISignedEventStreamEntry> entries) {
        Map<String, List<ISignedEventStreamEntry>> roomStreams = new HashMap<>();
        entries.forEach(ev -> roomStreams.computeIfAbsent(ev.get().getRoomId(), rId -> new ArrayList<>()).add(ev));
        roomStreams.forEach((roomId, stream) -> {
            SyncRoomData data = buildSingleTimeline(global.getRoomMgr().getRoom(roomId), timelineIndex, stream);
            data.getMembership().ifPresent(membership -> {
                // TODO use the membership as key to add rooms to the global sync data, as it matches json key
                if (RoomMembership.Invite.is(membership)) {
                    b.addInvited(data);
                }

                if (RoomMembership.Join.is(membership)) {
                    b.addJoined(data);
                }

                if (RoomMembership.Leave.is(membership)) {
                    b.addLeft(data);
                }
            });
        });
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

        b.setInvited(syncState.invited.values().stream().map(ABuilder::get).collect(Collectors.toList()))
                .setJoined(syncState.joined.values().stream().map(ABuilder::get).collect(Collectors.toList()))
                .setToken(Integer.toString(streamIndex));

        SyncData syncData = b.get();
        statesCache.put(getDevice().getId() + syncData.getNextBatchToken(), syncState);

        return syncData;
    }

    private ISyncData fetchNext(ISyncOptions options, int fromIndex, int toIndex) {
        SyncData.Builder syncData = SyncData.build().setToken(Integer.toString(toIndex));
        String mxid = user.getId().getId();
        int amount = toIndex - fromIndex;

        ISignedEventStream stream = global.getEvMgr().getBackwardStreamFrom(toIndex);
        List<ISignedEventStreamEntry> entries = stream.getNext(amount);
        buildTimelines(syncData, fromIndex, entries);

        return syncData.get();
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
                        // FIXME this should not be here. Use async request handling in Spring instead (or similar)
                        wait(Math.max(Math.min(endTs.toEpochMilli() - Instant.now().toEpochMilli(), 500), 1));
                    } catch (InterruptedException e) {
                        // we got interrupted, let's try to fetch again
                    }
                }
            }
        } while (!Thread.interrupted() && Instant.now().isBefore(endTs));

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
    public IRoom joinRoom(String id) {
        IRoom room = global.getRoomMgr().getRoom(id);
        room.inject(new RoomMembershipEvent(user.getId().getId(), RoomMembership.Join.get(), user.getId().getId()));
        return room;
    }

    @Override
    public void leaveRoom(String id) {
        IRoom room = global.getRoomMgr().getRoom(id);
        room.inject(new RoomMembershipEvent(user.getId().getId(), RoomMembership.Leave.get(), user.getId().getId()));
    }

    @Override
    public void setReadMarker(String roomId, String type, String eventId) {
        JsonObject content = new JsonObject();
        content.addProperty("event_id", eventId);
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);
        obj.add("content", content);
        readMarkers.put(roomId, obj);
    }

    @Override
    public void logout() {
        // FIXME do something
    }

}
