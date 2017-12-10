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

package io.kamax.mxhsd.core.event;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEventBuilder;
import io.kamax.mxhsd.api.event.INakedEvent;
import io.kamax.mxhsd.api.event.ISignedEvent;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class EventBuilder implements IEventBuilder {

    private JsonObject base;
    private String id;
    private Instant timestamp;
    private String origin;
    private String roomId;
    private long depth = 0;
    private Set<String> authorization = new HashSet<>();
    private Set<String> parents = new HashSet<>();

    public EventBuilder(INakedEvent base) {
        this.base = GsonUtil.parseObj(GsonUtil.get().toJson(base));
    }

    @Override
    public IEventBuilder setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public IEventBuilder setTimestamp(Instant instant) {
        this.timestamp = instant;
        return this;
    }

    @Override
    public IEventBuilder setOrigin(String origin) {
        this.origin = origin;
        return this;
    }

    @Override
    public IEventBuilder setRoomId(String roomId) {
        this.roomId = roomId;
        return this;
    }

    @Override
    public IEventBuilder addAuthorization(String evId) {
        authorization.add(evId);
        return this;
    }

    @Override
    public IEventBuilder addParent(ISignedEvent ev) {
        parents.add(ev.getId());
        if (depth <= ev.getDepth()) depth = ev.getDepth() + 1;
        return this;
    }

    @Override
    public Event get() {
        JsonArray aEv = new JsonArray();
        authorization.forEach(aEv::add);
        JsonArray pEv = new JsonArray();
        parents.forEach(pEv::add);

        base.add(EventKey.AuthEvents.get(), aEv);
        base.addProperty(EventKey.Depth.get(), depth);
        base.addProperty(EventKey.Id.get(), id);
        base.addProperty(EventKey.Origin.get(), origin);
        base.addProperty(EventKey.RoomId.get(), roomId);
        base.addProperty(EventKey.Timestamp.get(), timestamp.toEpochMilli());
        base.add(EventKey.PreviousEvents.get(), pEv);

        String json = MatrixJson.encodeCanonical(base);
        return new Event(
                id,
                GsonUtil.getOrThrow(base, EventKey.Type.get()),
                GsonUtil.getOrThrow(base, EventKey.Sender.get()),
                GsonUtil.getOrThrow(base, EventKey.RoomId.get()),
                depth,
                new HashSet<>(parents),
                new HashSet<>(authorization),
                json
        );
    }

}
