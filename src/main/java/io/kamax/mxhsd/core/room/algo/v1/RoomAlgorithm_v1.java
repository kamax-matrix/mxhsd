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

package io.kamax.mxhsd.core.room.algo.v1;

import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IHashedProtoEvent;
import io.kamax.mxhsd.api.room.IRoomAlgorithm;
import io.kamax.mxhsd.api.room.IRoomEventAuthorizationAlgorithm;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.core.room.RoomEventAuthorization;

import java.util.Collection;
import java.util.function.Function;

public class RoomAlgorithm_v1 implements IRoomAlgorithm, IRoomEventAuthorizationAlgorithm {

    private RoomStateResolutionAlgorithm_v1 resolv;
    private RoomEventAuthorizationAlgorithm_v1 auth;

    public RoomAlgorithm_v1(Function<String, IEvent> fetcher) {
        auth = new RoomEventAuthorizationAlgorithm_v1();
        resolv = new RoomStateResolutionAlgorithm_v1(auth, fetcher);
    }

    @Override
    public RoomEventAuthorization authorize(IRoomState state, IHashedProtoEvent ev) {
        return auth.authorize(state, ev);
    }

    @Override
    public IRoomState resolve(Collection<IRoomState> states) {
        return resolv.resolve(states);
    }

}
