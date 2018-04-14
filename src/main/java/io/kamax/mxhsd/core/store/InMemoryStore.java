/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxhsd.core.store;

import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IProcessedEvent;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.store.IStore;
import io.kamax.mxhsd.core.event.ProcessedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryStore implements IStore {

    private Map<String, IProcessedEvent> events = new HashMap<>();

    @Override
    public Optional<IProcessedEvent> findEvent(String id) {
        return Optional.ofNullable(events.get(id));
    }

    @Override
    public synchronized IProcessedEvent putEvent(IEvent event) {
        IProcessedEvent pEv = new ProcessedEvent(events.size() + "", event);
        events.put(pEv.getId(), pEv);
        return pEv;
    }

    @Override
    public void findRoomState(String eventId) {

    }

    @Override
    public void putRoomState(IRoomState state, IProcessedEvent event) {

    }

}
