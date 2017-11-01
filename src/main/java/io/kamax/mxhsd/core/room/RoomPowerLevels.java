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
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.exception.MalformedEventException;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoomPowerLevels {

    public static class Builder {

        private RoomPowerLevels levels;

        public Builder() {
            levels = new RoomPowerLevels();
        }

        public Builder setBan(long pl) {
            levels.ban = pl;
            return this;
        }

        public Builder setEventsDefault(long pl) {
            levels.eventsDefault = pl;
            return this;
        }

        public Builder setInvite(long pl) {
            levels.invite = pl;
            return this;
        }

        public Builder setKick(long pl) {
            levels.kick = pl;
            return this;
        }

        public Builder setRedact(long pl) {
            levels.redact = pl;
            return this;
        }

        public Builder setStateDefault(long pl) {
            levels.stateDefault = pl;
            return this;
        }

        public Builder setUsersDefault(long pl) {
            levels.usersDefault = pl;
            return this;
        }

        public Builder addEvent(String type, long pl) {
            levels.events.put(type, pl);
            return this;
        }

        public Builder addUser(String id, long pl) {
            levels.users.put(id, pl);
            return this;
        }

        public RoomPowerLevels build() {
            return levels;
        }
    }

    private long ban;
    private Map<String, Long> events;
    private long eventsDefault;
    private long invite;
    private long kick;
    private long redact;
    private long stateDefault;
    private Map<String, Long> users;
    private long usersDefault;

    // https://matrix.org/speculator/spec/HEAD/client_server/unstable.html#m-room-power-levels
    public RoomPowerLevels(long stateDefault) {
        this.ban = 50;
        this.events = new HashMap<>();
        this.eventsDefault = 0;
        this.invite = 50;
        this.kick = 50;
        this.redact = 50;
        this.stateDefault = stateDefault;
        this.users = new HashMap<>();
        this.usersDefault = 0;
    }

    // If there was no previous PL event, state default is 0
    // FIXME this is wrong, state and user defaults can differ depending on their presence. Look at it.
    public RoomPowerLevels() {
        this(0);
    }

    // FIXME this is wrong, state and user defaults can differ depending on their presence. Look at it.
    public RoomPowerLevels(IEvent ev) {
        this(50);
        EventKey.Content.findObj(ev.getJson()).ifPresent(content -> {
            ban = GsonUtil.getLong(content, RoomMembership.Ban.get(), ban);
            eventsDefault = GsonUtil.getLong(content, "events_default", eventsDefault);  // FIXME turn into enum
            invite = GsonUtil.getLong(content, RoomMembership.Invite.get(), invite);
            kick = GsonUtil.getLong(content, "kick", kick); // FIXME turn into enum
            redact = GsonUtil.getLong(content, "redact", redact); // FIXME turn into enum
            stateDefault = GsonUtil.getLong(content, "state_default", stateDefault); // FIXME turn into enum
            redact = GsonUtil.getLong(content, "redact", redact); // FIXME turn into enum
            usersDefault = GsonUtil.getLong(content, "users_default", usersDefault); // FIXME turn into enum

            GsonUtil.findObj(content, "events").ifPresent(obj -> obj.entrySet().forEach(e -> {
                try {
                    events.put(e.getKey(), e.getValue().getAsJsonPrimitive().getAsLong());
                } catch (IllegalStateException | NumberFormatException ex) {
                    throw MalformedEventException.forId(ev.getId());
                }
            }));

            GsonUtil.findObj(content, "users").ifPresent(obj -> obj.entrySet().forEach(e -> {
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

    public long getForEvent(boolean isState, String type) {
        return events.getOrDefault(type, isState ? getStateDefault() : getEventsDefault());
    }

    public boolean canForEvent(boolean isState, String type, long withPl) {
        return can(withPl, getForEvent(isState, type));
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
