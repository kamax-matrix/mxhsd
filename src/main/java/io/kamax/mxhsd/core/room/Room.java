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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.*;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.exception.InvalidRequestException;
import io.kamax.mxhsd.api.room.*;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.core.HomeserverState;
import net.engio.mbassy.bus.MBassador;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Room implements IRoom {

    private Logger log = LoggerFactory.getLogger(Room.class);

    private HomeserverState global;

    private String id;
    private RoomState state;
    private MBassador<ISignedEvent> eventBus;
    private Map<String, RoomState> prevStates;

    private BlockingQueue<ISignedEvent> extremities = new LinkedBlockingQueue<>();

    public Room(HomeserverState global, String id) {
        this.global = global;
        this.id = id;
        this.prevStates = new ConcurrentHashMap<>();
        this.eventBus = new MBassador<>();
        setCurrentState(new RoomState.Builder(global, id).build());
    }

    private Comparator<ISignedEvent> getEventComparator() {
        return Comparator.comparingLong(ISignedEvent::getDepth)
                .thenComparing(ISignedEvent::getTimestamp);
    }

    public Room(HomeserverState global, String id, List<ISignedEvent> initialState, List<ISignedEvent> authChain, ISignedEvent seed) {
        this(global, id);

        // We order events so we can build a state properly
        List<ISignedEvent> c = authChain.stream().unordered()
                .distinct()
                // FIXME compute and use topological order
                .sorted(getEventComparator())
                .collect(Collectors.toList());

        List<ISignedEvent> s = initialState.stream().unordered()
                .distinct()
                // FIXME compute and use topological order
                .sorted(getEventComparator())
                .collect(Collectors.toList());

        RoomState.Builder b = new RoomState.Builder(global, id);
        //c.forEach(b::addEvent);
        s.forEach(b::addEvent);
        b.withStreamIndex(seed.getId());
        extremities.add(seed);
        setCurrentState(b.build());

        c.forEach(e -> global.getEvMgr().store(e));
        s.forEach(e -> global.getEvMgr().store(e));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public IEvent getCreation() {
        return state.getCreation();
    }

    // FIXME use RWLock
    private synchronized void setCurrentState(RoomState state) {
        if (state.getEventId() != null) prevStates.put(state.getEventId(), state);
        this.state = state;
    }

    // FIXME use RWLock
    @Override
    public synchronized RoomState getCurrentState() {
        return state;
    }

    // FIXME use RWLock
    @Override
    public synchronized ISignedEvent inject(NakedContentEvent evNaked) {
        List<ISignedEvent> parents = new ArrayList<>();
        extremities.drainTo(parents);
        try {
            log.info("Room {}: Injecting new event of type {}", id, evNaked.getType());
            IEventBuilder evTempBuilder = global.getEvMgr().populate(evNaked, getId(), state, parents);
            IEvent evTemp = evTempBuilder.get();
            log.debug("Formalized event: {}", GsonUtil.getPrettyForLog(evTemp.getJson()));
            RoomEventAuthorization val = state.isAuthorized(evTemp);
            if (!val.isAuthorized()) {
                log.debug("Room current state: {}", GsonUtil.getPrettyForLog(state));
                log.error(val.getReason());
                throw new ForbiddenException("Unauthorized event");
            } else {
                val.getBasedOn().forEach(evTempBuilder::addAuthorization);
                IEvent ev = evTempBuilder.get();
                RoomState.Builder stateBuilder = new RoomState.Builder(global, id).from(val.getNewState());
                log.info("Room {}: storing new event {}", id, ev.getId());
                ISignedEventStreamEntry entry = global.getEvMgr().store(ev);
                stateBuilder.withStreamIndex(ev.getId());

                // We update extremities info
                extremities.add(entry.get());
                parents.clear();

                ISignedEvent evSigned = entry.get();
                log.debug("Signed event: {}", GsonUtil.getPrettyForLog(evSigned.getJson()));
                log.info("Room {}: event {} stored at index {}", id, evSigned.getId(), entry.streamIndex());
                boolean changed = stateBuilder.addEvent(evSigned);

                RoomState newState = stateBuilder.build();
                prevStates.put(evSigned.getId(), newState);
                setCurrentState(newState);

                if (changed) {
                    log.debug("Room {} new state: {}", id, GsonUtil.getPrettyForLog(state));
                }

                eventBus.publish(evSigned);
                return evSigned;
            }
        } finally {
            extremities.addAll(parents);
        }
    }

    @Override
    public synchronized IRoomState getStateFor(String id) {
        // FIXME this is dumb, we need a way to calculate the state for an arbitrary event
        return Optional.ofNullable(prevStates.get(id)).orElseGet(() -> {
            RoomState.Builder b = new RoomState.Builder(global, this.id);
            b.addEvent(getCurrentState().getCreation());
            return b.build();
        });
    }

    @Override
    public IRoomEventChunk getEventsChunk(String from, int amount) {
        try {
            return getEventsChunk(Integer.parseInt(from), amount);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("From token is not in a valid format");
        }
    }

    private IRoomEventChunk getEventsChunk(int streamIndex, int amount) {
        ISignedEventStream stream = global.getEvMgr().getBackwardStreamFrom(streamIndex);
        List<ISignedEventStreamEntry> list = new ArrayList<>();
        int toFetch = amount;

        while (list.size() < amount) {
            List<ISignedEventStreamEntry> rawList = stream.getNext(toFetch).stream()
                    .filter(ev -> StringUtils.equals(id, ev.get().getRoomId())) // only events about this room.
                    .collect(Collectors.toList());

            if (rawList.isEmpty()) {
                // No more events at all, we stop
                break;
            }

            list.addAll(rawList);

            if (RoomEventType.Creation.is(rawList.get(rawList.size() - 1).get().getType())) {
                // This is the first event of the room, no need to go further
                break;
            }

            toFetch = amount - list.size();
        }

        RoomEventChunk.Builder builder = new RoomEventChunk.Builder();
        builder.setStartToken(Integer.toString(streamIndex));
        builder.setEndToken(Integer.toString(list.stream()
                .mapToInt(ISignedEventStreamEntry::streamIndex)
                .min()
                .orElse(streamIndex)));
        list.forEach(ev -> builder.addEvent(ev.get().getJson()));

        return builder.get();
    }

    @Override
    public JsonObject makeJoin(_MatrixID mxid) {
        // FIXME check if a join would be allowed

        JsonArray prevEvents = new JsonArray();
        global.getEvMgr().getBackwardStreamFrom(state.getStreamIndex())
                .getNext(1)
                .forEach(e -> prevEvents.add(e.get().getId()));

        JsonObject event = new JsonObject();
        event.addProperty(EventKey.Type.get(), RoomEventType.Membership.get());
        event.add(EventKey.AuthEvents.get(), GsonUtil.asArray(getCreation().getId()));
        event.add("content", GsonUtil.makeObj("membership", RoomMembership.Join.get()));
        event.addProperty(EventKey.Depth.get(), Integer.MAX_VALUE);
        event.addProperty(EventKey.Origin.get(), global.getDomain());
        event.addProperty(EventKey.Timestamp.get(), Instant.now().toEpochMilli());
        event.add(EventKey.PreviousEvents.get(), prevEvents);
        event.addProperty(EventKey.RoomId.get(), getId());
        event.addProperty(EventKey.Sender.get(), mxid.getId());
        event.addProperty(EventKey.StateKey.get(), mxid.getId());

        return event;
    }

    @Override
    public synchronized RemoteJoinRoomState injectJoin(ISignedEvent ev) {
        RoomState state = getCurrentState();
        RoomEventAuthorization eval = state.isAuthorized(ev);
        if (!eval.isAuthorized()) {
            log.debug("Room current state: {}", GsonUtil.getPrettyForLog(state));
            log.error(eval.getReason());
            throw new ForbiddenException("Unauthorized federated event");
        }

        RoomState.Builder stateBuilder = new RoomState.Builder(global, id).from(eval.getNewState());
        log.info("Room {}: storing federated event {}", id, ev.getId());
        ISignedEventStreamEntry entry = global.getEvMgr().store(ev);
        stateBuilder.withStreamIndex(ev.getId());

        // We update extremities info
        // FIXME we should clean up those which we are aware of
        extremities.add(entry.get());
        boolean changed = stateBuilder.addEvent(ev);

        RoomState newState = stateBuilder.build();
        prevStates.put(ev.getId(), newState);
        setCurrentState(newState);

        if (changed) {
            log.debug("Room {} new state: {}", id, GsonUtil.getPrettyForLog(state));
        }
        eventBus.publish(ev);

        Collection<String> eventIds = state.getEvents().values();
        List<ISignedEvent> events = global.getEvMgr().get(eventIds);
        return new RemoteJoinRoomState(events);
    }

    @Override
    public List<ISignedEvent> getEventsRange(Collection<String> firstEvId, Collection<String> lastEvId, long limit, long minDepth) {
        int realLimit = Math.min(50, (int) limit);

        BlockingQueue<ISignedEvent> events = new ArrayBlockingQueue<>(realLimit);
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

    @Override
    public synchronized void inject(ISignedEvent ev) { // FIXME use RWLock
        Optional<RoomState> parentOpt = ev.getParents().stream()
                .filter(ref -> prevStates.containsKey(ref.getEventId()))
                .map(ref -> global.getEvMgr().get(ref.getEventId()).get())
                .max(getEventComparator()).map(e -> prevStates.get(e.getId()));

        parentOpt.ifPresent(parent -> {
            RoomEventAuthorization eval = parent.isAuthorized(ev);
            if (!eval.isAuthorized()) {
                log.debug("Room current state: {}", GsonUtil.getPrettyForLog(state));
                log.error(eval.getReason());
                throw new ForbiddenException("Unauthorized federated event");
            }

            RoomState newState = new RoomState.Builder(global, id).from(eval.getNewState()).build();
            prevStates.put(ev.getId(), newState);
            setCurrentState(newState);
        });

        global.getEvMgr().store(ev);
        extremities.add(ev);
        eventBus.publish(ev);
    }

    @Override
    public void addListener(Object o) {
        eventBus.subscribe(o);
    }

    private class GetChildEventsRecursiveAction extends RecursiveAction {

        private Collection<String> parents;
        private Collection<String> childLimit;
        private long minDepth;
        private BlockingQueue<ISignedEvent> sink;

        public GetChildEventsRecursiveAction(Collection<String> parents, Collection<String> childLimit, long minDepth, BlockingQueue<ISignedEvent> sink) {
            this.parents = parents;
            this.childLimit = childLimit;
            this.minDepth = minDepth;
            this.sink = sink;
        }

        @Override
        protected void compute() {
            parents.forEach(evId -> {
                ISignedEvent ev = global.getEvMgr().get(evId).get();
                if (ev.getDepth() >= minDepth && sink.offer(ev) && !childLimit.contains(ev.getId())) {
                    List<String> parents = ev.getParents().stream().map(IEventIdReference::getEventId).collect(Collectors.toList());
                    new GetChildEventsRecursiveAction(parents, childLimit, minDepth, sink).fork().join();
                }
            });
        }
    }

}
