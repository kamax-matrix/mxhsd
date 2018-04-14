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
import com.google.gson.JsonElement;
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
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EventManager implements IEventManager {

    private transient final Logger log = LoggerFactory.getLogger(EventManager.class);

    private final List<String> essentialTopKeys;
    private final Map<String, List<String>> essentialContentKeys = new HashMap<>();

    private HomeserverState hsState;
    private Gson gson = GsonUtil.build();
    private MxSha256 sha256 = new MxSha256();

    private List<IProcessedEvent> eventsStream = Collections.synchronizedList(new ArrayList<>());
    private Map<String, IProcessedEvent> events = new ConcurrentHashMap<>();

    private MBassador<IEvent> eventBusFilter = new MBassador<>(new IPublicationErrorHandler.ConsoleLogger(true));
    private MBassador<IProcessedEvent> eventBusNotification = new MBassador<>(new IPublicationErrorHandler.ConsoleLogger(true));

    // FIXME enums
    public EventManager(HomeserverState hsState) {
        this.hsState = hsState;

        essentialTopKeys = Arrays.asList(
                EventKey.AuthEvents.get(),
                EventKey.Content.get(),
                EventKey.Depth.get(),
                EventKey.Id.get(),
                EventKey.Hashes.get(),
                EventKey.Membership.get(),
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
        essentialContentKeys.put(RoomEventType.JoinRules.get(), Collections.singletonList(EventContentKey.JoinRule));
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
    public IProtoEventBuilder populate(INakedEvent ev, String roomId, IRoomState withState, List<? extends IEvent> parents) {
        return new ProtoEventBuilder(ev)
                .setId(getNextId())
                .setRoomId(roomId)
                .setTimestamp(Instant.now())
                .setOrigin(hsState.getDomain())
                .addParents(parents);
    }

    @Override
    public IHashedProtoEvent hash(IProtoEvent ev) {
        return new Event(setHash(ev.getJson()));
    }

    @Override
    public IEvent sign(IHashedProtoEvent ev) {
        return null;
    }

    private JsonObject clone(JsonObject o) {
        return gson.fromJson(gson.toJson(o), JsonObject.class);
    }

    private JsonObject setHash(JsonObject base) { // TODO refactor into SDK
        base.remove(EventKey.Hashes.get());
        base.remove(EventKey.Signatures.get());
        JsonElement unsigned = base.remove(EventKey.Unsigned.get());
        String canonical = MatrixJson.encodeCanonical(base);

        JsonObject hashes = new JsonObject();
        hashes.addProperty("sha256", sha256.hash(canonical)); // FIXME do not hardcode
        base.add(EventKey.Hashes.get(), hashes);
        base.add(EventKey.Unsigned.get(), unsigned);
        return base;
    }

    private JsonObject getSignature(JsonObject event) { // TODO refactor into SDK
        JsonObject toSign = clone(event); // TODO how to do better?

        new HashSet<>(toSign.keySet()).forEach(key -> {
            if (!essentialTopKeys.contains(key)) toSign.remove(key);
        });

        JsonObject content = EventKey.Content.getObj(toSign);
        List<String> essentials = essentialContentKeys.getOrDefault(EventKey.Type.getString(toSign), Collections.emptyList());
        new HashSet<>(content.keySet()).forEach(key -> {
            if (!essentials.contains(key)) content.remove(key);
        });
        toSign.add(EventKey.Content.get(), content);

        return hsState.getSignMgr().signMessageGson(MatrixJson.encodeCanonical(toSign));
    }

    private JsonObject sign(JsonObject event) {
        JsonObject signatures = getSignature(event);
        event.add(EventKey.Signatures.get(), signatures);
        return event;
    }

    public JsonObject hashAndSign(JsonObject ev) { // TODO refactor into SDK
        return sign(setHash(ev));
    }

    @Override
    public IEvent sign(IProtoEvent ev) {
        return new Event(hashAndSign(ev.getJson()));
    }

    @Override
    public IEvent finalize(JsonObject ev) {
        ev.addProperty(EventKey.Id.get(), getNextId());
        ev.addProperty(EventKey.Origin.get(), hsState.getDomain());
        ev.addProperty(EventKey.Timestamp.get(), System.currentTimeMillis());
        return new Event(hashAndSign(ev));
    }

    @Override
    public synchronized IProcessedEvent store(IEvent ev) { // FIXME use RWLock
        eventBusFilter.publish(ev);

        IProcessedEvent entry = new ProcessedEvent(getPosition(), ev);
        eventsStream.add(entry);
        events.put(ev.getId(), entry);
        log.info("Event {} was stored in position {}", ev.getId(), entry.getInternalId());

        eventBusNotification.publish(entry); // TODO we might want to do this async?

        return entry;
    }

    @Override
    public IProcessedEvent get(String id) {
        IProcessedEvent ev = events.get(id);
        if (ev == null) {
            throw new IllegalArgumentException("Event ID " + id + " does not exist"); // FIXME we should do optional or something?
        }

        return ev;
    }

    @Override
    public List<IProcessedEvent> get(Collection<String> ids) {
        return ids.stream()
                .map(this::get)
                .collect(Collectors.toList());
    }

    @Override
    public IProcessedEventStream getBackwardStreamFrom(String position) {
        int id = Integer.parseInt(position);

        if (id < 0) {
            throw new IllegalArgumentException("stream index must be greater or equal to 0");
        }

        if (id > eventsStream.size()) {
            throw new IllegalArgumentException("index cannot be greater than current index");
        }

        return new IProcessedEventStream() {

            private int index = id - 1;

            @Override
            public String getPosition() {
                return Long.toString(index);
            }

            @Override
            public boolean hasNext() {
                return index > 0;
            }

            @Override
            public IProcessedEvent getNext() {
                // TODO Streams could help if we provide a supplier with the values we want?
                List<IProcessedEvent> events = new ArrayList<>();
                int destination = Math.max(-1, index - 1);
                log.info("Seek - Index: {} | Amount: {} | Destination: {}", index, 1, destination);
                for (int i = index; i > destination; i--) {
                    events.add(EventManager.this.eventsStream.get(i)); // FIXME might change under concurrent access
                }
                index = destination;
                return events.get(0);
            }
        };
    }

    @Override
    public IProcessedEventStream getForwardStreamFrom(String position) {
        int id = Integer.parseInt(position);

        if (id < 0) {
            throw new IllegalArgumentException("stream index must be greater or equal to 0");
        }

        if (id > eventsStream.size()) {
            throw new IllegalArgumentException("index cannot be greater than current index");
        }

        return new IProcessedEventStream() {

            private int index = id;

            private int getMax() {
                return eventsStream.size() - 1;
            }

            @Override
            public String getPosition() {
                return Long.toString(index);
            }

            @Override
            public boolean hasNext() {
                return index < getMax();
            }

            @Override
            public IProcessedEvent getNext() {
                // TODO Streams could help if we provide a supplier with the values we want?
                List<IProcessedEvent> events = new ArrayList<>();
                int destination = Math.max(getMax(), index + 1);
                log.info("Seek - Index: {} | Amount: {} | Destination: {}", index, 1, destination);
                for (int i = index; i <= destination; i++) {
                    events.add(EventManager.this.eventsStream.get(i)); // FIXME might change under concurrent access
                }
                index = destination;
                return events.get(0);
            }
        };
    }

    @Override
    public String getPosition() {
        return Integer.toString(eventsStream.size());
    }

    @Override
    public boolean isBefore(String toCheck, String reference) {
        return Long.compare(Long.parseLong(toCheck), Long.parseLong(reference)) < 0;
    }

    @Override
    public void addFilter(Object o) {
        eventBusFilter.subscribe(o);
    }

    @Override
    public void addListener(Object o) {
        eventBusNotification.subscribe(o);
    }

}
