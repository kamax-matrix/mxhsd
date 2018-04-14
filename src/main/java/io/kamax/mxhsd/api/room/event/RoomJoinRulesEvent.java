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
import io.kamax.mxhsd.api.event.EventContentKey;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.event.NakedContentEvent;

public class RoomJoinRulesEvent extends NakedContentEvent {

    public static RoomJoinRulesEvent get(JsonObject o) {
        RoomJoinRulesEvent e = new RoomJoinRulesEvent(o);
        if (!RoomEventType.JoinRules.is(e.getType())) {
            throw new IllegalArgumentException("Expected event JSON with type " + RoomEventType.JoinRules.get() + " but got " + e.getType());
        }
        return e;
    }

    private String stateKey = "";

    private RoomJoinRulesEvent(JsonObject o) {
        super(o);
    }

    public RoomJoinRulesEvent(String sender, String rule) {
        super(RoomEventType.JoinRules.get(), sender);
        content.addProperty(EventContentKey.JoinRule, rule);
    }

    public String getRule() {
        return GsonUtil.getOrThrow(content, EventContentKey.JoinRule);
    }

}
