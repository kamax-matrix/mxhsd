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

package io.kamax.mxhsd.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.kamax.mxhsd.api.exception.InvalidJsonException;

import java.util.Optional;

public class JsonUtil {

    public static JsonElement parse(String s) {
        try {
            return new JsonParser().parse(s);
        } catch (JsonParseException e) {
            throw new InvalidJsonException(e);
        }
    }

    public static String getOrThrow(JsonObject obj, String member) {
        if (!obj.has(member)) {
            throw new InvalidJsonException(member + " is missing");
        }

        return obj.get(member).getAsString();
    }

    public static JsonObject getObj(JsonObject o, String key) {
        return findObj(o, key).orElseGet(JsonObject::new);
    }

    public static long getLong(JsonObject o, String key, long failover) {
        if (!o.has(key)) {
            return failover;
        }

        return o.get(key).getAsLong();
    }

    public static Optional<JsonObject> findObj(JsonObject o, String key) {
        if (!o.has(key)) {
            return Optional.empty();
        }

        return Optional.ofNullable(o.getAsJsonObject(key));
    }

}
