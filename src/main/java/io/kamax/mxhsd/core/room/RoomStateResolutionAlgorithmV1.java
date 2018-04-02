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

package io.kamax.mxhsd.core.room;

import io.kamax.matrix.codec.MxBase64;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.IRoomStateResolutionAlgorithm;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.HomeserverState;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RoomStateResolutionAlgorithmV1 implements IRoomStateResolutionAlgorithm {

    private final HomeserverState global; // FIXME this shouldn't be necessary
    private final String roomId; // FIXME this shouldn't be necessary
    private final Function<String, ISignedEvent> fetcher;

    private MessageDigest md;

    public RoomStateResolutionAlgorithmV1(HomeserverState global, String roomId, Function<String, ISignedEvent> fetcher) {
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        this.global = global; // FIXME this shouldn't be necessary
        this.roomId = roomId; // FIXME this shouldn't be necessary
        this.fetcher = fetcher;
    }

    public IRoomState resolveAuth(Collection<IRoomState> states, String keyFull, IRoomState prevState) {
        // We collect all the events to resolve
        List<ISignedEvent> toResolve = states.stream()
                .map(state -> state.findEventFor(keyFull))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(fetcher)
                .collect(Collectors.toList());

        // We order them: depth ASC, SHA1(Ev ID) DESC
        toResolve.sort(Comparator.comparingLong(IEvent::getDepth).thenComparing((o1, o2) -> StringUtils.compare(
                MxBase64.encode(md.digest(o2.getId().getBytes(StandardCharsets.UTF_8))),
                MxBase64.encode(md.digest(o1.getId().getBytes(StandardCharsets.UTF_8)))
        )));

        // We add the first event to the state
        RoomState.Builder newStateBuilder = new RoomState.Builder(global, roomId).from(prevState);
        newStateBuilder.addEvent(toResolve.remove(0));
        IRoomState newState = newStateBuilder.build();

        // For the other events, we add one by one. If allowed, we continue. If not, we end.
        for (ISignedEvent event : toResolve) {
            RoomEventAuthorization auth = newState.isAuthorized(event);
            if (!auth.isAuthorized()) {
                break;
            }

            newState = auth.getNewState();
        }

        return newState;
    }

    // https://github.com/matrix-org/matrix-doc/blob/3dfa643b8b0353b76bc09a7beee69f94fdae60df/specification/server_server_api.rst#state-resolution-algorithm
    @Override
    public IRoomState resolve(Collection<IRoomState> states) {
        if (states.isEmpty()) {
            return new RoomState.Builder(global, roomId).build();
        }

        if (states.size() == 1) {
            return states.iterator().next();
        }

        /*
         We need collect all conflicting events between states which is defined as
         "different event_ids for the same (state_type, state_key)".

         To do so, we use a map of (state_type, state_key) as key and a Set of corresponding event IDs
         so we can directly see which key has more than one distinct event ID and is therefore conflicting.
          */

        // We collect distinct event IDs per state type+key
        Map<String, Set<String>> stateMix = new HashMap<>();
        states.forEach(state -> {
            state.getEvents().forEach((stateKey, eventId) -> {
                stateMix.computeIfAbsent(stateKey, key -> new HashSet<>()).add(eventId);
            });
        });

        // We add the non-conflicting state into the builder and collect the conflicting ones for further processing.
        RoomState.Builder builder = new RoomState.Builder(global, roomId);
        Set<String> conflicts = new HashSet<>();
        stateMix.forEach((key, value) -> {
            if (value.size() > 1) {
                // this is a conflict
                conflicts.add(key);
            } else {
                String eventId = value
                        .stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("You found a bug! Please report this!"));
                builder.addEvent(fetcher.apply(eventId));
            }
        });

        /*
        We now need to process every conflicting event following the specification from the initial non-conflicting state
        and apply the usual authorization rules for each, after sorting them in a specific order.
         */

        // Non-conflicting initial state
        IRoomState initState = builder.build();

        // We resolve conflicts between m.room.power_levels events, if any.
        IRoomState plState = conflicts.stream()
                .filter(RoomEventType.PowerLevels::is)
                .findAny() // FIXME there can only be one for now, but we need better IRoomState structure
                .map(keyFull -> {
                    conflicts.remove(keyFull);
                    return resolveAuth(states, keyFull, initState);
                })
                .orElse(initState);

        // We resolve conflicts between m.room.join_rules events, if any.
        IRoomState jrState = conflicts.stream()
                .filter(RoomEventType.JoinRules::is)
                .findAny() // FIXME there can only be one for now, but we need better IRoomState structure
                .map(keyFull -> {
                    conflicts.remove(keyFull);
                    return resolveAuth(states, keyFull, plState);
                })
                .orElse(plState);

        // We resolve membership events conflicts, if any.
        IRoomState memberState = jrState;

        // We collect each specific membership conflict.
        Set<String> mc = conflicts.stream()
                .filter(key -> StringUtils.startsWith(key, RoomEventType.Membership.get()))
                .collect(Collectors.toSet());

        // We resolve them.
        for (String keyFull : mc) {
            memberState = resolveAuth(states, keyFull, memberState);
            conflicts.remove(keyFull);
        }

        // We process all events that are not related to authorization.
        IRoomState authState = memberState;
        for (String keyFull : conflicts) {
            List<ISignedEvent> toResolve = states.stream()
                    .map(state -> state.findEventFor(keyFull))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(fetcher)
                    .sorted(Comparator.comparingLong(IEvent::getDepth).reversed().thenComparing((o1, o2) -> StringUtils.compare(
                            MxBase64.encode(md.digest(o1.getId().getBytes(StandardCharsets.UTF_8))),
                            MxBase64.encode(md.digest(o2.getId().getBytes(StandardCharsets.UTF_8)))
                    ))).collect(Collectors.toList());

            for (ISignedEvent event : toResolve) {
                RoomEventAuthorization auth = memberState.isAuthorized(event);
                if (auth.isAuthorized()) {
                    authState = auth.getNewState();
                    break;
                }
            }
        }

        return authState;
    }

}
