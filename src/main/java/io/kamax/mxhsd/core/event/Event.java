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

import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IEventReference;

import java.time.Instant;
import java.util.*;

public class Event implements IEvent {

    private String id;
    private String type;
    private String roomId;
    private String sender;
    private long timestamp;
    private long depth;
    private Set<IEventReference> parents;
    private Set<IEventReference> authorization;
    private String json;

    public Event(String id, String type, String sender, String roomId, long timestamp, long depth, Collection<IEventReference> parents, Collection<IEventReference> auth, String json) {
        this.id = id;
        this.type = type;
        this.sender = sender;
        this.roomId = roomId;
        this.timestamp = timestamp;
        this.depth = depth;
        this.parents = new HashSet<>(parents);
        this.authorization = new HashSet<>(auth);
        this.json = json;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Instant getTimestamp() {
        return Instant.ofEpochMilli(timestamp);
    }

    @Override
    public String getOrigin() {
        return GsonUtil.getString(getJson(), EventKey.Origin.get());
    }

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getSender() {
        return sender;
    }

    @Override
    public long getDepth() {
        return depth;
    }

    @Override
    public List<IEventReference> getParents() {
        return Collections.unmodifiableList(new ArrayList<>(parents));
    }

    @Override
    public List<IEventReference> getAuthorization() {
        return Collections.unmodifiableList(new ArrayList<>(authorization));
    }

    @Override
    public JsonObject getJson() {
        return GsonUtil.parseObj(json);
    }

}
