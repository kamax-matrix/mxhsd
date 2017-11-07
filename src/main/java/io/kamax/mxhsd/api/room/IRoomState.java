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

import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.room.event.IMembershipContext;
import io.kamax.mxhsd.core.room.RoomPowerLevels;

import java.util.Optional;
import java.util.Set;

public interface IRoomState {

    IEvent getCreation(); // FIXME this should be IRoomCreationContext, IEventReference or ISignedEvent

    Set<IMembershipContext> getMemberships();

    Optional<IMembershipContext> getMembership(String target);

    default Optional<String> getMembershipValue(String target) {
        return getMembership(target).map(IMembershipContext::getMembership);
    }

    boolean hasPowerLevels();

    Optional<RoomPowerLevels> getPowerLevels();

    RoomPowerLevels getEffectivePowerLevels();

    String getPowerLevelsEventId();

    int getStreamIndex();

    boolean isAccessibleAs(String user);

}
