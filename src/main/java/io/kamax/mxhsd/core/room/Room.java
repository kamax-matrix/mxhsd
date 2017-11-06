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

import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.event.ISignedEventStreamEntry;
import io.kamax.mxhsd.api.event.NakedContentEvent;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.HomeserverState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Room implements IRoom {

    private Logger log = LoggerFactory.getLogger(Room.class);

    private HomeserverState globalState;

    private String id;
    private RoomState state;
    private Map<String, RoomState> prevStates; // FIXME caching

    private BlockingQueue<ISignedEvent> extremities = new LinkedBlockingQueue<>();

    Room(HomeserverState globalState, String id) {
        this.globalState = globalState;
        this.id = id;
        this.prevStates = new ConcurrentHashMap<>();
        setCurrentState(new RoomState.Builder(globalState, id).build());

    }

    @Override
    public String getId() {
        return id;
    }

    // FIXME use RWLock
    private synchronized void setCurrentState(RoomState state) {
        this.state = state;
    }

    // FIXME use RWLock
    @Override
    public synchronized IRoomState getCurrentState() {
        return state;
    }

    // FIXME use RWLock
    @Override
    public synchronized ISignedEvent inject(NakedContentEvent evNaked) {
        List<ISignedEvent> parents = new ArrayList<>();
        extremities.drainTo(parents);
        try {
            log.info("Room {}: Injecting new event of type {}", id, evNaked.getType());
            IEvent ev = globalState.getEvMgr().populate(evNaked, getId(), state, parents);
            log.debug("Formalized event: {}", GsonUtil.getPrettyForLog(ev.getJson()));
            RoomEventAuthorization val = state.isAuthorized(ev);
            if (!val.isAuthorized()) {
                log.debug("Room current state: {}", GsonUtil.getPrettyForLog(state));
                log.error(val.getReason());
                throw new ForbiddenException("Unauthorized event");
            } else {
                RoomState.Builder stateBuilder = new RoomState.Builder(globalState, id).from(val.getNewState());
                log.info("Room {}: storing new event {}", id, ev.getId());
                ISignedEventStreamEntry entry = globalState.getEvMgr().store(ev);
                stateBuilder.withStreamIndex(entry.streamIndex());

                // We update extremities info
                extremities.add(entry.get());
                parents.clear();

                ISignedEvent evSigned = entry.get();
                log.debug("Signed event: {}", GsonUtil.getPrettyForLog(evSigned.getJson()));
                log.info("Room {}: event {} stored at index {}", id, evSigned.getId(), entry.streamIndex());

                if (RoomEventType.from(evSigned.getType()).isState()) {
                    log.debug("Room {} new state: {}", id, GsonUtil.getPrettyForLog(state));
                }

                RoomState newState = stateBuilder.build();
                prevStates.put(evSigned.getId(), newState);
                setCurrentState(newState);

                return evSigned;
            }
        } finally {
            extremities.addAll(parents);
        }
    }

    @Override
    public synchronized IRoomState getStateFor(String id) {
        // FIXME this is dumb, we need a way to calculate the state for an arbitrary event
        RoomState state = prevStates.get(id);
        if (state == null) {
            throw new RuntimeException("No previous state for event " + id + " - how?!");
        }

        return state;
    }

}
