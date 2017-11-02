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

import io.kamax.matrix.hs.RoomMembership;

public class PowerLevelKey {

    public static final String Ban = RoomMembership.Ban.get();
    public static final String Events = "events";
    public static final String EventsDefault = "events_default";
    public static final String Invite = RoomMembership.Invite.get();
    public static final String Kick = "kick";
    public static final String Redact = "redact";
    public static final String StateDefault = "state_default";
    public static final String Users = "users";
    public static final String UsersDefault = "users_default";

}
