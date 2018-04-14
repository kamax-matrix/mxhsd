/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxhsd.core.room.algo.v1;

import com.google.gson.JsonObject;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IHashedProtoEvent;
import io.kamax.mxhsd.api.event.StateTuple;
import io.kamax.mxhsd.api.room.*;
import io.kamax.mxhsd.api.room.event.RoomJoinRulesEvent;
import io.kamax.mxhsd.core.room.RoomEventAuthorization;
import io.kamax.mxhsd.core.room.RoomPowerLevels;
import io.kamax.mxhsd.core.room.RoomState;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

import static io.kamax.matrix.hs.RoomMembership.*;

public class RoomEventAuthorizationAlgorithm_v1 implements IRoomEventAuthorizationAlgorithm {

    private boolean isMembership(String toCheck, RoomMembership... validValues) {
        if (StringUtils.isBlank(toCheck)) {
            return false;
        }

        return Stream.of(validValues).anyMatch(m -> m.is(toCheck));
    }

    private String computeMembershipValue(IRoomState state, String member) {
        return state.findMembershipValue(member).orElseGet(RoomMembership.Leave::get);
    }

    private boolean hasMembership(IRoomState state, String member, RoomMembership... validValues) {
        return isMembership(computeMembershipValue(state, member), validValues);
    }

    @Override
    public RoomEventAuthorization authorize(IRoomState state, IHashedProtoEvent ev) {
        JsonObject evJson = ev.getJson();
        JsonObject content = EventKey.Content.getObj(evJson);
        String type = evJson.get(EventKey.Type.get()).getAsString();

        RoomEventAuthorization.Builder auth = new RoomEventAuthorization.Builder(ev);
        RoomState.Builder stateBuilder = new RoomState.Builder().from(state); // we'll only make an update to our current state
        stateBuilder.addEvent(ev);

        if (RoomEventType.Creation.is(type)) {
            return state.findEventFor(StateTuple.of(RoomEventType.Creation))
                    .map(evCreate -> auth.deny(ev, "Room creation event already exists: " + evCreate.getId()))
                    .orElseGet(() -> auth.allow(stateBuilder));
        }
        auth.basedOn(state.getCreation());

        RoomPowerLevels effectivePls = state.getEffectivePowerLevels();
        String sender = EventKey.Sender.getString(evJson);
        String senderMs = state.findMembershipValue(sender).orElseGet(RoomMembership.Leave::get);
        long senderPl = state.getEffectivePowerLevels().getForUser(sender);

        String target = EventKey.StateKey.getString(evJson);
        long targetPl = state.getEffectivePowerLevels().getForUser(target);

        if (RoomEventType.Membership.is(type)) {
            String membership = content.get(RoomEventKey.Membership.get()).getAsString();
            if (Join.is(membership)) {
                if (ev.getParents().size() == 1) {
                    String createId = state.getCreation().getId();
                    String createIdToCheck = ev.getParents().get(0).getEventId();

                    if (StringUtils.equals(createId, createIdToCheck)) {
                        String creator = EventKey.Content.getObj(state.getCreation().getJson()).get("creator").getAsString();
                        if (StringUtils.equals(EventKey.StateKey.getString(evJson), creator)) {
                            return auth.allow(stateBuilder);
                        }
                    }
                }

                if (!StringUtils.equals(sender, target)) {
                    return auth.deny(ev, "sender is not the state key");
                }

                if (isMembership(senderMs, Join)) {
                    auth.basedOn(state.getMembershipEvent(sender));
                    return auth.allow(stateBuilder);
                }

                if (isMembership(senderMs, Invite)) {
                    auth.basedOn(state.getMembershipEvent(sender));
                    return auth.allow(stateBuilder);
                }

                String rule = state.findEventFor(StateTuple.of(RoomEventType.JoinRules))
                        .map(jrEv -> RoomJoinRulesEvent.get(jrEv.getJson()).getRule())
                        .orElse(RoomJoinRule.Private);
                if (!StringUtils.equals(rule, RoomJoinRule.Public)) {
                    return auth.deny(ev, "room is not public and sender was never invited");
                }
                auth.basedOn(state.getEventFor(StateTuple.of(RoomEventType.JoinRules)));

                return auth.deny(ev, "Not allowed to join");
            } else if (Invite.is(membership)) {
                if (!hasMembership(state, sender, Join)) {
                    return auth.deny(ev, "sender cannot invite without being in the room");
                }
                auth.basedOn(state.getMembershipEvent(sender));

                if (isMembership(computeMembershipValue(state, target), Ban, Join)) {
                    return auth.deny(ev, "invitee is already in the room or is banned from the room");
                }

                if (!effectivePls.canInvite(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum invite PL");
                }
                auth.basedOn(state.getEventFor(StateTuple.of(RoomEventType.PowerLevels)));

                return auth.allow(stateBuilder);
            } else if (Leave.is(membership)) {
                boolean isSame = StringUtils.equals(sender, target);
                if (isSame && hasMembership(state, sender, Invite, Join)) {
                    auth.basedOn(state.getMembershipEvent(target));
                    return auth.allow(stateBuilder);
                }

                if (!hasMembership(state, sender, Join)) {
                    return auth.deny(ev, "sender cannot send leave in a room they are not in");
                }

                if (isMembership(computeMembershipValue(state, target), Ban) && !effectivePls.canBan(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum ban PL");
                }

                if (!effectivePls.canKick(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum kick PL");
                }

                if (senderPl <= targetPl) {
                    return auth.deny(ev, "sender PL is not higher than target PL");
                }

                auth.basedOn(state.getEventFor(StateTuple.of(RoomEventType.PowerLevels)));
                return auth.allow(stateBuilder);
            } else if (Ban.is(membership)) {
                if (!isMembership(senderMs, Join)) {
                    return auth.deny(ev, "sender cannot ban in a room they are not in");
                }
                auth.basedOn(state.getMembershipEvent(sender));

                if (!effectivePls.canBan(senderPl)) {
                    return auth.deny(ev, "sender does not have minimum ban PL");
                }

                if (senderPl <= targetPl) {
                    return auth.deny(ev, "sender PL is not higher than target PL");
                }
                auth.basedOn(state.getEventFor(StateTuple.of(RoomEventType.PowerLevels)));

                return auth.allow(stateBuilder);
            } else {
                return auth.deny(ev, "unknown membership: " + membership);
            }
        }

        if (!isMembership(senderMs, Join)) {
            return auth.deny(ev, "sender " + sender + " is not in the room");
        }
        auth.basedOn(state.getMembershipEvent(sender));

        if (!effectivePls.canForEvent(evJson.has(EventKey.StateKey.get()), type, senderPl)) {
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
            auth.basedOn(state.getEventFor(StateTuple.of(RoomEventType.PowerLevels)));

            return auth.allow(stateBuilder);
        }

        if (RoomEventType.Redaction.is(ev.getType())) {
            if (effectivePls.canRedact(senderPl)) {
                auth.basedOn(state.getEventFor(StateTuple.of(RoomEventType.PowerLevels)));
                return auth.allow(stateBuilder);
            }

            if (StringUtils.equals(sender, target)) {
                auth.basedOn(state.getEventFor(StateTuple.of(RoomEventType.PowerLevels)));
                return auth.allow(stateBuilder);
            }

            return auth.deny(ev, "sender does not have minimum redact PL");
        }

        auth.basedOn(state.getEventFor(StateTuple.of(RoomEventType.PowerLevels)));
        return auth.allow(stateBuilder);
    }

}
