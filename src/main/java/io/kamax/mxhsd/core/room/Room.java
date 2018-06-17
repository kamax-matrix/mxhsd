/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxhsd.core.room;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.Caches;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.Lists;
import io.kamax.mxhsd.api.event.*;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.exception.MatrixException;
import io.kamax.mxhsd.api.federation.IRemoteHomeServer;
import io.kamax.mxhsd.api.room.*;
import io.kamax.mxhsd.api.room.event.EventComparator;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.core.GlobalStateHolder;
import io.kamax.mxhsd.core.event.Event;
import io.kamax.mxhsd.core.event.GetAuthChainTask;
import io.kamax.mxhsd.core.event.NakedContentEvent;
import io.kamax.mxhsd.core.room.algo.v1.RoomAlgorithm_v1;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Room implements IRoom {

    private final Logger log = LoggerFactory.getLogger(Room.class);

    private GlobalStateHolder global;
    private IRoomAlgorithm algo;

    private MBassador<IEvent> eventBus;

    private String id;
    private RoomState state;
    private List<String> extremities = new ArrayList<>();

    private LoadingCache<String, IRoomState> states;

    private void info(String message, Object... parms) {
        log.info(String.format("%s : %s", id, message), parms);
    }

    public Room(GlobalStateHolder global, String id) {
        this.global = global;
        this.id = id;
        this.eventBus = new MBassador<>(new IPublicationErrorHandler.ConsoleLogger(true));
        this.algo = new RoomAlgorithm_v1(eventId -> this.global.getEvMgr().get(eventId));
        this.states = CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(30000)
                .build(new CacheLoader<String, IRoomState>() {
                    @Override
                    public IRoomState load(String eventId) {
                        return global.getStore().findRoomState(id, eventId)
                                .orElseGet(() -> {
                                    info("State for event {} was not found, trying to fetch remotely", eventId);

                                    // We compute all servers that could have the event
                                    Set<String> targets = new HashSet<>();
                                    global.getStore().findEvent(eventId).map(IProtoEvent::getParents)
                                            .map(parents -> Lists.map(parents, IEventIdReference::getEventId)).orElseGet(ArrayList::new)
                                            .stream().map(evId -> global.getStore().findRoomState(id, evId).orElseGet(RoomState::empty))
                                            .forEach(rState -> targets.addAll(rState.getServers()));

                                    // In case we don't have the previous events
                                    if (targets.isEmpty()) {
                                        info("No candidate servers were found for fetching state, using current room state");
                                        targets.addAll(getCurrentState().getServers());
                                        targets.add(eventId.split(":")[1]);
                                    } else {
                                        info("We found {} candidate servers for fetching state at event {}", targets.size(), eventId);
                                    }

                                    for (IRemoteHomeServer srv : global.getHsMgr().get(targets)) {
                                        if (StringUtils.equals(global.getDomain(), srv.getDomain())) {
                                            // We don't need to call ourselves
                                            continue;
                                        }

                                        try {
                                            info("Asking {} for state", srv.getDomain());
                                            IRoomStateSnapshotIds snapshotIds = srv.getStateIds(id, eventId);
                                            EventLookup chainLookup = global.getEvMgr().lookup(snapshotIds.getAuthChainIds());
                                            EventLookup pduLookup = global.getEvMgr().lookup(snapshotIds.getStateEventIds());

                                            IRoomStateSnapshot snapshot;
                                            if (chainLookup.hasAll() && pduLookup.hasAll()) {
                                                info("Found all state events via ID");
                                                snapshot = new RoomStateSnapshot(pduLookup.getFound(), chainLookup.getFound());
                                            } else {
                                                info("Missing state events, fetching full data set");
                                                snapshot = srv.getState(id, eventId);
                                                info("Storing auth chain events");
                                                snapshot.getAuthChain().stream().sorted(EventComparator.forEvent()).forEach(global.getStore()::putEvent);
                                                info("Storing state events");
                                                snapshot.getState().stream().sorted(EventComparator.forEvent()).forEach(global.getStore()::putEvent);
                                            }

                                            info("We got state");
                                            // FIXME we must optimize this - rely on the caching?
                                            // We store every event of the

                                            info("Building state");
                                            RoomState.Builder builder = RoomState.build();
                                            snapshot.getState().forEach(builder::addEvent);
                                            RoomState state = builder.get();
                                            info("Storing state");
                                            global.getStore().putRoomState(id, eventId, state);
                                            return state;
                                            /*
                                        } catch (FederationException fedEx) {
                                            FIXME MUST HANDLE
                                            */
                                        } catch (MatrixException mxEx) {
                                            if (StringUtils.equals(ForbiddenException.Code, mxEx.getErrorCode())) {
                                                info("We are not allowed to see the state, returning empty state");
                                                RoomState state = RoomState.empty();
                                                global.getStore().putRoomState(id, eventId, state);
                                                return state;
                                            } else {
                                                log.warn("Unable to fetch state from {}: {}", srv.getDomain(), mxEx.getMessage());
                                            }
                                        } catch (RuntimeException e) {
                                            log.warn("Unable to fetch state from {}: {}", srv.getDomain(), e.getMessage());
                                        }
                                    }

                                    log.warn("We couldn't find, fetch or compute a room state for {}-{}, returning empty state", id, eventId);
                                    return RoomState.empty();
                                });
                    }
                });

        setCurrentState(RoomState.build().get());
    }

    public Room(GlobalStateHolder global, String id, List<String> extremities) {
        this(global, id);
        setCurrentState(RoomState.from(algo.resolve(Lists.map(extremities, this::getStateFor))));
        this.extremities = new ArrayList<>(extremities);
    }

    public Room(GlobalStateHolder global, String id, List<IEvent> initialState, List<IEvent> authChain, IEvent seed) {
        this(global, id);

        // We order events so we can build a state properly
        List<IEvent> chain = authChain.stream().unordered()
                .distinct()
                // FIXME compute and use topological order
                .sorted(EventComparator.forEvent())
                .collect(Collectors.toList());

        // We order events so we can build a state properly
        List<IEvent> state = initialState.stream().unordered()
                .distinct()
                // FIXME compute and use topological order
                .sorted(EventComparator.forEvent())
                .collect(Collectors.toList());

        info("Storing auth chain", id);
        chain.forEach(e -> global.getEvMgr().store(e));

        info("Storing state", id);
        state.forEach(e -> global.getEvMgr().store(e));

        info("Storing seed", id);
        global.getEvMgr().store(seed);

        RoomState.Builder b = RoomState.build();
        state.forEach(b::addEvent);
        state.add(seed);
        RoomState seedState = b.get();
        global.getStore().putRoomState(id, seed.getId(), seedState);

        extremities.add(seed.getId());
        setCurrentState(seedState);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public IProcessedEvent getCreation() {
        return global.getEvMgr().get(state.getCreation().getId());
    }

    // FIXME use RWLock
    private synchronized void setCurrentState(RoomState state) {
        this.state = state;
    }

    // FIXME use RWLock
    @Override
    public synchronized RoomState getCurrentState() {
        return state;
    }

    private List<IRoomState> getStates(IProtoEvent ev) {
        return Lists.collect(ev.getParents().stream()
                .map(IEventIdReference::getEventId)
                .map(evId -> Caches.get(states, evId)));
    }

    // FIXME use RWLock
    private synchronized void addExtremity(IEvent ev) {
        log.info("{}: Adding extremity {}", id, ev.getId());

        // We remove parents that were listed as extremities
        ev.getParents().forEach(ref -> extremities.remove(ref.getEventId()));

        // We add the current event as extremity
        extremities.add(ev.getId());

        // We calculate the new current state of the room
        setCurrentState(RoomState.build().from(algo.resolve(Lists.map(extremities, states))).get());
    }

    // FIXME use RWLock
    private synchronized IProcessedEvent inject(IEvent ev, RoomState state) {
        IProcessedEvent evStored = global.getEvMgr().store(ev);
        global.getStore().putRoomState(id, evStored.getId(), state);
        addExtremity(ev);
        return evStored;
    }

    // FIXME use RWLock
    private synchronized IProcessedEvent inject(IEvent ev, RoomEventAuthorization eval) {
        // FIXME hack?
        try {
            return inject(ev, RoomState.from(eval.getNewState()));
        } finally {
            eventBus.publish(ev);
        }
    }

    @Override
    public IProcessedEvent getLastEvent() {
        return getExtremities().stream().max(EventComparator.forProcessed()).orElseThrow(RuntimeException::new);
    }

    // FIXME use RWLock
    @Override
    public synchronized IProcessedEvent inject(NakedContentEvent evNaked) {
        List<IProcessedEvent> parents = getExtremities();
        log.info("Room {}: Injecting new event of type {}", id, evNaked.getType());
        IProtoEventBuilder evTempBuilder = global.getEvMgr().populate(evNaked, getId(), state, parents);
        IHashedProtoEvent evTemp = global.getEvMgr().hash(evTempBuilder.get());

        IRoomState parentState = algo.resolve(getStates(evTemp));
        RoomEventAuthorization eval = algo.authorize(parentState, evTemp);
        if (!eval.isAuthorized()) {
            log.debug("Previous state: {}", GsonUtil.getPrettyForLog(parentState));
            log.error(eval.getReason());
            throw new ForbiddenException("Unauthorized event");
        }

        eval.getBasedOn().forEach(evTempBuilder::addAuthorization);
        IEvent evSigned = global.getEvMgr().sign(evTempBuilder.get());
        log.debug("Signed event to inject: {}", GsonUtil.getPrettyForLog(evSigned.getJson()));
        return inject(evSigned, eval);
    }

    @Override
    public IRoomEventChunk getEventsChunk(String from, long amount) {
        IProcessedEventStream stream = global.getEvMgr().getBackwardStreamFrom(from);
        List<IProcessedEvent> list = new ArrayList<>();

        while (list.size() < amount && stream.hasNext()) {
            IProcessedEvent ev = stream.getNext();
            if (!id.equals(ev.getRoomId())) {
                continue;
            }

            list.add(stream.getNext());

            if (RoomEventType.Creation.is(ev.getType())) {
                // This is the first event of the room, no need to go further
                break;
            }
        }

        RoomEventChunk.Builder builder = new RoomEventChunk.Builder();
        builder.setStartToken(from);
        builder.setEndToken(list.stream().map(ev -> ev.getSid().toString()).findFirst().orElse(from));
        list.forEach(ev -> builder.addEvent(ev.getJson()));

        return builder.get();
    }

    @Override
    public synchronized IRoomState getStateFor(String eventId) {
        return Caches.get(states, eventId);
    }

    @Override
    public JsonObject makeJoin(_MatrixID mxid) {
        JsonArray prevEvents = new JsonArray();
        getExtremities().stream()
                .map(ev -> new EventReference(ev.getId(), ev.getHashes()))
                .map(GsonUtil.get()::toJson)
                .forEach(prevEvents::add);

        JsonObject event = new JsonObject();
        event.addProperty(EventKey.Id.get(), "$makeJoinId:" + mxid.getDomain());
        event.addProperty(EventKey.Type.get(), RoomEventType.Membership.get());
        event.add(EventKey.AuthEvents.get(), GsonUtil.asArray(getCreation().getId()));
        event.add(EventKey.Content.get(), GsonUtil.makeObj(EventKey.Membership.get(), RoomMembership.Join.get()));
        event.addProperty(EventKey.Depth.get(), Integer.MAX_VALUE);
        event.addProperty(EventKey.Origin.get(), global.getDomain());
        event.addProperty(EventKey.Timestamp.get(), Instant.now().toEpochMilli());
        event.add(EventKey.PreviousEvents.get(), prevEvents);
        event.addProperty(EventKey.RoomId.get(), getId());
        event.addProperty(EventKey.Sender.get(), mxid.getId());
        event.addProperty(EventKey.StateKey.get(), mxid.getId());

        RoomEventAuthorization authEval = algo.authorize(getCurrentState(), new Event(event));
        if (!authEval.isAuthorized()) {
            throw new ForbiddenException(authEval.getReason());
        }

        return event;
    }

    @Override
    public synchronized RemoteJoinRoomState injectJoin(IEvent ev) {
        RoomState state = getCurrentState();
        RoomEventAuthorization eval = algo.authorize(state, ev);
        if (!eval.isAuthorized()) {
            log.debug("Room current state: {}", GsonUtil.getPrettyForLog(state));
            log.error(eval.getReason());
            throw new ForbiddenException("Unauthorized: " + eval.getReason());
        }

        inject(ev, eval);
        return new RemoteJoinRoomState(global.getEvMgr().getFull(state.getEvents().values()));
    }

    @Override
    public IRoomStateSnapshotIds getSnapshot(String eventId) {
        List<String> state = getStateFor(eventId).getEvents().values().stream()
                .map(IHashedProtoEvent::getId)
                .collect(Collectors.toList());

        Set<String> authChain = ForkJoinPool.commonPool().invoke(new GetAuthChainTask(state, s -> global.getEvMgr().get(s)));
        return new RoomStateSnapshotIds(state, authChain);
    }

    @Override
    public List<IEvent> getEventsRange(Collection<String> firstEvId, Collection<String> lastEvId, long limit, long minDepth) {
        int realLimit = Math.min(50, (int) limit);

        BlockingQueue<IEvent> events = new ArrayBlockingQueue<>(realLimit);
        RecursiveAction getEvTask = new GetChildEventsRecursiveAction(lastEvId, firstEvId, minDepth, events);
        ForkJoinPool.commonPool().execute(getEvTask);
        try {
            getEvTask.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>(events);
    }

    @Override
    public IRoom joinAs(_MatrixID userId) {
        inject(new RoomMembershipEvent(userId.getId(), RoomMembership.Join.get(), userId.getId()));
        return this;
    }

    // FIXME use RWLock
    @Override
    public synchronized List<IProcessedEvent> getExtremities() {
        return extremities.stream().map(id -> global.getEvMgr().get(id)).collect(Collectors.toList());
    }

    @Override
    public synchronized void inject(IEvent ev) { // FIXME use RWLock
        info("Injecting event {}", ev.getId());

        boolean isConnected = ev.getParents().stream()
                .map(IEventIdReference::getEventId)
                .allMatch(id -> global.getEvMgr().has(id));

        if (!isConnected) {
            JsonArray earliest = GsonUtil.asArray(extremities);
            JsonArray latest = GsonUtil.asArray(ev.getId());
        }

        IRoomState parentState = algo.resolve(getStates(ev));
        RoomEventAuthorization eval = algo.authorize(parentState, ev);
        if (!eval.isAuthorized()) {
            log.debug("Previous state: {}", GsonUtil.getPrettyForLog(parentState));
            log.error(eval.getReason());
            throw new ForbiddenException("Unauthorized event: " + eval.getReason());
        }
        inject(ev, eval);
    }

    @Override
    public void addListener(Object o) {
        eventBus.subscribe(o);
    }

    private class GetChildEventsRecursiveAction extends RecursiveAction {

        private Collection<String> parents;
        private Collection<String> childLimit;
        private long minDepth;
        private BlockingQueue<IEvent> sink;

        GetChildEventsRecursiveAction(
                Collection<String> parents,
                Collection<String> childLimit,
                long minDepth,
                BlockingQueue<IEvent> sink
        ) {
            this.parents = parents;
            this.childLimit = childLimit;
            this.minDepth = minDepth;
            this.sink = sink;
        }

        @Override
        protected void compute() {
            parents.forEach(evId -> {
                try {
                    IEvent ev = global.getEvMgr().get(evId);
                    if (ev.getDepth() >= minDepth && sink.offer(ev) && !childLimit.contains(ev.getId())) {
                        List<String> parents = ev.getParents().stream().map(IEventIdReference::getEventId).collect(Collectors.toList());
                        new GetChildEventsRecursiveAction(parents, childLimit, minDepth, sink).fork().join();
                    }
                } catch (IllegalArgumentException e) {
                    log.debug("Event {} is not known to use, skipping", evId);
                }
            });
        }
    }

}
