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

package io.kamax.mxhsd;

import com.google.gson.*;
import io.kamax.mxhsd.api.exception.InvalidJsonException;

import java.util.Optional;

public class GsonUtil { // FIXME refactor into matrix-java-sdk

    private static Gson instance = build();
    private static Gson instancePretty = buildPretty();

    private static GsonBuilder buildImpl() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .disableHtmlEscaping();
    }

    public static Gson buildPretty() {
        return buildImpl().setPrettyPrinting().create();
    }

    public static Gson build() {
        return buildImpl().create();
    }

    public static JsonObject getObj(Object o) {
        return instance.toJsonTree(o).getAsJsonObject();
    }

    public static Gson get() {
        return instance;
    }

    public static Gson getPretty() {
        return instancePretty;
    }

    public static String getPrettyForLog(Object o) {
        return System.lineSeparator() + getPretty().toJson(o);
    }

    public static JsonElement parse(String s) {
        try {
            return new JsonParser().parse(s);
        } catch (JsonParseException e) {
            throw new InvalidJsonException(e);
        }
    }

    public static JsonObject parseObj(String s) {
        try {
            return parse(s).getAsJsonObject();
        } catch (IllegalStateException e) {
            throw new InvalidJsonException("Not an object");
        }
    }

    public static String getOrThrow(JsonObject obj, String member) {
        if (!obj.has(member)) {
            throw new InvalidJsonException(member + " key is missing");
        }

        return obj.get(member).getAsString();
    }

    public static String getString(JsonObject o, String key) {
        JsonElement el = o.get(key);
        if (el != null && el.isJsonPrimitive()) {
            return el.getAsString();
        } else {
            return null;
        }
    }

    public static Optional<JsonElement> findElement(JsonObject o, String key) {
        return Optional.ofNullable(o.get(key));
    }

    public static Optional<Long> findLong(JsonObject o, String key) {
        return findElement(o, key).map(el -> o.get(key)).map(JsonElement::getAsLong);
    }

    public static Optional<JsonObject> findObj(JsonObject o, String key) {
        if (!o.has(key)) {
            return Optional.empty();
        }

        return Optional.ofNullable(o.getAsJsonObject(key));
    }

}
