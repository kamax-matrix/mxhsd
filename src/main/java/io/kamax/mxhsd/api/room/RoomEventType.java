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

package io.kamax.mxhsd.api.room;

import org.apache.commons.lang.StringUtils;

public enum RoomEventType {

    Aliases("m.room.aliases"),
    CanonicalAliases("m.room.canonical_alias"),
    Creation("m.room.create"),
    HistoryVisiblity("m.room.history_visibility"),
    JoinRules("m.room.join_rules"),
    Membership("m.room.member"),
    PowerLevels("m.room.power_levels"),
    Redaction("m.room.redaction");

    private String id;

    RoomEventType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean is(String id) {
        return StringUtils.equals(this.id, id);
    }

}
