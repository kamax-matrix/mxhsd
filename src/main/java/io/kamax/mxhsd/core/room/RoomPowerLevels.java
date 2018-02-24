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

import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.exception.MalformedEventException;
import io.kamax.mxhsd.api.room.PowerLevel;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.api.room.event.PowerLevelKey;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoomPowerLevels {

    public static class Builder {

        private RoomPowerLevels levels = new RoomPowerLevels();

        public static RoomPowerLevels initial() {
            Builder b = new Builder();
            b.levels.isInitialState = true;
            return b.get();
        }

        public static Builder from(RoomPowerLevels orignal) {
            Builder b = new Builder();
            b.levels = new RoomPowerLevels(GsonUtil.get().toJsonTree(orignal).getAsJsonObject());
            b.levels.isInitialState = orignal.isInitialState;
            return b;
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

        public RoomPowerLevels get() {
            return levels;
        }

        public Builder defaults() {
            // Admins can do these
            addEvent(RoomEventType.HistoryVisibility.get(), PowerLevel.Admin);
            addEvent(RoomEventType.PowerLevels.get(), PowerLevel.Admin);

            // Moderators can do these
            setStateDefault(PowerLevel.Moderator);
            setBan(PowerLevel.Moderator);
            setKick(PowerLevel.Moderator);
            setRedact(PowerLevel.Moderator);

            // Anyone can do these
            setEventsDefault(PowerLevel.None);
            setInvite(PowerLevel.None);

            // Users don't get any PL by default
            setUsersDefault(PowerLevel.None);

            return this;
        }
    }

    public static Builder build() {
        return new Builder();
    }

    private transient boolean isInitialState = false;
    private Long ban;
    private Map<String, Long> events = new HashMap<>();
    private Long eventsDefault;
    private Long invite;
    private Long kick;
    private Long redact;
    private Long stateDefault;
    private Map<String, Long> users = new HashMap<>();
    private Long usersDefault;

    // https://matrix.org/speculator/spec/HEAD/client_server/unstable.html#m-room-power-levels
    // If there was no previous PL event, state default is 0
    // FIXME this is wrong, state and user defaults can differ depending on their presence. Look at it.
    private RoomPowerLevels() {
    }

    public RoomPowerLevels(JsonObject json) {
        init(json);
    }

    // FIXME this is wrong, state and user defaults can differ depending on their presence. Look at it.
    public RoomPowerLevels(IEvent ev) {
        if (!RoomEventType.PowerLevels.is(ev.getType())) {
            throw new IllegalArgumentException(ev.getId() + " is not a PL event type, but " + ev.getType());
        }

        init(ev.getId(), ev.getJson());
    }

    private void init(String eventId, JsonObject json) {
        try {
            EventKey.Content.findObj(json).ifPresent(this::init);
        } catch (IllegalStateException | NumberFormatException ex) {
            throw MalformedEventException.forId(eventId);
        }
    }

    private void init(JsonObject content) {
        GsonUtil.findLong(content, PowerLevelKey.Ban).ifPresent(v -> ban = v);
        GsonUtil.findLong(content, PowerLevelKey.EventsDefault).ifPresent(v -> eventsDefault = v);
        GsonUtil.findLong(content, PowerLevelKey.Invite).ifPresent(v -> invite = v);
        GsonUtil.findLong(content, PowerLevelKey.Kick).ifPresent(v -> kick = v);
        GsonUtil.findLong(content, PowerLevelKey.Redact).ifPresent(v -> redact = v);
        GsonUtil.findLong(content, PowerLevelKey.StateDefault).ifPresent(v -> stateDefault = v);
        GsonUtil.findLong(content, PowerLevelKey.UsersDefault).ifPresent(v -> usersDefault = v);

        GsonUtil.findObj(content, PowerLevelKey.Events).ifPresent(obj -> obj.entrySet().forEach(e -> {
            events.put(e.getKey(), e.getValue().getAsJsonPrimitive().getAsLong());
        }));

        GsonUtil.findObj(content, PowerLevelKey.Users).ifPresent(obj -> obj.entrySet().forEach(e -> {
            users.put(e.getKey(), e.getValue().getAsJsonPrimitive().getAsLong());
        }));

    }

    private boolean canReplace(long wihtPl, Map<String, Long> oldPls, Map<String, Long> newPls, long defaultPl) {
        return Stream.concat(oldPls.keySet().stream(), newPls.keySet()
                .stream()).collect(Collectors.toSet()).stream()
                .allMatch(type -> {
                    long oldPl = oldPls.getOrDefault(type, defaultPl);
                    long newPl = newPls.getOrDefault(type, defaultPl);
                    return wihtPl >= oldPl && wihtPl >= newPl;
                });
    }

    // OK to do as obj is immutable
    // matrix-doc@c7c08ea - https://matrix.org/speculator/spec/HEAD/server_server/unstable.html#rules @ 5.
    public boolean canReplace(String sender, long withPl, RoomPowerLevels newPls) {
        boolean basic = canSetTo(getUsersDefault(), newPls.getUsersDefault(), withPl) &&
                canSetTo(getEventsDefault(), newPls.getEventsDefault(), withPl) &&
                canSetTo(getStateDefault(), newPls.getStateDefault(), withPl);
        if (!basic) {
            return false;
        }


        boolean membership = canSetTo(getBan(), newPls.getBan(), withPl) &&
                canSetTo(getKick(), newPls.getKick(), withPl) &&
                canSetTo(getInvite(), newPls.getInvite(), withPl);
        if (!membership) {
            return false;
        }

        if (!canSetTo(getRedact(), newPls.getRedact(), withPl)) {
            return false;
        }

        long evPl = getEventsDefaultOrCompute();
        if (!canReplace(withPl, events, newPls.events, evPl)) {
            return false;
        }

        long userPl = getUsersDefaultOrCompute();
        if (!canReplace(withPl, users, newPls.users, userPl)) {
            return false;
        }

        return Stream.concat(users.keySet().stream(), newPls.users.keySet().stream()).collect(Collectors.toSet()).stream()
                .noneMatch(id -> {
                    if (StringUtils.equals(sender, id)) {
                        return false;
                    }

                    long oldPl = getForUser(id);
                    return oldPl == withPl;
                });
    }

    public boolean can(long withPl, Long requiredPl) {
        return requiredPl == null || withPl >= requiredPl;
    }

    public boolean canSetTo(Optional<Long> oldPl, Optional<Long> newPl, long withPl) {
        return oldPl.map(o -> canSetTo(o, newPl.orElse(Long.MIN_VALUE), withPl)).orElse(true);
    }

    public boolean canSetTo(long oldPl, long newPl, long withPl) {
        if (oldPl == newPl) {
            return true;
        }

        if (oldPl > withPl) {
            return false;
        }

        return withPl >= newPl;
    }

    public Optional<Long> getBan() {
        return Optional.ofNullable(ban);
    }

    public boolean canBan(long withPl) {
        return can(withPl, getBan().orElse(50L));
    }

    public Long getForEvent(boolean isState, String type) {
        return events.getOrDefault(type, isState ? getStateDefaultOrCompute() : getEventsDefaultOrCompute());
    }

    public boolean canForEvent(boolean isState, String type, long withPl) {
        return can(withPl, getForEvent(isState, type));
    }

    public Optional<Long> getEventsDefault() {
        return Optional.ofNullable(eventsDefault);
    }

    public long getEventsDefaultOrCompute() {
        if (isInitialState) {
            return 0;
        } else {
            return getEventsDefault().orElse(50L);
        }
    }

    public Optional<Long> getInvite() {
        return Optional.ofNullable(invite);
    }

    public boolean canInvite(long withPl) {
        return can(withPl, getInvite().orElse(50L));
    }

    public Optional<Long> getKick() {
        return Optional.ofNullable(kick);
    }

    public boolean canKick(long withPl) {
        return can(withPl, getKick().orElse(50L));
    }

    public Optional<Long> getRedact() {
        return Optional.ofNullable(redact);
    }

    public boolean canRedact(long withPl) {
        return can(withPl, getRedact().orElse(50L));
    }

    public Optional<Long> getStateDefault() {
        return Optional.ofNullable(stateDefault);
    }

    public long getStateDefaultOrCompute() {
        if (isInitialState) {
            return 0;
        } else {
            return getStateDefault().orElse(50L);
        }
    }

    public long getForUser(String id) {
        return users.getOrDefault(id, getUsersDefaultOrCompute());
    }

    public Optional<Long> getUsersDefault() {
        return Optional.ofNullable(usersDefault);
    }

    public long getUsersDefaultOrCompute() {
        return getUsersDefault().orElse(0L);
    }

    public boolean isInitialState() {
        return isInitialState;
    }

}
