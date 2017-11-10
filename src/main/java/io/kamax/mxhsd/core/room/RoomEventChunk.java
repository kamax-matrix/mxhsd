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

import com.google.gson.JsonObject;
import io.kamax.mxhsd.ABuilder;
import io.kamax.mxhsd.api.room.IRoomEventChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomEventChunk implements IRoomEventChunk {

    public static class Builder extends ABuilder<RoomEventChunk> {

        @Override
        protected RoomEventChunk buildObj() {
            return new RoomEventChunk();
        }

        public Builder setStartToken(String token) {
            obj.start = token;
            return this;
        }

        public Builder setEndToken(String token) {
            obj.end = token;
            return this;
        }

        public Builder addEvent(JsonObject ev) {
            obj.events.add(ev);
            return this;
        }

    }

    private String start;
    private String end;
    private List<JsonObject> events = new ArrayList<>();

    @Override
    public String getStartToken() {
        return start;
    }

    @Override
    public String getEndToken() {
        return end;
    }

    @Override
    public List<JsonObject> getEvents() {
        return Collections.unmodifiableList(events);
    }

}
