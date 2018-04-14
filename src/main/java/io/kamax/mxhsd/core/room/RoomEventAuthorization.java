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

import io.kamax.mxhsd.api.event.EventReference;
import io.kamax.mxhsd.api.event.IEventReference;
import io.kamax.mxhsd.api.event.IHashedProtoEvent;
import io.kamax.mxhsd.api.event.IProtoEvent;
import io.kamax.mxhsd.api.room.IRoomState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomEventAuthorization {

    public static class Builder {

        private RoomEventAuthorization o;

        public Builder(IProtoEvent ev) {
            o = new RoomEventAuthorization();
            o.ev = ev;
        }

        private RoomEventAuthorization set(boolean a, String r, IRoomState newState) {
            o.isAuthorized = a;
            o.reason = r;
            o.newState = newState;
            return o;
        }

        public Builder basedOn(IHashedProtoEvent ev) {
            o.basedOn.add(new EventReference(ev.getId(), ev.getHashes()));
            return this;
        }

        public RoomEventAuthorization allow(RoomState.Builder b) {
            return allow(b.get());
        }

        public RoomEventAuthorization allow(IRoomState newState) {
            return set(true, null, newState);
        }

        public RoomEventAuthorization deny(String reason) {
            return set(false, reason, null);
        }

        public RoomEventAuthorization deny(IProtoEvent ev, String reason) {
            return deny("Reject event " + ev.getId() + ": " + reason);
        }

    }

    private IProtoEvent ev;
    private List<IEventReference> basedOn = new ArrayList<>();
    private boolean isAuthorized;
    private String reason;
    private IRoomState newState;

    private RoomEventAuthorization() {
        // only for builder
    }

    private RoomEventAuthorization(boolean isAuthorized, String reason) {
        this.isAuthorized = isAuthorized;
        this.reason = reason;
    }

    public IProtoEvent getEvent() {
        return ev;
    }

    public List<IEventReference> getBasedOn() {
        return Collections.unmodifiableList(basedOn);
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public String getReason() {
        return reason;
    }

    public IRoomState getNewState() {
        return newState;
    }

}
