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

package io.kamax.mxhsd.core.room;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.api.event.IHashedProtoEvent;
import io.kamax.mxhsd.api.event.StateTuple;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.IRoomStateSnapshot;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.api.room.event.IMembershipContext;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;

import java.util.*;

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

        private RoomState r = new RoomState();

        public Builder from(IRoomState state) {
            if (state instanceof RoomState) { // we simply copy the cache
                RoomState other = (RoomState) state;
                r.events = new HashMap<>(other.events);
                r.members = new HashMap<>(other.members);
                r.servers = new HashSet<>(other.servers);
                r.pls = other.pls;
                r.plsId = other.plsId;
            } else { // we need to rebuild the cache ourselves: we process them one by one
                from(state.getEvents());
            }

            return this;
        }

        public Builder from(Map<StateTuple, IHashedProtoEvent> events) {
            events.values().forEach(this::addEvent);

            return this;
        }

        public Builder from(IRoomStateSnapshot snapshot) {
            snapshot.getState().forEach(this::addEvent);

            return this;
        }

        public boolean addEvent(IHashedProtoEvent ev) {
            ev.getStateKey().ifPresent(stateKey -> {
                r.events.put(StateTuple.of(ev.getType(), stateKey), ev);

                // We compute some cached values

                if (RoomEventType.Membership.is(ev.getType())) {
                    RoomMembershipEvent mEv = new RoomMembershipEvent(ev.getJson());
                    setMember(new MembershipContext(ev.getId(), stateKey, mEv.getMembership()));
                    r.servers.add(MatrixID.asAcceptable(mEv.getStateKey()).getDomain());
                }

                if (RoomEventType.PowerLevels.is(ev.getType())) {
                    RoomPowerLevels pl = new RoomPowerLevels(ev);
                    r.plsId = ev.getId();
                    r.pls = pl;
                }
            });

            return ev.getStateKey().isPresent();
        }

        private void setMember(MembershipContext ev) {
            if (RoomMembership.Leave.is(ev.getMembership())) {
                r.events.remove(StateTuple.of(RoomEventType.Membership, ev.getStateKey()));
                r.members.remove(ev.getStateKey());
            } else {
                r.members.put(ev.getStateKey(), ev);
            }
        }

        public RoomState get() {
            return r;
        }

    }

    public static Builder build() {
        return new Builder();
    }

    public static RoomState from(IRoomState state) {
        return build().from(state).get();
    }

    public static RoomState empty() {
        return build().get();
    }

    private Map<StateTuple, IHashedProtoEvent> events = new HashMap<>();
    private Map<String, IMembershipContext> members = new HashMap<>();
    private Set<String> servers = new HashSet<>();
    private RoomPowerLevels pls;
    private String plsId;

    @Override
    public IHashedProtoEvent getCreation() {
        return getEventFor(StateTuple.of(RoomEventType.Creation));
    }

    @Override
    public Set<IMembershipContext> getMemberships() {
        return new HashSet<>(members.values());
    }

    @Override
    public Optional<IMembershipContext> findMembership(String target) {
        return Optional.ofNullable(members.get(target));
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
    public boolean isAccessibleAs(String user) {
        return true;
    }

    @Override
    public Set<String> getServers() {
        return new HashSet<>(servers);
    }

    @Override
    public Map<StateTuple, IHashedProtoEvent> getEvents() {
        return Collections.unmodifiableMap(events);
    }

    @Override
    public Optional<IHashedProtoEvent> findEventFor(StateTuple tuple) {
        return Optional.ofNullable(events.get(tuple));
    }

}
