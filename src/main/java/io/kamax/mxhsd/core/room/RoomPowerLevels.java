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

import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.exception.MalformedEventException;
import io.kamax.mxhsd.core.JsonUtil;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoomPowerLevels {

    private long ban;
    private Map<String, Long> events;
    private long eventsDefault;
    private long invite;
    private long kick;
    private long redact;
    private long stateDefault;
    private Map<String, Long> users;
    private long usersDefault;

    public RoomPowerLevels(IEvent ev) {
        ban = 50;
        events = new HashMap<>();
        eventsDefault = 0;
        invite = 50;
        kick = 50;
        redact = 50;
        stateDefault = 0;
        users = new HashMap<>();
        usersDefault = 0;

        EventKey.Content.findObj(ev.getJson()).ifPresent(content -> {
            ban = JsonUtil.getLong(content, RoomMembership.Ban.get(), ban);
            eventsDefault = JsonUtil.getLong(content, "events_default", eventsDefault);  // FIXME turn into enum
            invite = JsonUtil.getLong(content, RoomMembership.Invite.get(), invite);
            kick = JsonUtil.getLong(content, "kick", kick); // FIXME turn into enum
            redact = JsonUtil.getLong(content, "redact", redact); // FIXME turn into enum
            stateDefault = JsonUtil.getLong(content, "state_default", stateDefault); // FIXME turn into enum
            redact = JsonUtil.getLong(content, "redact", redact); // FIXME turn into enum
            usersDefault = JsonUtil.getLong(content, "users_default", usersDefault); // FIXME turn into enum

            JsonUtil.findObj(content, "events").ifPresent(obj -> obj.entrySet().forEach(e -> {
                try {
                    events.put(e.getKey(), e.getValue().getAsJsonPrimitive().getAsLong());
                } catch (IllegalStateException | NumberFormatException ex) {
                    throw MalformedEventException.forId(ev.getId());
                }
            }));

            JsonUtil.findObj(content, "users").ifPresent(obj -> obj.entrySet().forEach(e -> {
                try {
                    users.put(e.getKey(), e.getValue().getAsJsonPrimitive().getAsLong());
                } catch (IllegalStateException | NumberFormatException ex) {
                    throw MalformedEventException.forId(ev.getId());
                }
            }));
        });
    }

    private boolean canReplace(long wihtPl, Map<String, Long> oldPls, Map<String, Long> newPls) {
        return Stream.concat(oldPls.keySet().stream(), newPls.keySet()
                .stream()).collect(Collectors.toSet()).stream()
                .noneMatch(type -> {
                    long oldPl = oldPls.getOrDefault(type, Long.MIN_VALUE);
                    long newPl = newPls.getOrDefault(type, Long.MIN_VALUE);
                    return oldPl != newPl && (oldPl > wihtPl || newPl > wihtPl);
                });
    }

    // OK to do as obj is immutable
    // matrix-doc@c7c08ea - https://matrix.org/speculator/spec/HEAD/server_server/unstable.html#rules @ 5.
    public boolean canReplace(String sender, long withPl, RoomPowerLevels newPls) {
        boolean basic = can(withPl, eventsDefault) && can(withPl, stateDefault) && can(withPl, usersDefault);
        if (!basic) {
            return false;
        }

        boolean member = canBan(withPl) && canKick(withPl) && canInvite(withPl);
        if (!member) {
            return false;
        }

        if (!can(withPl, getRedact())) {
            return false;
        }

        if (!canReplace(withPl, events, newPls.events)) {
            return false;
        }

        if (!canReplace(withPl, users, newPls.users)) {
            return false;
        }

        return Stream.concat(events.keySet().stream(), newPls.events.keySet().stream()).collect(Collectors.toSet()).stream()
                .noneMatch(id -> {
                    if (StringUtils.equals(sender, id)) {
                        return true;
                    }

                    long oldPl = events.getOrDefault(id, Long.MIN_VALUE);
                    long newPl = newPls.events.getOrDefault(id, Long.MIN_VALUE);
                    return oldPl != newPl && withPl == newPl;
                });
    }

    public boolean can(long withPl, long requiredPl) {
        return withPl >= requiredPl;
    }

    public long getBan() {
        return ban;
    }

    public boolean canBan(long withPl) {
        return can(withPl, getBan());
    }

    public long getForEvent(String type) {
        return events.getOrDefault(type, getEventsDefault());
    }

    public boolean canForEvent(String type, long withPl) {
        return can(withPl, getForEvent(type));
    }

    public long getEventsDefault() {
        return eventsDefault;
    }

    public long getInvite() {
        return invite;
    }

    public boolean canInvite(long withPl) {
        return can(withPl, getInvite());
    }

    public long getKick() {
        return kick;
    }

    public boolean canKick(long withPl) {
        return can(withPl, getKick());
    }

    public long getRedact() {
        return redact;
    }

    public boolean canRedact(long withPl) {
        return can(withPl, getRedact());
    }

    public long getStateDefault() {
        return stateDefault;
    }

    public long getForUser(String id) {
        return users.getOrDefault(id, getUsersDefault());
    }

    public long getUsersDefault() {
        return usersDefault;
    }

}
