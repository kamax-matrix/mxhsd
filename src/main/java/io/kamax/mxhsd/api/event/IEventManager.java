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

package io.kamax.mxhsd.api.event;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.api.room.IRoomState;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface IEventManager {

    IProtoEventBuilder populate(INakedEvent ev, String roomId, IRoomState withState, List<? extends IEvent> parents);

    IHashedProtoEvent hash(IProtoEvent ev);

    IEvent sign(IHashedProtoEvent ev);

    default IEvent sign(IProtoEvent ev) {
        return sign(hash(ev));
    }

    IEvent finalize(JsonObject ev);

    default IProcessedEvent store(IProtoEvent ev) {
        return store(sign(ev));
    }

    IProcessedEvent store(IEvent ev);

    IProcessedEvent get(String id);

    List<IProcessedEvent> get(Collection<String> ids);

    default List<IProcessedEvent> getFull(Collection<? extends IProtoEvent> evs) {
        return get(evs.stream().map(IProtoEvent::getId).collect(Collectors.toList()));
    }

    // From newest to oldest in a linear graph
    IProcessedEventStream getBackwardStreamFrom(String position);

    IProcessedEventStream getForwardStreamFrom(String position);

    boolean isBefore(String toCheck, String reference);

    // MBassador bus
    // TODO consider refactoring this into accepting a consumer (functional interface) to abstract MBassador
    void addFilter(Object o);

    // MBassador bus
    // TODO consider refactoring this into accepting a consumer (functional interface) to abstract MBassador
    void addListener(Object o);

}
