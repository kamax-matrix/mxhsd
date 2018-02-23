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
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.room.*;
import io.kamax.mxhsd.api.room.event.IMembershipContext;
import io.kamax.mxhsd.api.room.event.RoomJoinRulesEvent;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.event.GetAuthChainTask;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

import static io.kamax.matrix.hs.RoomMembership.*;

public class RoomState implements IRoomState {

    public static class MembershipContext implements IMembershipContext {

        private String eventId;
        private String target;
        private String membership;

        private MembershipContext(IMembershipContext c) {
            this(c.getEventId(), c.getStateKey(), c.getMembership());
        }

        private MembershipContext(String eventId, String target, String membership) {
            this.eventId = eventId;
            this.target = target;
            this.membership = membership;
        }

        @Override
        public String getEventId() {
            return eventId;
        }

        @Override
        public String getStateKey() {
            return target;
        }

        @Override
        public String getMembership() {
            return membership;
        }

    }

    public static class Builder {

        private final Logger logger = LoggerFactory.getLogger(Builder.class);

        private RoomState r;

        public Builder(HomeserverState globalState, String roomId) {
            r = new RoomState();
            r.global = globalState;
            r.roomId = roomId;
        }

        public RoomState build() {
            return r;
        }

        public Builder from(IRoomState state) {
            if (state instanceof RoomState) { // we simply copy the cache
                RoomState other = (RoomState) state;
                setEvents(other.events);
                r.evId = other.evId;
                r.members = new HashMap<>(other.members);
                r.pls = other.pls;
                r.plsId = other.plsId;
            } else {
                state.getEvents().values().stream() // we need to rebuild the cache ourselves
                        .map(id -> r.global.getEvMgr().get(id).get()) // we fetch each event
                        .forEach(this::addEvent); // we process them one by one
            }

            return this;
        }

        public Builder from(Map<String, String> events) {
            setEvents(events);

            return this;
        }

        private void setEvents(Map<String, String> events) {
            r.events = new HashMap<>(events);
        }

        public boolean addEvent(IEvent ev) {
            if (!StringUtils.equals(r.roomId, ev.getRoomId())) {
                logger.warn("Got event {} with an unexpected room ID {} instead of {}", ev.getId(), ev.getRoomId(), r.roomId);
                return false;
            }

            JsonObject json = ev.getJson();
            if (!json.has(EventKey.StateKey.get())) {
                // Ignoring non-state event
                return false;
            }

            String sKey = GsonUtil.getString(json, EventKey.StateKey.get());
            r.events.put(ev.getType() + sKey, ev.getId());

            if (RoomEventType.Membership.is(ev.getType())) {
                RoomMembershipEvent mEv = new RoomMembershipEvent(json);
                setMember(new MembershipContext(ev.getId(), sKey, mEv.getMembership()));
            }

            if (RoomEventType.PowerLevels.is(ev.getType())) {
                RoomPowerLevels pl = new RoomPowerLevels(ev);
                r.plsId = ev.getId();
                r.pls = pl;
            }

            return true;
        }

        private void setMember(MembershipContext ev) {
            if (RoomMembership.Leave.is(ev.getMembership())) {
                r.events.remove(RoomEventType.Membership.get() + ev.getStateKey());
                r.members.remove(ev.getStateKey());
            } else {
                r.members.put(ev.getStateKey(), ev);
            }
        }

        public Builder withStreamIndex(String evId) {
            r.evId = evId;
            return this;
        }

    }

    private transient HomeserverState global; // FIXME this shouldn't be necessary

    private String roomId; // FIXME this shouldn't be necessary
    private String evId; // FIXME this does not make sense. A state is not bound to an event
    private Map<String, String> events = new HashMap<>(); // TODO we should keep IEventReference to speed up
    private Map<String, IMembershipContext> members = new HashMap<>();
    private RoomPowerLevels pls;
    private String plsId;

    private ISignedEvent getEv(String id) {
        return global.getEvMgr().get(id).get();
    }

    @Override
    public ISignedEvent getCreation() {
        return getEv(events.get(RoomEventType.Creation.get()));
    }

    @Override
    public Set<IMembershipContext> getMemberships() {
        return new HashSet<>(members.values());
    }

    @Override
    public Optional<IMembershipContext> getMembership(String target) {
        return Optional.ofNullable(members.get(target));
    }

    @Override
    public boolean hasPowerLevels() {
        return pls != null;
    }

    @Override
    public Optional<RoomPowerLevels> getPowerLevels() {
        return Optional.ofNullable(pls);
    }

    @Override
    public RoomPowerLevels getEffectivePowerLevels() {
        return getPowerLevels().orElse(RoomPowerLevels.Builder.initial());
    }

    @Override
    public String getPowerLevelsEventId() {
        return plsId;
    }

    @Override
    public String getEventId() {
        return evId;
    }

    @Override
    public int getStreamIndex() {
        return global.getEvMgr().get(getEventId()).streamIndex();
    }

    @Override
    public boolean isAccessibleAs(String user) {
        return true;
    }

    @Override
    public Optional<String> findEventFor(String combineKey) {
        return Optional.ofNullable(events.get(combineKey));
    }

    @Override
    public Optional<String> findEventFor(String type, String key) {
        return Optional.ofNullable(events.get(type + key));
    }

    private ISignedEvent getEventFor(String type, String key) {
        return findEventFor(type, key)
                .map(id -> global.getEvMgr().get(id).get())
                .orElseThrow(() -> new RuntimeException("Cannot find internal event for state key " +
                        type + key + " in room " + roomId));
    }

    @Override
    public Map<String, String> getEvents() {
        return Collections.unmodifiableMap(events);
    }

    @Override
    public IRoomStateSnapshot getSnapshot() {
        List<String> state = new ArrayList<>(getEvents().values());
        Set<String> authChain = ForkJoinPool.commonPool().invoke(new GetAuthChainTask(state, s -> global.getEvMgr().get(s).get()));
        return new RoomStateSnapshot(state, authChain);
    }

    private JsonObject getEventJson(String id) {
        return global.getEvMgr().get(id).get().getJson();
    }

    private String getMembershipOrDefault(String member) {
        return getMembership(member).map(IMembershipContext::getMembership).orElseGet(RoomMembership.Leave::get);
    }

    private boolean hasMembership(String member, RoomMembership... validValues) {
        return isMembership(getMembershipOrDefault(member), validValues);
    }

    private boolean isMembership(String toCheck, RoomMembership... validValues) {
        if (StringUtils.isNotBlank(toCheck)) {
            for (RoomMembership m : validValues) {
                if (m.is(toCheck)) {
                    return true;
                }
            }
        }

        return false;
    }

    // https://matrix.org/speculator/spec/HEAD/server_server/unstable.html#rules
    public synchronized RoomEventAuthorization isAuthorized(IEvent ev) { // FIXME use a better locking mechanism
        JsonObject evJson = ev.getJson();
        JsonObject content = EventKey.Content.getObj(evJson);
        String type = evJson.get(EventKey.Type.get()).getAsString();

        RoomEventAuthorization.Builder auth = new RoomEventAuthorization.Builder(roomId, ev);
        Builder stateBuilder = new Builder(global, roomId).from(this); // we'll only make an update to our current state
        stateBuilder.addEvent(ev);

        if (RoomEventType.Creation.is(type)) {
            if (!ev.getParents().isEmpty()) {
                return auth.deny(ev, "there is a previous event");
            }

            return auth.allow(stateBuilder);
        }
        auth.basedOn(getCreation());

        RoomPowerLevels effectivePls = getEffectivePowerLevels();
        String sender = EventKey.Sender.getString(evJson);
        String senderMs = getMembershipValue(sender).orElseGet(RoomMembership.Leave::get);
        long senderPl = getEffectivePowerLevels().getForUser(sender);

        String target = EventKey.StateKey.getString(evJson);
        long targetPl = getEffectivePowerLevels().getForUser(target);

        if (RoomEventType.Membership.is(type)) {
            String membership = content.get(RoomEventKey.Membership.get()).getAsString();
            if (Join.is(membership)) {
                IEvent firstParentEv = global.getEvMgr().get(ev.getParents().get(0).getEventId()).get();
                if (RoomEventType.Creation.is(firstParentEv.getType()) && ev.getParents().size() == 1) {
                    String creator = EventKey.Content.getObj(firstParentEv.getJson()).get("creator").getAsString();
                    if (!StringUtils.equals(EventKey.StateKey.getString(evJson), creator)) {
                        return auth.deny(ev, "sender is not creator");
                    }

                    return auth.allow(stateBuilder);
                }

                if (!StringUtils.equals(sender, target)) {
                    return auth.deny(ev, "sender is not the state context");
                }

                if (isMembership(senderMs, Join)) {
                    auth.basedOn(getEv(members.get(sender).getEventId()));
                    return auth.allow(this);
                }

                if (isMembership(senderMs, Invite)) {
                    auth.basedOn(getEv(members.get(sender).getEventId()));
                    return auth.allow(stateBuilder);
                }

                String rule = findEventFor(RoomEventType.JoinRules.get(), "")
                        .map(id -> RoomJoinRulesEvent.get(getEventJson(id)).getRule())
                        .orElse(RoomJoinRule.Private);
                if (!StringUtils.equals(rule, RoomJoinRule.Public)) {
                    return auth.deny(ev, "room is not public and sender was never invited");
                }
                auth.basedOn(getEv(findEventFor(RoomEventType.JoinRules.get(), "").orElse("")));

                return auth.allow(stateBuilder);
            } else if (Invite.is(membership)) {
                if (!hasMembership(sender, Join)) {
                    return auth.deny(ev, "sender cannot invite without being in the room");
                }
                auth.basedOn(getEv(members.get(sender).getEventId()));

                if (isMembership(getMembershipOrDefault(target), Ban, Join)) {
                    return auth.deny(ev, "invitee is already in the room or is banned from the room");
                }

                if (!effectivePls.canInvite(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum invite PL");
                }
                auth.basedOn(getEventFor(RoomEventType.PowerLevels.get(), ""));

                return auth.allow(stateBuilder);
            } else if (Leave.is(membership)) {
                boolean isSame = StringUtils.equals(sender, target);
                if (isSame && hasMembership(sender, Invite, Join)) {
                    auth.basedOn(getEv(members.get(target).getEventId()));
                    return auth.allow(stateBuilder);
                }

                if (!hasMembership(sender, Join)) {
                    return auth.deny(ev, "sender cannot send leave in a room they are not in");
                }

                if (isMembership(getMembershipOrDefault(target), Ban) && !effectivePls.canBan(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum ban PL");
                }

                if (!effectivePls.canKick(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum kick PL");
                }

                if (senderPl <= targetPl) {
                    return auth.deny(ev, "sender PL is not higher than target PL");
                }

                auth.basedOn(getEventFor(RoomEventType.PowerLevels.get(), ""));
                return auth.allow(stateBuilder);
            } else if (Ban.is(membership)) {
                if (!isMembership(senderMs, Join)) {
                    return auth.deny(ev, "sender cannot ban in a room they are not in");
                }
                auth.basedOn(getEv(members.get(sender).getEventId()));

                if (!effectivePls.canBan(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum ban PL");
                }

                if (senderPl <= targetPl) {
                    return auth.deny(ev, "sender PL is not higher than target PL");
                }
                auth.basedOn(getEventFor(RoomEventType.PowerLevels.get(), ""));

                return auth.allow(stateBuilder);
            } else {
                return auth.deny(ev, "unknown membership: " + membership);
            }
        }

        if (!isMembership(senderMs, Join)) {
            return auth.deny(ev, "sender " + sender + " is not in the room");
        }
        auth.basedOn(getEv(members.get(sender).getEventId()));

        // State events definition: https://matrix.org/speculator/spec/HEAD/client_server/unstable.html#state-event-fields
        // FIXME must clarify spec that state_key is mandatory on state events, and only there
        // FIXME yes, "state_key" turns any event into a state event, update Matrix.org spec to be clear
        if (!getEffectivePowerLevels().canForEvent(evJson.has(EventKey.StateKey.get()), type, senderPl)) {
            return auth.deny(ev, "sender does not have minimum PL for event type " + type);
        }

        if (RoomEventType.PowerLevels.is(ev.getType())) {
            RoomPowerLevels newPls = new RoomPowerLevels(ev);
            if (effectivePls.isInitialState()) { // Because https://github.com/matrix-org/synapse/issues/2644
                return auth.allow(stateBuilder);
            }

            if (!effectivePls.canReplace(sender, senderPl, newPls)) {
                return auth.deny(ev, "sender is missing minimum PL to change room PLs");
            }
            auth.basedOn(getEventFor(RoomEventType.PowerLevels.get(), ""));

            return auth.allow(stateBuilder);
        }

        if (RoomEventType.Redaction.is(ev.getType())) {
            if (pls.canRedact(senderPl)) {
                auth.basedOn(getEventFor(RoomEventType.PowerLevels.get(), ""));
                return auth.allow(stateBuilder);
            }

            if (StringUtils.equals(sender, target)) {
                auth.basedOn(getEventFor(RoomEventType.PowerLevels.get(), ""));
                return auth.allow(stateBuilder);
            }

            return auth.deny(ev, "sender does not have minimum redact PL");
        }

        auth.basedOn(getEventFor(RoomEventType.PowerLevels.get(), ""));
        return auth.allow(stateBuilder);
    }

}
