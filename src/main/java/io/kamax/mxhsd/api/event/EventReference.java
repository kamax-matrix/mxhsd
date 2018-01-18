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

package io.kamax.mxhsd.api.event;

import com.google.gson.JsonArray;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EventReference implements IEventReference {

    private String id;
    private Map<String, String> hashes;

    public EventReference(String id, Map<String, String> hashes) {
        this.id = id;
        this.hashes = new HashMap<>(hashes);
    }

    public EventReference(JsonArray reference) {
        this.id = reference.get(0).getAsString();
        this.hashes = new HashMap<>();
        reference.get(1).getAsJsonObject().entrySet().forEach(e -> hashes.put(e.getKey(), e.getValue().getAsString()));
    }

    @Override
    public String getEventId() {
        return id;
    }

    @Override
    public Map<String, String> getHashes() {
        return Collections.unmodifiableMap(hashes);
    }

}
