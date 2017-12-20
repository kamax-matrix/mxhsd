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

import io.kamax.mxhsd.api.event.ISignedEvent;

import java.util.ArrayList;
import java.util.List;

public class RemoteJoinRoomState {

    private List<ISignedEvent> state;

    public RemoteJoinRoomState(List<ISignedEvent> state) {
        this.state = new ArrayList<>(state);
    }

    public List<ISignedEvent> getState() {
        return state;
    }

    public void setState(List<ISignedEvent> state) {
        this.state = state;
    }

}
