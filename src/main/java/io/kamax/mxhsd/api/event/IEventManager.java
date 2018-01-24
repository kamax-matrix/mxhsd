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

package io.kamax.mxhsd.api.event;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.api.room.IRoomState;

import java.util.Collection;
import java.util.List;

public interface IEventManager {

    IEventBuilder populate(INakedEvent ev, String roomId, IRoomState withState, List<ISignedEvent> parents);

    ISignedEvent sign(IEvent ev);

    ISignedEvent finalize(JsonObject ev);

    default ISignedEventStreamEntry store(IEvent ev) {
        return store(sign(ev));
    }

    ISignedEventStreamEntry store(ISignedEvent ev);

    ISignedEventStreamEntry get(String id);

    List<ISignedEvent> get(Collection<String> ids);

    // From newest to oldest in a linear graph
    ISignedEventStream getBackwardStreamFrom(int id);

    int getStreamIndex();

    // MBassador bus
    // TODO consider refactoring this into accepting a consumer (functional interface) to abstract MBassador
    void addFilter(Object o);

    // MBassador bus
    // TODO consider refactoring this into accepting a consumer (functional interface) to abstract MBassador
    void addListener(Object o);

}
