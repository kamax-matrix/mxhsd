/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Kamax Sarl
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

import io.kamax.mxhsd.api.room.RoomEventType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

public final class StateTuple {

    public static StateTuple of(RoomEventType type) {
        return of(type, "");
    }

    public static StateTuple of(RoomEventType type, String key) {
        if (!type.isState()) {
            throw new IllegalArgumentException("Type is not state related");
        }

        return new StateTuple(type.get(), key);
    }

    public static StateTuple of(String type, String key) {
        return new StateTuple(type, key);
    }

    private String type;
    private String key;

    public StateTuple(String type, String key) {
        this.type = Objects.requireNonNull(type);
        this.key = Objects.requireNonNull(key);
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        StateTuple that = (StateTuple) o;

        return new EqualsBuilder()
                .append(type, that.type)
                .append(key, that.key)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(type)
                .append(key)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "[" + type + "," + key + "]";
    }

}
