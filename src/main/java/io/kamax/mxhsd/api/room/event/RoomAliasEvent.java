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
import io.kamax.mxhsd.api.event.INakedEvent;
import io.kamax.mxhsd.api.event.NakedContentEvent;
import io.kamax.mxhsd.api.room.RoomAlias;
import io.kamax.mxhsd.api.room.RoomEventType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomAliasEvent extends NakedContentEvent {

    private List<String> aliases = new ArrayList<>();

    public RoomAliasEvent(INakedEvent ev) {
        this(ev.getJson());
    }

    public RoomAliasEvent(JsonObject o) {
        super(o);

        if (!RoomEventType.Aliases.is(getType())) {
            throw new IllegalArgumentException("Type is not " + RoomEventType.Aliases.get());
        }

        GsonUtil.getArrayOrThrow(content, "aliases")
                .forEach(v -> aliases.add(RoomAlias.from(v.getAsString()).getId()));
    }

    public List<String> getAliases() {
        return Collections.unmodifiableList(aliases);
    }

}
