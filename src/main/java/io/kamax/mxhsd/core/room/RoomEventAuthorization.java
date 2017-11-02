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

import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.room.IRoomState;

public class RoomEventAuthorization {

    public static class Builder {

        private String roomId;
        private RoomEventAuthorization o;

        public Builder(String roomId, IEvent ev) {
            this.roomId = roomId;
            o = new RoomEventAuthorization();
            o.ev = ev;
        }

        private RoomEventAuthorization set(boolean a, String r, IRoomState newState) {
            o.isAuthorized = a;
            o.reason = r;
            o.newState = newState;
            return o;
        }

        public RoomEventAuthorization allow(RoomState.Builder b) {
            return allow(b.build());
        }

        public RoomEventAuthorization allow(IRoomState newState) {
            return set(true, null, newState);
        }

        public RoomEventAuthorization deny(String reason) {
            return set(false, reason, null);
        }

        public RoomEventAuthorization deny(IEvent ev, String reason) {
            return deny("Reject event " + ev.getId() + " in room " + roomId + ": " + reason);
        }

    }

    private IEvent ev;
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

    public IEvent getEvent() {
        return ev;
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
