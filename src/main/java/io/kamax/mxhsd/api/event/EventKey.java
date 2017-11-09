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

package io.kamax.mxhsd.api.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.exception.MalformedEventException;

import java.util.Optional;

public enum EventKey {

    AuthEvents("auth_events"),
    Content("content"),
    Depth("depth"),
    Hashes("hashes"),
    Id("event_id"),
    Origin("origin"),
    Timestamp("origin_server_ts"),
    PreviousEvents("prev_events"),
    PreviousState("prev_state"),
    RoomId("room_id"),
    Sender("sender"),
    Signatures("signatures"),
    StateKey("state_key"),
    Type("type"),
    // FIXME not documented at top level (only content), not consistent with other state ev - clarify why?
    Membership("membership"),
    Unsigned("unsigned");

    private String key;

    EventKey(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }

    public JsonObject getObj(JsonObject o) {
        return findObj(o).orElseThrow(() -> new MalformedEventException(key));
    }

    public Optional<JsonElement> findElement(JsonObject o) {
        return GsonUtil.findElement(o, key);
    }

    public JsonElement getElement(JsonObject o) {
        return GsonUtil.findElement(o, key).orElseThrow(() -> new MalformedEventException(key));
    }

    public Optional<JsonObject> findObj(JsonObject o) {
        return GsonUtil.findObj(o, key);
    }

    public Optional<String> findString(JsonObject o) {
        if (o.has(key)) {
            return Optional.ofNullable(o.get(key).getAsString());
        }

        return Optional.empty();
    }

    public String getString(JsonObject o) {
        return GsonUtil.getString(o, key);
    }

}
