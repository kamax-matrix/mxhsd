/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Maxime Dor
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

import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.event.NakedContentEvent;

public class RoomMessageEvent extends NakedContentEvent {

    private class Content {

        private String msgtype;
        private String body;

        Content(String msgtype, String body) {
            this.msgtype = msgtype;
            this.body = body;
        }

    }

    public RoomMessageEvent(String sender, String msgtype, String body) {
        super(RoomEventType.Message.get(), sender);
        setContent(new Content(msgtype, body));
    }

    public RoomMessageEvent(String sender, String body) {
        this(sender, "m.text", body);
    }

}
