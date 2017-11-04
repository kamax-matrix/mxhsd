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
import io.kamax.mxhsd.api.event.NakedRoomEvent;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.HomeserverState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Room implements IRoom {

    private Logger log = LoggerFactory.getLogger(Room.class);

    private HomeserverState globalState;

    private String id;
    private RoomState state;

    Room(HomeserverState globalState, String id) {
        this.globalState = globalState;
        this.id = id;
        state = new RoomState.Builder(globalState, id).build();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized IRoomState getCurrentState() { // FIXME use RWLock
        return state;
    }

    @Override
    public synchronized ISignedEvent inject(NakedRoomEvent evNaked) { // FIXME use RWLock
        log.info("Room {}: Injecting new event of type {}", id, evNaked.getType());
        IEvent ev = globalState.getEvMgr().populate(evNaked, state.getExtremities());
        log.debug("Formalized event: {}", GsonUtil.getPrettyForLog(ev.getJson()));
        RoomEventAuthorization val = state.isAuthorized(ev);
        if (!val.isAuthorized()) {
            log.debug("Room current state: {}", GsonUtil.getPrettyForLog(state));
            log.error(val.getReason());
            throw new ForbiddenException("Unauthorized event");
        } else {
            log.info("Room {}: storing new event {}", id, ev.getId());
            ISignedEventStreamEntry entry = globalState.getEvMgr().store(ev);
            ISignedEvent evSigned = entry.get();
            log.info("Room {}: event {} stored at index {}", id, evSigned.getId(), entry.streamIndex());
            if (RoomEventType.from(evSigned.getType()).isState()) {
                log.info("Room {}: updating state", id);
                log.debug("Room current state: {}", GsonUtil.getPrettyForLog(state));
                state = new RoomState.Builder(globalState, id).from(val.getNewState()).setExtremities(evSigned).build();
                log.debug("Room new state: {}", GsonUtil.getPrettyForLog(state));
            }
            return evSigned;
        }
    }

}
