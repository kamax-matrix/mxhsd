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
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.event.ISimpleRoomEvent;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.RoomEventKey;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.api.room.event.RoomCreateEvent;
import io.kamax.mxhsd.core.HomeserverState;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.kamax.matrix.hs.RoomMembership.*;

public class Room implements IRoom {

    private class RoomMemberState {

        private RoomMembership membership = RoomMembership.Leave;
        private long powerLevel = Long.MIN_VALUE;

        RoomMembership getMembership() {
            return membership;
        }

        public void setMembership(RoomMembership membership) {
            this.membership = membership;
        }

        long getPowerLevel() {
            return powerLevel;
        }

        public void setPowerLevel(long powerLevel) {
            this.powerLevel = powerLevel;
        }

    }

    private class EventValidation {

        private boolean isValid;
        private String reason;

        private EventValidation(boolean isValid, String reason) {
            this.isValid = isValid;
            this.reason = reason;
        }

        boolean isValid() {
            return isValid;
        }

        String getReason() {
            return reason;
        }

    }

    private Logger log = LoggerFactory.getLogger(Room.class);

    private HomeserverState globalState;

    private String id;
    private Map<String, RoomMemberState> members = new HashMap<>();
    private ISignedEvent joinRules;
    private RoomPowerLevels powerlevels;
    private ISignedEvent latestEvent;

    Room(HomeserverState globalState, String id) {
        this.globalState = globalState;
        this.id = id;
    }

    private RoomMemberState getMemberState(String member) {
        return Optional.ofNullable(members.get(member)).orElseGet(RoomMemberState::new);
    }

    private RoomMembership getMembership(String member) {
        return getMemberState(member).getMembership();
    }

    private boolean isMembership(RoomMembership toCheck, RoomMembership... validValues) {
        return toCheck != null && Arrays.asList(validValues).contains(toCheck);
    }

    private boolean hasMembership(String member, RoomMembership... validValues) {
        return isMembership(getMembership(member), validValues);
    }

    private EventValidation valid() {
        return new EventValidation(true, null);
    }

    private EventValidation invalid(IEvent ev, String reason) {
        return new EventValidation(false, "Reject event " + ev.getId() + " in room " + id + ": " + reason);
    }


    // https://matrix.org/speculator/spec/HEAD/server_server/unstable.html#rules
    private synchronized EventValidation isAuthorized(IEvent ev) { // FIXME use a better locking mechanism
        JsonObject evJson = ev.getJson();
        String type = evJson.get(EventKey.Type.get()).getAsString();
        long depth = evJson.get(EventKey.Depth.get()).getAsLong();

        if (RoomEventType.Creation.is(type)) {
            if (depth != 0) {
                return invalid(ev, "depth is not 0");
            }

            if (latestEvent != null) {
                return invalid(ev, "there is a previous event");
            }

            return valid();
        }

        String sender = EventKey.Sender.getString(evJson);
        RoomMemberState senderState = getMemberState(sender);
        RoomMembership senderMs = senderState.getMembership();
        long senderPl = senderState.getPowerLevel();

        String target = EventKey.StateKey.getString(evJson);
        long targetPl = getMemberState(target).getPowerLevel();

        if (RoomEventType.Membership.is(type)) {
            String membership = evJson.get(RoomEventKey.Membership.get()).getAsString();
            if (Join.is(membership)) {
                if (RoomEventType.Creation.is(latestEvent.getType())) {
                    if (depth != 1) {
                        return invalid(ev, "depth is not 1");
                    }

                    if (!StringUtils.equals(EventKey.StateKey.getString(latestEvent.getJson()), sender)) {
                        return invalid(ev, "sender is not creator");
                    }

                    return valid();
                }

                if (!StringUtils.equals(sender, target)) {
                    return invalid(ev, "sender is not the state context");
                }

                if (isMembership(senderMs, Invite, Join)) {
                    return valid();
                }

                if (!StringUtils.equals(EventKey.Content.getObj(evJson).get("join_rule").getAsString(), "public")) {
                    return invalid(ev, "room is private and sender was never invited");
                }

                return valid();
            } else if (Invite.is(membership)) {
                if (!isMembership(senderMs, Join)) {
                    return invalid(ev, "sender cannot invite without being in the room");
                }

                if (isMembership(getMembership(target), Ban, Join)) {
                    return invalid(ev, "invitee is already in the room or is banned from the room");
                }

                if (!powerlevels.canInvite(senderPl)) {
                    return invalid(ev, "sender does not have minimum invite PL");
                }

                return valid();
            } else if (Leave.is(membership)) {
                boolean isSame = StringUtils.equals(sender, target);
                if (isSame && hasMembership(sender, Invite)) {
                    return valid();
                }

                if (isSame && !hasMembership(sender, Join)) {
                    return invalid(ev, "sender cannot leave a room they are not in");
                }

                if (isMembership(getMembership(target), Ban) && !powerlevels.canBan(senderPl)) {
                    return invalid(ev, "sender does not have minimum ban PL");
                }

                if (!powerlevels.canKick(senderPl)) {
                    return invalid(ev, "sender does not have minimum kick PL");
                }

                if (senderPl <= targetPl) {
                    return invalid(ev, "sender PL is not higher than target PL");
                }

                return valid();
            } else if (Ban.is(membership)) {
                if (!isMembership(senderMs, Join)) {
                    return invalid(ev, "sender cannot ban in a room they are not in");
                }

                if (!powerlevels.canBan(senderPl)) {
                    return invalid(ev, "sender does not have minimum ban PL");
                }

                if (senderPl <= targetPl) {
                    return invalid(ev, "sender PL is not higher than target PL");
                }

                return valid();
            } else {
                return invalid(ev, "unknown membership: " + membership);
            }
        }

        if (!isMembership(senderMs, Join)) {
            return invalid(ev, "sender is not in the room");
        }

        if (!powerlevels.canForEvent(type, senderPl)) {
            return invalid(ev, "sender does not have minimum PL for event type " + type);
        }

        if (RoomEventType.PowerLevels.is(ev.getType())) {
            if (!powerlevels.canReplace(sender, senderPl, new RoomPowerLevels(ev))) {
                return invalid(ev, "sender is missing minimum PL to change room PLs");
            }
        }

        if (RoomEventType.Redaction.is(ev.getType())) {
            if (powerlevels.canRedact(senderPl)) {
                return valid();
            }

            if (StringUtils.equals(sender, target)) {
                return valid();
            }

            return invalid(ev, "sender does not have minimum redact PL");
        }

        return valid();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized ISignedEvent inject(ISimpleRoomEvent evSimple) {
        log.info("Room {}: Injecting new event of type {}", id, evSimple.getType());
        IEvent ev = globalState.getEvMgr().populate(evSimple, latestEvent);
        EventValidation val = isAuthorized(ev);
        if (!val.isValid()) {
            log.error(val.getReason());
            throw new ForbiddenException("Illegal event");
        } else {
            log.info("Room {}: storing new event ", id, ev.getId());
            return latestEvent = globalState.getEvMgr().store(ev);
        }
    }

    public ISignedEvent setCreator(_MatrixID creator) {
        return inject(new RoomCreateEvent(id, creator));
    }

}
