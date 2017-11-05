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
import io.kamax.matrix.codec.MxBase64;
import io.kamax.matrix.codec.MxSha256;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.*;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.HomeserverState;
import net.engio.mbassy.bus.MBassador;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager implements IEventManager {

    private final List<String> essentialTopKeys;
    private final Map<String, List<String>> essentialContentKeys = new HashMap<>();

    private HomeserverState hsState;
    private Gson gson = GsonUtil.build();
    private MxSha256 sha256 = new MxSha256();

    private List<ISignedEventStreamEntry> eventsStream = Collections.synchronizedList(new ArrayList<>());
    private Map<String, ISignedEventStreamEntry> events = new ConcurrentHashMap<>();

    private MBassador<ISignedEventStreamEntry> eventBus = new MBassador<>();

    // FIXME enums
    public EventManager(HomeserverState hsState) {
        this.hsState = hsState;

        essentialTopKeys = Arrays.asList(
                EventKey.AuthEvents.get(),
                EventKey.Content.get(),
                EventKey.Depth.get(),
                EventKey.Id.get(),
                EventKey.Hashes.get(),
                "membership", // FIXME check this is actually required to hash/sign? and where is this ever set? spec is outdated?
                EventKey.Origin.get(),
                EventKey.Timestamp.get(),
                EventKey.PreviousEvents.get(),
                EventKey.PreviousState.get(),
                EventKey.RoomId.get(),
                EventKey.Sender.get(),
                EventKey.Signatures.get(),
                EventKey.StateKey.get(),
                EventKey.Type.get()
        );

        essentialContentKeys.put(RoomEventType.Aliases.get(), Collections.singletonList("aliases"));
        essentialContentKeys.put(RoomEventType.Creation.get(), Collections.singletonList("creator"));
        essentialContentKeys.put(RoomEventType.HistoryVisibility.get(), Collections.singletonList("history_visiblity"));
        essentialContentKeys.put(RoomEventType.JoinRules.get(), Collections.singletonList("join_rule"));
        essentialContentKeys.put(RoomEventType.Membership.get(), Collections.singletonList("membership"));
        essentialContentKeys.put(RoomEventType.PowerLevels.get(), Arrays.asList(
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

    // TODO find a better way than synchronized
    // TODO Externalize into dedicated class
    private synchronized String getNextId() {
        String local = MxBase64.encode(Long.toString(System.currentTimeMillis()) +
                RandomStringUtils.randomAlphabetic(4));
        return "$" + local + ":" + hsState.getDomain();
    }

    @Override
    public IEvent populate(INakedEvent ev, IRoomState roomState) {
        return new EventBuilder(hsState.getDomain(), ev.getJson()).addParents(roomState.getExtremities()).build(getNextId());
    }

    private JsonObject hash(JsonObject base) {
        base.remove(EventKey.Hashes.get());
        base.remove(EventKey.Signatures.get());
        base.remove(EventKey.Unsigned.get());
        String canonical = MatrixJson.encodeCanonical(base);

        JsonObject hashes = new JsonObject();
        hashes.addProperty("sha256", sha256.hash(canonical)); // FIXME do not hardcode
        base.add(EventKey.Hashes.get(), hashes);
        return base;
    }

    private JsonObject sign(JsonObject base) {
        JsonObject signBase = gson.fromJson(gson.toJson(base), JsonObject.class); // TODO how to do better?

        signBase.keySet().forEach(key -> {
            if (!essentialTopKeys.contains(key)) signBase.remove(key);
        });

        JsonObject content = EventKey.Content.getObj(signBase);
        List<String> essentials = essentialContentKeys.getOrDefault(EventKey.Type.getString(signBase), Collections.emptyList());
        JsonObject newContent = new JsonObject();
        content.keySet().forEach(key -> {
            if (essentials.contains(key)) newContent.remove(key);
        });
        signBase.add(EventKey.Content.get(), newContent);

        return hsState.getSignMgr().signMessageGson(MatrixJson.encodeCanonical(base));
    }

    @Override
    public ISignedEvent sign(IEvent ev) {
        JsonObject base = hash(ev.getJson());
        JsonObject signs = sign(base);
        base.add(EventKey.Signatures.get(), signs);
        return new SignedEvent(ev.getId(), ev.getType(), ev.getSender(), ev.getRoomId(), ev.getDepth(), MatrixJson.encodeCanonical(base));
    }

    @Override
    public synchronized ISignedEventStreamEntry store(ISignedEvent ev) { // FIXME use RWLock
        ISignedEventStreamEntry entry = new SignedEventStreamEntry(eventsStream.size(), ev);
        eventsStream.add(entry);
        events.put(ev.getId(), entry);

        eventBus.post(entry).now(); // TODO we might want to do this async?

        return entry;
    }

    @Override
    public ISignedEventStreamEntry get(String id) {
        ISignedEventStreamEntry ev = events.get(id);
        if (ev == null) {
            throw new IllegalArgumentException("Event ID " + id + " does not exist"); // FIXME we should do optional or something?
        }

        return ev;
    }

    @Override
    public ISignedEventStream getBackwardStreamFrom(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("stream index must be greater or equal to 0");
        }

        return new ISignedEventStream() {

            private int index = id;

            @Override
            public int getIndex() {
                return index;
            }

            @Override
            public List<ISignedEventStreamEntry> getNext(int amount) {
                if (amount <= 0) {
                    throw new IllegalArgumentException("amount must be greater than 0");
                }

                // TODO Streams could help if we provide a supplier with the values we want?
                List<ISignedEventStreamEntry> events = new ArrayList<>();
                int destination = Math.max(0, index - amount);
                for (int i = index; i >= destination && i >= 0; i--) {
                    events.add(EventManager.this.eventsStream.get(i)); // FIXME might change under concurrent access
                }
                index = destination;
                return events;
            }
        };
    }

    @Override
    public int getStreamIndex() {
        return eventsStream.size();
    }

    @Override
    public void addListener(Object o) {
        eventBus.subscribe(o);
    }

}
