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

package io.kamax.mxhsd.core.room;

import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.room.IRoomCreateOptions;

import java.util.Optional;

public class RoomCreateOptions implements IRoomCreateOptions {

    private _MatrixID creator;
    private String name;
    private String topic;
    private String preset;

    public RoomCreateOptions(_MatrixID creator) {
        this.creator = creator;
    }

    @Override
    public _MatrixID getCreator() {
        return creator;
    }

    @Override
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public Optional<String> getTopic() {
        return Optional.ofNullable(topic);
    }

    @Override
    public Optional<String> getPreset() {
        return Optional.ofNullable(preset);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

}
