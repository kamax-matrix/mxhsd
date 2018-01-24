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

package io.kamax.mxhsd.api.room.event;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.NakedContentEvent;
import io.kamax.mxhsd.api.room.RoomEventType;

public class RoomMembershipEvent extends NakedContentEvent {

    public class Content {

        private String membership;

        Content(String membership) {
            this.membership = membership;
        }

        public String getMembership() {
            return membership;
        }
    }

    private String membership;
    private String stateKey;

    public RoomMembershipEvent(JsonObject o) {
        super(o);
        this.stateKey = EventKey.StateKey.getString(o);
        this.membership = EventKey.Membership.getString(o);
    }

    public RoomMembershipEvent(String sender, String membership, String target) {
        super(RoomEventType.Membership.get(), sender);
        this.stateKey = target;
        this.membership = membership;
        setContent(new Content(membership));
    }

    public String getStateKey() {
        return stateKey;
    }

    public String getMembership() {
        return GsonUtil.getString(getContent(), "membership");
    }

}
