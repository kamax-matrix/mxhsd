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
import com.google.gson.JsonObject;
import io.kamax.matrix.codec.MxSha256;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IEventBuilder;
import io.kamax.mxhsd.api.event.IEventManager;
import io.kamax.mxhsd.api.event.ISimpleEvent;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.HomeserverState;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventManager implements IEventManager {

    private final List<String> essentialTopKeys;
    private final Map<String, List<String>> essentialContentKeys = new HashMap<>();

    private HomeserverState hsState;
    private Gson gson = GsonUtil.build();
    private MxSha256 sha256 = new MxSha256();

    private Map<String, IEvent> events = new ConcurrentHashMap<>();

    public EventManager(HomeserverState hsState) {
        this.hsState = hsState;

        essentialTopKeys = Arrays.asList(
                "auth_events",
                "content",
                "depth",
                "event_id",
                "hashes",
                "membership",
                "origin",
                "origin_server_ts",
                "prev_events",
                "prev_state",
                "room_id",
                "sender",
                "signatures",
                "states_key",
                "type"
        );

        essentialContentKeys.put(RoomEventType.Aliases.getId(), Collections.singletonList("aliases"));
        essentialContentKeys.put(RoomEventType.Creation.getId(), Collections.singletonList("creator"));
        essentialContentKeys.put(RoomEventType.HistoryVisiblity.getId(), Collections.singletonList("history_visiblity"));
        essentialContentKeys.put(RoomEventType.JoinRules.getId(), Collections.singletonList("join_rule"));
        essentialContentKeys.put(RoomEventType.Membership.getId(), Collections.singletonList("membership"));
        essentialContentKeys.put(RoomEventType.PowerLevels.getId(), Arrays.asList(
                "ban",
                "events",
                "events_default",
                "kick",
                "redact",
                "state_default",
                "users",
                "users_default"
        ));
    }

    private synchronized String getNextId() { // TODO find a better way than synchronized
        return ("$" + Long.toString(System.currentTimeMillis()) +
                RandomStringUtils.randomAscii(4) + ":" + hsState.getDomain()).toLowerCase();
    }

    private IEventBuilder createEventImpl(Consumer<IEventBuilder> c) {
        IEventBuilder builder = new EventBuilder(hsState.getDomain());
        c.accept(builder);
        return builder;
    }

    private JsonObject hashEvent(JsonObject o) {
        o.remove("signatures");
        o.remove("unsigned");
        o.remove("hashes");
        String canonical = MatrixJson.encodeCanonical(o);

        JsonObject hashes = new JsonObject();
        hashes.addProperty("sha256", sha256.hash(canonical));
        o.add("hashes", hashes);
        return o;
    }

    private JsonObject signEvent(JsonObject original) {
        if (!original.has("hashes")) {
            throw new IllegalStateException("Event cannot be signed: missing hashes key");
        }

        JsonObject o = gson.fromJson(gson.toJson(original), JsonObject.class);
        o.keySet().forEach(key -> {
            if (!essentialTopKeys.contains(key)) o.remove(key);
        });

        if (o.has("content")) {
            JsonObject content = o.get("content").getAsJsonObject();
            List<String> essentials = essentialContentKeys.getOrDefault(content.get("type").getAsString(), Collections.emptyList());
            content.keySet().forEach(key -> {
                if (!essentials.contains(key)) content.remove(key);
            });
        }

        JsonObject signs = hsState.getSignMgr().signMessageGson(MatrixJson.encodeCanonical(o));
        original.add("signatures", signs);
        return original;
    }

    public IEvent createEvent(Consumer<IEventBuilder> c) {
        IEventBuilder builder = createEventImpl(c);
        String id = getNextId();
        JsonObject obj = signEvent(hashEvent(builder.build(id)));
        return new Event(id, gson.toJson(obj));
    }

    @Override
    public IEvent buildEvent(Consumer<IEventBuilder> c) {
        throw new NotImplementedException("");
    }

    @Override
    public IEvent buildEvent(JsonObject o) {
        throw new NotImplementedException("");
    }

    @Override
    public IEvent buildEvent(ISimpleEvent ev) {
        throw new NotImplementedException("");
    }

    @Override
    public void storeEvent(IEvent ev) {
        throw new NotImplementedException("");
    }

    @Override
    public IEvent getEvent(String id) {
        IEvent ev = events.get(id);
        if (ev == null) {
            throw new IllegalArgumentException("Event ID " + id + " does not exist");
        }

        return ev;
    }

}
