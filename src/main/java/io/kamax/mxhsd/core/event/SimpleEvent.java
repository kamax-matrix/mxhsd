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
import io.kamax.mxhsd.api.event.ISimpleEvent;

import java.time.Instant;

public abstract class SimpleEvent implements ISimpleEvent {

    private String type;
    private Instant timestamp;

    public SimpleEvent(String type) {
        this(type, Instant.now());
    }

    public SimpleEvent(String type, Instant timestamp) {
        this.type = type;
        this.timestamp = timestamp;
    }

    protected JsonObject getBaseObj() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type);
        obj.addProperty("origin_server_ts", timestamp.toEpochMilli());
        return obj;
    }

    protected abstract JsonObject produce(JsonObject o);

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public JsonObject getJson() {
        return produce(getBaseObj());
    }

}
