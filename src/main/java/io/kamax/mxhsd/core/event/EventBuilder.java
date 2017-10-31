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
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IEventBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class EventBuilder implements IEventBuilder {

    private String domain;

    private Instant timestamp = Instant.now();
    private String type;
    private long depth = 1;
    private Set<String> parents = new HashSet<>();
    private JsonArray authEv = new JsonArray();
    private JsonObject content = new JsonObject();
    private JsonObject prevState = new JsonObject();
    private JsonObject ev = new JsonObject();

    private Gson gson = GsonUtil.build();

    public EventBuilder(String domain) {
        this.domain = domain;
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
    public IEventBuilder addParent(IEvent ev) {
        parents.add(ev.getId());
        if (depth <= ev.getDepth()) depth++;
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
        return ev;
    }

    @Override
    public JsonObject build(String eventId) {
        JsonArray pEv = new JsonArray();
        this.parents.forEach(pEv::add);

        ev.add("auth_events", getAuthEvents());
        ev.addProperty("depth", depth);
        ev.addProperty("event_id", eventId);
        ev.addProperty("origin", domain);
        ev.addProperty("origin_server_ts", timestamp.toEpochMilli());
        ev.add("prev_state", getPrevState());
        ev.addProperty("type", type);
        return ev;
    }

}
