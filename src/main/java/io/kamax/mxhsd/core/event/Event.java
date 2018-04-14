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
import com.google.gson.reflect.TypeToken;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.EventReference;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IEventReference;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class Event extends ProtoEvent implements IEvent {

    private static final transient Type hashesType = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final transient Type signType = new TypeToken<Map<String, Map<String, String>>>() {
    }.getType();

    private Event(String raw, JsonObject o) {
        this(EventKey.Id.getStringOrThrow(o),
                EventKey.Type.getStringOrThrow(o),
                EventKey.Sender.getStringOrThrow(o),
                EventKey.RoomId.getStringOrThrow(o),
                EventKey.Timestamp.getElement(o).getAsLong(),
                EventKey.Depth.getElement(o).getAsLong(),
                GsonUtil.asList(EventKey.PreviousEvents.getElement(o).getAsJsonArray(), JsonArray.class).stream()
                        .map(EventReference::new).collect(Collectors.toList()),
                GsonUtil.asList(EventKey.AuthEvents.getElement(o).getAsJsonArray(), JsonArray.class).stream()
                        .map(EventReference::new).collect(Collectors.toList()),
                raw);
    }

    public Event(String id, String type, String sender, String roomId, long timestamp, long depth, Collection<IEventReference> parents, Collection<IEventReference> auth, String json) {
        super(id, type, sender, roomId, timestamp, depth, parents, auth, json);
    }

    public Event(String raw) {
        this(raw, GsonUtil.parseObj(raw));
    }

    public Event(JsonObject obj) {
        this(MatrixJson.encodeCanonical(obj), obj);
    }

    @Override
    public Map<String, String> getHashes() {
        return GsonUtil.get().fromJson(EventKey.Hashes.getObj(getJson()), hashesType);
    }

    @Override
    public Map<String, Map<String, String>> getSignatures() {
        return GsonUtil.get().fromJson(EventKey.Hashes.getObj(getJson()), signType);
    }

}
