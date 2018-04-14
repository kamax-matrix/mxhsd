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
import io.kamax.matrix.MatrixID;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.INakedEvent;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.event.NakedContentEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomAliasEvent extends NakedContentEvent {

    public class Content {

        private List<String> aliases;

        Content(List<String> aliases) {
            this.aliases = Collections.unmodifiableList(new ArrayList<>(aliases));
        }

        public List<String> getAliases() {
            return aliases;
        }

    }

    private String stateKey;

    public RoomAliasEvent(String sender, String alias) {
        this(sender, Collections.singletonList(alias));
    }

    public RoomAliasEvent(String sender, List<String> alias) {
        super(RoomEventType.Aliases.get(), sender);
        stateKey = new MatrixID(sender).getDomain();
        setContent(new Content(alias));
    }

    public RoomAliasEvent(INakedEvent ev) {
        this(ev.getJson());
    }

    public RoomAliasEvent(JsonObject o) {
        super(o);

        GsonUtil.findString(o, EventKey.StateKey.get()).ifPresent(s -> stateKey = s);

        if (!RoomEventType.Aliases.is(getType())) {
            throw new IllegalArgumentException("Type is not " + RoomEventType.Aliases.get());
        }
    }

    public List<String> getAliases() {
        return GsonUtil.get().fromJson(content, Content.class).getAliases();
    }

}
