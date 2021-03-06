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
import io.kamax.mxhsd.api.event.INakedEvent;

public class NakedEvent implements INakedEvent {

    private String type;
    private String sender;
    private JsonObject unsigned;

    protected final JsonObject init(JsonObject o) {
        this.type = EventKey.Type.getString(o);
        this.sender = EventKey.Sender.getString(o);
        this.unsigned = EventKey.Unsigned.getObj(o);

        return o;
    }

    protected NakedEvent() {
        // for subclasses only
    }

    public NakedEvent(JsonObject o) {
        init(o);
    }

    public NakedEvent(String type, String sender) {
        this.type = type;
        this.sender = sender;
        this.unsigned = new JsonObject();
    }

    @Override
    public String getSender() {
        return sender;
    }

    @Override
    public String getType() {
        return type;
    }

    public JsonObject getUnsigned() {
        return unsigned;
    }

    @Override
    public JsonObject getJson() {
        return GsonUtil.makeObj(this);
    }

}
