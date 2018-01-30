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
import io.kamax.mxhsd.api.event.NakedContentEvent;
import io.kamax.mxhsd.api.room.RoomEventType;

public class RoomCreateEvent extends NakedContentEvent {

    public class Content {

        private String creator;

        Content(String creator) {
            this.creator = creator;
        }

        public String getCreator() {
            return creator;
        }

    }

    private String stateKey = "";

    public RoomCreateEvent(JsonObject o) {
        super(o);
    }

    public RoomCreateEvent(String creator) {
        this(creator, creator);
    }

    public RoomCreateEvent(String sender, String creator) {
        super(RoomEventType.Creation.get(), sender);
        content = GsonUtil.makeObj(new Content(creator));
    }

    public String getCreator() {
        return GsonUtil.getString(getContent(), "creator"); // FIXME enum
    }

}
