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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IEventBuilder;
import io.kamax.mxhsd.api.event.ISignedEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class EventBuilder implements IEventBuilder {

    private String domain;

    private JsonObject base;
    private Instant timestamp = Instant.now();
    private String type;
    private long depth = 0;
    private Set<String> parents = new HashSet<>();
    private JsonArray authEv = new JsonArray();
    private JsonObject content = new JsonObject();
    private JsonObject prevState = new JsonObject();

    private Gson gson = GsonUtil.build();

    public EventBuilder(String domain, JsonObject base) {
        this.domain = domain;
        this.base = base;
        EventKey.Type.findString(base).ifPresent(v -> type = v);
    }

    @Override
    public IEventBuilder setTimestamp(Instant instant) {
        this.timestamp = instant;
        return this;
    }

    @Override
    public IEventBuilder setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public IEventBuilder addParent(ISignedEvent ev) {
        parents.add(ev.getId());
        if (depth <= ev.getDepth()) depth = ev.getDepth() + 1;
        return this;
    }

    @Override
    public Collection<String> getParents() {
        return new ArrayList<>(parents);
    }

    @Override
    public JsonArray getAuthEvents() {
        return authEv;
    }

    @Override
    public JsonObject getContent() {
        return content;
    }

    @Override
    public JsonObject getPrevState() {
        return prevState;
    }

    @Override
    public JsonObject getJson() {
        return base;
    }

    @Override
    public IEvent build(String id) {
        JsonArray pEv = new JsonArray();
        parents.forEach(pEv::add);

        base.add(EventKey.AuthEvents.get(), getAuthEvents());
        base.addProperty(EventKey.Depth.get(), depth);
        base.addProperty(EventKey.Id.get(), id);
        base.addProperty(EventKey.Origin.get(), domain);
        base.addProperty(EventKey.Timestamp.get(), timestamp.toEpochMilli());
        base.add(EventKey.PreviousState.get(), getPrevState());
        base.addProperty(EventKey.Type.get(), type);
        return new Event(id, type, depth, gson.toJson(base));
    }

}
