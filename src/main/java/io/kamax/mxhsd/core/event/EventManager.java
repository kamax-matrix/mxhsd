/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Kamax Sarl
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
import io.kamax.mxhsd.api.exception.NotFoundException;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.GlobalStateHolder;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class EventManager implements IEventManager {

    private transient final Logger log = LoggerFactory.getLogger(EventManager.class);

    private final List<String> essentialTopKeys;
    private final Map<String, List<String>> essentialContentKeys = new HashMap<>();

    private GlobalStateHolder global;
    private Gson gson = GsonUtil.build();
    private MxSha256 sha256 = new MxSha256();

    private MBassador<IEvent> eventBusFilter = new MBassador<>(new IPublicationErrorHandler.ConsoleLogger(true));
    private MBassador<IProcessedEvent> eventBusNotification = new MBassador<>(new IPublicationErrorHandler.ConsoleLogger(true));

    // FIXME enums
    public EventManager(GlobalStateHolder hsState) {
        this.global = hsState;

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
        return "$" + local + ":" + global.getDomain();
    }

    @Override
    public IProtoEventBuilder populate(INakedEvent ev, String roomId, IRoomState withState, List<? extends IEvent> parents) {
        return new ProtoEventBuilder(ev)
                .setId(getNextId())
                .setRoomId(roomId)
                .setTimestamp(Instant.now())
                .setOrigin(global.getDomain())
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

        return global.getSignMgr().signMessageGson(MatrixJson.encodeCanonical(toSign));
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
        ev.addProperty(EventKey.Origin.get(), global.getDomain());
        ev.addProperty(EventKey.Timestamp.get(), System.currentTimeMillis());
        return new Event(hashAndSign(ev));
    }

    @Override
    public synchronized IProcessedEvent store(IEvent ev) { // FIXME use RWLock
        eventBusFilter.publish(ev);

        IProcessedEvent entry = global.getStore().putEvent(ev);

        eventBusNotification.publish(entry); // TODO we might want to do this async?

        return entry;
    }

    @Override
    public IProcessedEvent get(String id) {
        return global.getStore().findEvent(id).orElseThrow(() ->
                new IllegalArgumentException("Event ID " + id + " does not exist"));
    }

    @Override
    public List<IProcessedEvent> get(Collection<String> ids) {
        return ids.stream()
                .map(this::get)
                .collect(Collectors.toList());
    }

    @Override
    public IProcessedEventStream getBackwardStreamFrom(String position) {
        long id = Long.parseLong(position);

        if (id < 0) {
            throw new IllegalArgumentException("Invalid stream ID: " + id);
        }

        if (id > global.getStore().getCurrentStreamId()) {
            throw new IllegalArgumentException("position is not wihtin valid stream stream IDs");
        }

        return new IProcessedEventStream() {

            private long index = id - 1;

            @Override
            public String getPosition() {
                return Long.toString(index);
            }

            @Override
            public boolean hasNext() {
                return index >= 0;
            }

            @Override
            public IProcessedEvent getNext() {
                // TODO Streams could help if we provide a supplier with the values we want?
                List<IProcessedEvent> events = new ArrayList<>();
                while (hasNext()) {
                    long destination = Math.max(-1, index - 1);
                    for (long i = index; i > destination; i--) {
                        try {
                            events.add(global.getStore().getEventAtStreamId(i)); // FIXME might change under concurrent access
                        } catch (NotFoundException e) {
                            log.debug("Stream ID {} does not exist", i);
                        }
                    }
                    index = destination;
                    if (!events.isEmpty()) {
                        return events.get(0);
                    }
                }

                throw new IllegalStateException();
            }
        };
    }

    @Override
    public IProcessedEventStream getForwardStreamFrom(String position) {
        long id = Long.parseLong(position);
        long max = global.getStore().getCurrentStreamId();

        if (id < 0) {
            throw new IllegalArgumentException("Invalid stream ID: " + id);
        }

        if (id > max) {
            throw new IllegalArgumentException("position is not wihtin valid stream stream IDs");
        }

        return new IProcessedEventStream() {

            private long index = id - 1;

            @Override
            public String getPosition() {
                return Long.toString(index);
            }

            @Override
            public boolean hasNext() {
                return index <= max;
            }

            @Override
            public IProcessedEvent getNext() {
                // TODO Streams could help if we provide a supplier with the values we want?
                List<IProcessedEvent> events = new ArrayList<>();
                while (hasNext()) {
                    long destination = Math.max(max + 1, index + 1);
                    for (long i = index; i <= max; i++) {
                        try {
                            events.add(global.getStore().getEventAtStreamId(i)); // FIXME might change under concurrent access
                        } catch (NotFoundException e) {
                            log.debug("Stream ID {} does not exist", i);
                        }
                    }
                    index = destination;
                    if (!events.isEmpty()) {
                        return events.get(0);
                    }
                }

                throw new IllegalStateException();
            }
        };
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
