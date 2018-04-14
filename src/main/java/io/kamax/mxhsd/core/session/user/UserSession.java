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

import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.matrix.room.RoomAlias;
import io.kamax.mxhsd.ABuilder;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.event.IProcessedEvent;
import io.kamax.mxhsd.api.event.IProcessedEventStream;
import io.kamax.mxhsd.api.event.ISignedEventStreamEntry;
import io.kamax.mxhsd.api.exception.NotFoundException;
import io.kamax.mxhsd.api.room.*;
import io.kamax.mxhsd.api.room.directory.IFederatedRoomAliasLookup;
import io.kamax.mxhsd.api.room.event.EventComparator;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.api.session.user.IUserRoomDirectory;
import io.kamax.mxhsd.api.session.user.IUserSession;
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
            log.debug("We got new data to sync with: {}", ev.get().getId());
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
                        .map(e -> global.getEvMgr().get(e.getEventId()))
                        .collect(Collectors.toList()));
    }

    // FIXME this doesn't filter on what the user can see. Need to do better
    private SyncRoomData.Builder buildJoinState(IRoom room, String position) {
        IProcessedEvent head = room.getLastEvent();

        return SyncRoomData.build()

                // we set the room ID
                .setRoomId(room.getId())

                // we set the previous token
                .setPreviousBatchToken(position)

                // We set the last event for the room
                .addTimeline(head)

                // we process the room state and remove "leave" membership
                .addState(room.getStateFor(head.getId()).getEvents().values().stream().filter(ev -> {
                    if (RoomEventType.Membership.is(ev.getType())) {
                        RoomMembershipEvent eventDetails = new RoomMembershipEvent(ev.getJson());
                        return !RoomMembership.Leave.is(eventDetails.getMembership());
                    }

                    return true;
                }).collect(Collectors.toList()));
    }

    private SyncRoomData buildSingleTimeline(IRoom room, String fromPosition, List<IProcessedEvent> entries) {
        String mxid = user.getId().getId();
        SyncRoomData.Builder builder = new SyncRoomData.Builder().setRoomId(room.getId());

        entries.stream().sorted(EventComparator.forProcessed()).forEach(ev -> {
            String evId = ev.getId();
            RoomEventType evType = RoomEventType.from(ev.getType());
            IRoomState roomState = room.getStateFor(evId);

            Optional<String> opt = roomState.findMembershipValue(mxid);
            opt.ifPresent(builder::setMembership);

            if (!roomState.isAccessibleAs(mxid)) {
                log.info("Event {} at position {} is not accessible to {}, skipping", evId, ev.getInternalId(), mxid);
            } else {
                if (RoomEventType.Membership.equals(evType)) {
                    RoomMembershipEvent eventDetails = new RoomMembershipEvent(ev.getJson());
                    boolean isAboutUs = StringUtils.equals(mxid, eventDetails.getStateKey());
                    if (isAboutUs) {
                        log.info("Found membership about ourself in room {}: {}", room.getId(), eventDetails.getMembership());
                        builder.setMembership(eventDetails.getMembership());

                        if (RoomMembership.Join.is(eventDetails.getMembership())) {
                            IProcessedEvent rCreate = global.getEvMgr().get(roomState.getCreation().getId());
                            if (global.getEvMgr().isBefore(rCreate.getInternalId(), fromPosition)) {
                                builder.addState(rCreate);
                            } else {
                                builder.addTimeline(rCreate);
                            }

                            roomState.getMemberships().stream().map(ref -> global.getEvMgr().get(ref.getEventId()))
                                    .forEach(ev1 -> {
                                        if (global.getEvMgr().isBefore(ev1.getInternalId(), fromPosition)) {
                                            builder.addState(ev1);
                                        } else {
                                            builder.addTimeline(ev1);
                                        }
                                    });
                        }

                        // TODO if invite, we should include data about membership of the sender

                        if (RoomMembership.Leave.is(eventDetails.getMembership())) {
                            builder.setMembership(RoomMembership.Leave.get());
                        }
                        // TODO if leave, find out if we need to include something?
                    }
                }

                builder.addTimeline(ev);
            }
        });

        builder.setLimited(false);
        builder.setPreviousBatchToken(fromPosition); // TODO have a separate generator?
        Optional.ofNullable(readMarkers.get(room.getId())).ifPresent(builder::addAccountData); // ugly, but effective

        return builder.get();
    }

    private void buildTimelines(SyncData.Builder b, String fromPosition, List<IProcessedEvent> entries) {
        Map<String, List<IProcessedEvent>> roomStreams = new HashMap<>();
        entries.forEach(ev -> roomStreams.computeIfAbsent(ev.getRoomId(), rId -> new ArrayList<>()).add(ev));

        roomStreams.forEach((roomId, stream) -> {
            Optional<IRoom> opt = global.getRoomMgr().findRoom(roomId);
            if (opt.isPresent()) {
                SyncRoomData data = buildSingleTimeline(opt.get(), fromPosition, stream);
                Optional<String> rOpt = data.getMembership();
                rOpt.ifPresent(membership -> {
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
            } else {
                log.warn("We have event(s) for an unknown room: {}", roomId);
            }
        });
    }

    private ISyncData fetchInitial(ISyncOptions options) {
        String mxId = user.getId().getId();
        String currentPosition = global.getEvMgr().getPosition();
        State syncState = new State();
        SyncData.Builder b = SyncData.build();

        // We fetch room states
        global.getRoomMgr().listRooms().parallelStream().forEach(room -> {
            IRoomState state = room.getCurrentState();
            state.findMembershipValue(mxId).ifPresent(m -> {
                if (RoomMembership.Invite.is(m)) {
                    syncState.invited.put(room.getId(), buildInviteState(room, state));
                }

                if (RoomMembership.Join.is(m)) {
                    syncState.joined.put(room.getId(), buildJoinState(room, currentPosition));
                }
            });
        });

        b.setInvited(syncState.invited.values().stream().map(ABuilder::get).collect(Collectors.toList()))
                .setJoined(syncState.joined.values().stream().map(ABuilder::get).collect(Collectors.toList()))
                .setLeft(syncState.left.values().stream().map(ABuilder::get).collect(Collectors.toList()))
                .setToken(currentPosition);

        SyncData syncData = b.get();
        statesCache.put(getDevice().getId() + syncData.getNextBatchToken(), syncState);
        return syncData;
    }

    private ISyncData fetchNext(ISyncOptions options, String fromPosition, String toPosition) {
        SyncData.Builder syncData = SyncData.build().setToken(toPosition);

        List<IProcessedEvent> events = new ArrayList<>();
        IProcessedEventStream stream = global.getEvMgr().getBackwardStreamFrom(toPosition);
        while (stream.hasNext() && events.size() < 10) {
            events.add(stream.getNext());
        }
        buildTimelines(syncData, fromPosition, events);

        return syncData.get();
    }

    private ISyncData fetchNextOrWait(ISyncOptions options, String since) {
        // FIXME catch exception and throw appropriate error in case of parse error
        // TODO SPEC - Possible errors
        Instant endTs = Instant.now().plus(options.getTimeout(), ChronoUnit.MILLIS);
        SyncData.Builder syncBuild = SyncData.build();

        do { // at least one time
            String currentPosition = global.getEvMgr().getPosition();

            if (!StringUtils.equals(since, currentPosition)) { // we got new data
                return fetchNext(options, since, currentPosition);
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
        return global.getRoomMgr().getRoom(id);
    }

    private IRoom joinLocalRoom(String roomId) {
        IRoom room = global.getRoomMgr().getRoom(roomId);
        room.inject(new RoomMembershipEvent(user.getId().getId(), RoomMembership.Join.get(), user.getId().getId()));
        return room;
    }

    @Override
    public IRoom joinRoom(String idOrAlias) {
        if (RoomAlias.is(idOrAlias)) {
            IFederatedRoomAliasLookup lookup = global.getRoomDir()
                    .lookup(idOrAlias).orElseThrow(() -> new NotFoundException(idOrAlias));

            return global.getRoomMgr()
                    .getRoom(lookup)
                    .joinAs(user.getId());
        }

        if (!RoomID.is(idOrAlias)) {
            throw new IllegalArgumentException("Not a valid room ID or alias: " + idOrAlias);
        }

        return joinLocalRoom(idOrAlias);
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
    public IUserRoomDirectory getRoomDirectory() {
        return new UserRoomDirectory(global, user.getId());
    }

    @Override
    public void logout() {
        // FIXME do something
    }

}
