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

package io.kamax.mxhsd.core.room;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.*;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.room.*;
import io.kamax.mxhsd.api.room.event.EventComparator;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.event.Event;
import io.kamax.mxhsd.core.event.GetAuthChainTask;
import io.kamax.mxhsd.core.event.NakedContentEvent;
import io.kamax.mxhsd.core.room.algo.v1.RoomAlgorithm_v1;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Room implements IRoom {

    private Logger log = LoggerFactory.getLogger(Room.class);

    private HomeserverState global;
    private IRoomAlgorithm algo;

    private String id;
    private RoomState state;
    private MBassador<IEvent> eventBus;
    private Map<String, RoomState> prevStates;

    private List<String> extremities = new ArrayList<>();

    public Room(HomeserverState global, String id) {
        this.global = global;
        this.id = id;
        this.prevStates = new ConcurrentHashMap<>();
        this.eventBus = new MBassador<>(new IPublicationErrorHandler.ConsoleLogger(true));
        this.algo = new RoomAlgorithm_v1(eventId -> this.global.getEvMgr().get(eventId));
        setCurrentState(RoomState.build().get());
    }

    private Comparator<IEvent> getEventComparator() {
        return ((Comparator<IEvent>) (o1, o2) -> {
            boolean isO1Parent = Streams.concat(o2.getAuthorization().stream(), o2.getParents().stream())
                    .anyMatch(ref -> o1.getId().equals(ref.getEventId()));
            if (isO1Parent) return -1;

            boolean isO2Parent = Streams.concat(o1.getAuthorization().stream(), o1.getParents().stream())
                    .anyMatch(ref -> o2.getId().equals(ref.getEventId()));
            if (isO2Parent) return 1;

            return 0;
        }).thenComparingLong(IEvent::getDepth)
                .thenComparing(IEvent::getTimestamp);
    }

    public Room(HomeserverState global, String id, List<IEvent> initialState, List<IEvent> authChain, IEvent seed) {
        this(global, id);

        // We order events so we can build a state properly
        List<IEvent> chain = authChain.stream().unordered()
                .distinct()
                // FIXME compute and use topological order
                .sorted(getEventComparator())
                .collect(Collectors.toList());

        // We order events so we can build a state properly
        List<IEvent> state = initialState.stream().unordered()
                .distinct()
                // FIXME compute and use topological order
                .sorted(getEventComparator())
                .collect(Collectors.toList());

        chain.forEach(e -> global.getEvMgr().store(e));
        state.forEach(e -> global.getEvMgr().store(e));

        RoomState.Builder b = RoomState.build();
        state.forEach(b::addEvent);
        state.add(seed);
        inject(seed, b.get());
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
        return ev.getParents().stream()
                .map(ref -> global.getEvMgr().get(ref.getEventId()))
                .map(parentEv -> prevStates.get(parentEv.getId())).collect(Collectors.toList());
    }

    // FIXME use RWLock
    private synchronized void addExtremity(IEvent ev) {
        log.info("{}: Adding extremity {}", id, ev.getId());

        // We remove parents that were listed as extremities
        ev.getParents().forEach(ref -> extremities.remove(ref.getEventId()));

        // We add the current event as extremity
        extremities.add(ev.getId());

        // We calculate the new current state of the room
        setCurrentState(RoomState.build()
                .from(algo.resolve(
                        global.getEvMgr().get(extremities).stream()
                                .map(id -> prevStates.get(id.getId()))
                                .collect(Collectors.toList()))
                ).get()
        );
    }

    // FIXME use RWLock
    private synchronized IProcessedEvent inject(IEvent ev, RoomState state) {
        prevStates.put(ev.getId(), state);
        IProcessedEvent evStored = global.getEvMgr().store(ev);
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
        builder.setEndToken(list.stream().map(IProcessedEvent::getInternalId).findFirst().orElse(from));
        list.forEach(ev -> builder.addEvent(ev.getJson()));

        return builder.get();
    }

    @Override
    public synchronized IRoomState getStateFor(String id) {
        // FIXME this is dumb, we need a way to calculate the state for an arbitrary event
        return Optional.ofNullable(prevStates.get(id)).orElseGet(() -> {
            RoomState.Builder b = RoomState.build();
            b.addEvent(getCurrentState().getCreation());
            return b.get();
        });
    }

    @Override
    public JsonObject makeJoin(_MatrixID mxid) {
        JsonArray prevEvents = new JsonArray();
        getExtremities().stream()
                .map(ev -> new EventReference(ev.getId(), ev.getHashes()))
                .map(GsonUtil.get()::toJson)
                .forEach(prevEvents::add);

        JsonObject event = new JsonObject();
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
    public IRoomStateSnapshot getSnapshot(String eventId) {
        List<String> state = getStateFor(eventId).getEvents().values().stream()
                .map(IHashedProtoEvent::getId)
                .collect(Collectors.toList());

        Set<String> authChain = ForkJoinPool.commonPool().invoke(new GetAuthChainTask(state, s -> global.getEvMgr().get(s)));
        return new RoomStateSnapshot(state, authChain);
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
        IRoomState parentState = algo.resolve(getStates(ev));
        RoomEventAuthorization eval = algo.authorize(parentState, ev);
        if (!eval.isAuthorized()) {
            log.debug("Previous state: {}", GsonUtil.getPrettyForLog(parentState));
            log.error(eval.getReason());
            throw new ForbiddenException("Unauthorized event");
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
