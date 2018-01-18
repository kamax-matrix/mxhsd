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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// FIXME refactor into sdk
public class RoomAlias {

    private static final String sigill = "#";
    private static final Pattern idPattern = Pattern.compile(sigill + "(.+?):(.+)");

    private String id;
    private String localpart;
    private String domain;

    public static RoomAlias from(String localpart, String domain) {
        return from(sigill + localpart + ":" + domain);
    }

    public static RoomAlias from(String id) {
        if (id.length() > 255) {
            throw new IllegalArgumentException("Room aliases cannot be longer than 255 characters");
        }

        Matcher m = idPattern.matcher(id);
        if (!m.matches()) {
            throw new IllegalArgumentException(id + " is not a valid Room alias");
        }

        RoomAlias r = new RoomAlias();
        r.id = id;
        r.localpart = m.group(1);
        r.domain = m.group(2);

        return r;
    }

    public static boolean is(String id) {
        try {
            from(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private RoomAlias() {
        // cannot instanciate directly
    }

    public String getId() {
        return id;
    }

    public String getLocalpart() {
        return localpart;
    }

    public String getDomain() {
        return domain;
    }

}
