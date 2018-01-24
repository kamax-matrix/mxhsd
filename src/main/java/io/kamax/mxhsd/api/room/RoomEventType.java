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

import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

public enum RoomEventType {

    Aliases("m.room.aliases", true),
    Avatar("m.room.avatar", true),
    CanonicalAliases("m.room.canonical_alias", true),
    Creation("m.room.create", true),
    GuestAccess("m.room.guest_access", true),
    HistoryVisibility("m.room.history_visibility", true),
    JoinRules("m.room.join_rules", true),
    Membership("m.room.member", true),
    Message("m.room.message", false),
    PowerLevels("m.room.power_levels", true),
    Redaction("m.room.redaction", false),
    ThirdPartyInvite("m.room.third_party_invite", true),
    Unknown(null, false);

    public static RoomEventType from(String id) {
        return Stream.of(RoomEventType.values()).filter(t -> StringUtils.equals(t.id, id)).findAny().orElse(Unknown);
    }

    private String id;
    private boolean isState;

    RoomEventType(String id, boolean isState) {
        this.id = id;
        this.isState = isState;
    }

    public String get() {
        return id;
    }

    public boolean is(String id) {
        return StringUtils.equals(this.id, id);
    }

    public boolean isState() {
        return isState;
    }

}
