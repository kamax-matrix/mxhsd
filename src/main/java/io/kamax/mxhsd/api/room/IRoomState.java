/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Kamax Sarl
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

import io.kamax.mxhsd.api.event.IHashedProtoEvent;
import io.kamax.mxhsd.api.event.StateTuple;
import io.kamax.mxhsd.api.room.event.IMembershipContext;
import io.kamax.mxhsd.core.room.RoomPowerLevels;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IRoomState {

    // Basic methods

    Map<StateTuple, IHashedProtoEvent> getEvents();

    Optional<IHashedProtoEvent> findEventFor(StateTuple tuple);

    default IHashedProtoEvent getEventFor(StateTuple key) {
        return findEventFor(key).orElseThrow(() -> new IllegalArgumentException("No event for tuple " + key.toString()));
    }

    // Creation methods

    IHashedProtoEvent getCreation();

    // Memberships methods

    default IHashedProtoEvent getMembershipEvent(String userId) {
        return findEventFor(StateTuple.of(RoomEventType.Membership, userId))
                .orElseThrow(() -> new IllegalArgumentException("No membership event for " + userId));
    }

    Set<IMembershipContext> getMemberships();

    Optional<IMembershipContext> findMembership(String target);

    default Optional<String> findMembershipValue(String target) {
        return findMembership(target).map(IMembershipContext::getMembership);
    }

    // Power Levels methods
    Optional<RoomPowerLevels> getPowerLevels();

    RoomPowerLevels getEffectivePowerLevels();

    String getPowerLevelsEventId();

    // FIXME refactor into own algo
    boolean isAccessibleAs(String user);

    Set<String> getServers();

}
