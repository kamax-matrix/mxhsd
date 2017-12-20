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

package io.kamax.mxhsd.core.event;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.ISignedEvent;

import java.util.Collection;

public class SignedEvent extends Event implements ISignedEvent {

    private SignedEvent(String raw, JsonObject o) {
        this(EventKey.Id.getStringOrThrow(o),
                EventKey.Type.getStringOrThrow(o),
                EventKey.Sender.getStringOrThrow(o),
                EventKey.RoomId.getStringOrThrow(o),
                EventKey.Depth.getElement(o).getAsLong(),
                GsonUtil.asList(EventKey.PreviousEvents.getElement(o).getAsJsonArray(), String.class),
                GsonUtil.asList(EventKey.AuthEvents.getElement(o).getAsJsonArray(), String.class),
                raw);
    }

    public SignedEvent(String id, String type, String sender, String roomId, long depth, Collection<String> parents, Collection<String> auth, String json) {
        super(id, type, sender, roomId, depth, parents, auth, json);
    }

    public SignedEvent(String raw) {
        this(raw, GsonUtil.parseObj(raw));
    }

}
