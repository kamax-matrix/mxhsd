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
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.RoomEventKey;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.api.room.event.IMembershipContext;
import io.kamax.mxhsd.core.HomeserverState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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

        private RoomState r;

        public Builder(HomeserverState globalState, String roomId) {
            r = new RoomState();
            r.globalState = globalState;
            r.roomId = roomId;
        }

        public RoomState build() {
            return r;
        }

        public Builder from(IRoomState state) {
            return withCreation(state.getCreation())
                    .withPower(state.getPowerLevelsEventId(), state.getPowerLevels())
                    .setMembers(state.getMemberships())
                    .setExtremities(state.getExtremities());
        }

        public Builder withCreation(IEvent c) {
            r.creation = c;
            return this;
        }

        private Builder setMember(IMembershipContext ev) {
            r.membership.put(ev.getStateKey(), new MembershipContext(ev));
            return this;
        }

        public Builder setMembers(Collection<IMembershipContext> ms) {
            ms.forEach(this::setMember);
            return this;
        }

        public Builder setMember(IEvent ev, String target, String membership) {
            setMember(new MembershipContext(ev.getId(), target, membership));
            return this;
        }

        public Builder withPower(String evenId, RoomPowerLevels p) {
            r.pId = evenId;
            r.powerLevels = p;
            return this;
        }

        public Builder withPower(RoomPowerLevels p) {
            return withPower(null, p);
        }

        public Builder setExtremities(ISignedEvent... evs) {
            return setExtremities(Arrays.asList(evs));
        }

        public Builder setExtremities(Collection<ISignedEvent> evs) {
            r.extremities = new ArrayList<>(evs);
            return this;
        }

    }

    private transient Logger log = LoggerFactory.getLogger(RoomState.class);
    private transient HomeserverState globalState;

    private String roomId;
    private IEvent creation;
    private Map<String, MembershipContext> membership = new HashMap<>();
    private RoomPowerLevels powerLevels;
    private String pId;
    private List<ISignedEvent> extremities = new ArrayList<>();

    @Override
    public IEvent getCreation() {
        return creation;
    }

    @Override
    public Set<IMembershipContext> getMemberships() {
        return new HashSet<>(membership.values());
    }

    @Override
    public Optional<IMembershipContext> getMembership(String target) {
        return Optional.ofNullable(membership.get(target));
    }

    @Override
    public RoomPowerLevels getPowerLevels() {
        return powerLevels;
    }

    @Override
    public String getPowerLevelsEventId() {
        return pId;
    }

    @Override
    public Set<ISignedEvent> getExtremities() {
        return new HashSet<>(extremities);
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
        long depth = evJson.get(EventKey.Depth.get()).getAsLong();

        RoomEventAuthorization.Builder auth = new RoomEventAuthorization.Builder(roomId, ev);
        Builder stateBuilder = new Builder(globalState, roomId).from(this); // we'll only make an update to our current state
        if (RoomEventType.Creation.is(type)) {
            if (depth != 1) { // FIXME rules talk about depth 0, but synapse (and dendrite?) set 1
                return auth.deny(ev, "depth is not 1");
            }

            if (!extremities.isEmpty()) {
                return auth.deny(ev, "there is a previous event");
            }

            return auth.allow(stateBuilder.withCreation(ev));
        }

        String sender = EventKey.Sender.getString(evJson);
        String senderMs = getMembershipValue(sender).orElseGet(RoomMembership.Leave::get);
        long senderPl = powerLevels.getForUser(sender);

        String target = EventKey.StateKey.getString(evJson);
        long targetPl = powerLevels.getForUser(target);

        if (RoomEventType.Membership.is(type)) {
            String membership = content.get(RoomEventKey.Membership.get()).getAsString();
            stateBuilder.setMember(ev, target, membership);
            if (Join.is(membership)) {
                IEvent firstParentEv = extremities.get(0);
                if (RoomEventType.Creation.is(firstParentEv.getType()) && extremities.size() == 1) {
                    if (depth != 2) { // FIXME rules talk about depth 1, but synapse (and dendrite?) set 2
                        return auth.deny(ev, "depth is not 2");
                    }

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
                    return auth.allow(this);
                }

                if (isMembership(senderMs, Invite)) {
                    return auth.allow(stateBuilder);
                }

                if (!StringUtils.equals(EventKey.Content.getObj(evJson).get("join_rule").getAsString(), "public")) {
                    return auth.deny(ev, "room is private and sender was never invited");
                }

                return auth.allow(stateBuilder);
            } else if (Invite.is(membership)) {
                if (!hasMembership(sender, Join)) {
                    return auth.deny(ev, "sender cannot invite without being in the room");
                }

                if (isMembership(getMembershipOrDefault(target), Ban, Join)) {
                    return auth.deny(ev, "invitee is already in the room or is banned from the room");
                }

                if (!powerLevels.canInvite(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum invite PL");
                }

                return auth.allow(stateBuilder);
            } else if (Leave.is(membership)) {
                boolean isSame = StringUtils.equals(sender, target);
                if (isSame && hasMembership(sender, Invite)) {
                    return auth.allow(stateBuilder);
                }

                if (isSame && !hasMembership(sender, Join)) {
                    return auth.deny(ev, "sender cannot leave a room they are not in");
                }

                if (isMembership(getMembershipOrDefault(target), Ban) && !powerLevels.canBan(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum ban PL");
                }

                if (!powerLevels.canKick(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum kick PL");
                }

                if (senderPl <= targetPl) {
                    return auth.deny(ev, "sender PL is not higher than target PL");
                }

                return auth.allow(stateBuilder);
            } else if (Ban.is(membership)) {
                if (!isMembership(senderMs, Join)) {
                    return auth.deny(ev, "sender cannot ban in a room they are not in");
                }

                if (!powerLevels.canBan(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum ban PL");
                }

                if (senderPl <= targetPl) {
                    return auth.deny(ev, "sender PL is not higher than target PL");
                }

                return auth.allow(stateBuilder);
            } else {
                return auth.deny(ev, "unknown membership: " + membership);
            }
        }

        if (!isMembership(senderMs, Join)) {
            return auth.deny(ev, "sender " + sender + " is not in the room");
        }

        // State events definition: https://matrix.org/speculator/spec/HEAD/client_server/unstable.html#state-event-fields
        // FIXME must clarify spec that state_key is mandatory on state events, and only there
        // FIXME yes, "state_key" turns any event into a state event, update Matrix.org spec to be clear
        if (!powerLevels.canForEvent(evJson.has(EventKey.StateKey.get()), type, senderPl)) {
            return auth.deny(ev, "sender does not have minimum PL for event type " + type);
        }

        if (RoomEventType.PowerLevels.is(ev.getType())) {
            RoomPowerLevels newPls = new RoomPowerLevels(ev);
            if (!powerLevels.canReplace(sender, senderPl, newPls)) {
                return auth.deny(ev, "sender is missing minimum PL to change room PLs");
            }

            return auth.allow(stateBuilder.withPower(ev.getId(), newPls));
        }

        if (RoomEventType.Redaction.is(ev.getType())) {
            if (powerLevels.canRedact(senderPl)) {
                return auth.allow(stateBuilder);
            }

            if (StringUtils.equals(sender, target)) {
                return auth.allow(stateBuilder);
            }

            return auth.deny(ev, "sender does not have minimum redact PL");
        }

        return auth.allow(stateBuilder);
    }

}
