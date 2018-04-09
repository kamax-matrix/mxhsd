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
import io.kamax.mxhsd.api.event.*;

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
    private Set<IEventReference> authorization = new HashSet<>();
    private Set<IEventReference> parents = new HashSet<>();

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
    public IEventBuilder addAuthorization(IEventReference ref) {
        authorization.add(new EventReference(ref.getEventId(), ref.getHashes()));
        return this;
    }

    @Override
    public IEventBuilder addParent(ISignedEvent ev) {
        parents.add(new EventReference(ev.getId(), ev.getHashes()));
        if (depth <= ev.getDepth()) depth = ev.getDepth() + 1;
        return this;
    }

    @Override
    public Event get() {
        JsonArray aEv = new JsonArray();
        authorization.forEach(p -> {
            JsonArray v = new JsonArray();
            v.add(p.getEventId());
            v.add(GsonUtil.makeObj(p.getHashes()));
            aEv.add(v);
        });
        JsonArray pEv = new JsonArray();
        parents.forEach(p -> {
            JsonArray v = new JsonArray();
            v.add(p.getEventId());
            v.add(GsonUtil.makeObj(p.getHashes()));
            pEv.add(v);
        });

        base.add(EventKey.AuthEvents.get(), aEv);
        base.addProperty(EventKey.Depth.get(), depth);
        base.addProperty(EventKey.Id.get(), id);
        base.addProperty(EventKey.Origin.get(), origin);
        base.addProperty(EventKey.RoomId.get(), roomId);
        base.addProperty(EventKey.Timestamp.get(), timestamp.toEpochMilli());
        base.add(EventKey.PreviousEvents.get(), pEv);

        // SYNAPSE-BUG
        // This is required for synapse to accept various events event tho the spec does not mandate it
        // Legacy key?
        if (!base.has(EventKey.PreviousState.get())) {
            base.add(EventKey.PreviousState.get(), new JsonArray());
        }

        String json = MatrixJson.encodeCanonical(base);
        return new Event(
                id,
                GsonUtil.getOrThrow(base, EventKey.Type.get()),
                GsonUtil.getOrThrow(base, EventKey.Sender.get()),
                GsonUtil.getOrThrow(base, EventKey.RoomId.get()),
                EventKey.Timestamp.getElement(base).getAsLong(),
                depth,
                new HashSet<>(parents),
                new HashSet<>(authorization),
                json
        );
    }

}
