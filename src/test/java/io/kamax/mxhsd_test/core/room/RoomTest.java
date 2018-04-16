/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Maxime Dor
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

package io.kamax.mxhsd_test.core.room;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix.crypto.KeyManager;
import io.kamax.matrix.crypto.KeyMemoryStore;
import io.kamax.matrix.crypto.SignatureManager;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IEventReference;
import io.kamax.mxhsd.api.room.PowerLevel;
import io.kamax.mxhsd.api.room.event.RoomCreateEvent;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.api.room.event.RoomMessageEvent;
import io.kamax.mxhsd.api.room.event.RoomPowerLevelEvent;
import io.kamax.mxhsd.core.GlobalStateHolder;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd.core.room.Room;
import io.kamax.mxhsd.core.room.RoomPowerLevels;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;

public class RoomTest {

    private static GlobalStateHolder internals;
    private static MatrixID user;

    private Room room;

    private boolean containsAll(Collection<IEventReference> c, String... v) {
        return c.stream().map(IEventReference::getEventId).collect(Collectors.toList()).containsAll(Arrays.asList(v));
    }

    @BeforeClass
    public static void beforeClass() {
        internals = new GlobalStateHolder();
        internals.setDomain("localhost");
        internals.setKeyMgr(new KeyManager(new KeyMemoryStore("")));
        internals.setSignMgr(new SignatureManager(internals.getKeyMgr(), internals.getDomain()));
        internals.setEvMgr(new EventManager(internals));

        user = MatrixID.from("user", internals.getDomain()).valid();
    }

    @Before
    public void before() {
        room = new Room(internals, UUID.randomUUID().toString().replace("-", ""));
    }

    @Test
    public void eventRelationships() {
        RoomPowerLevels pls = RoomPowerLevels.build().defaults()
                .addUser(user.getId(), PowerLevel.Admin)
                .get();

        IEvent cEv = room.inject(new RoomCreateEvent(user.getId()));
        assertTrue(cEv.getAuthorization().isEmpty());
        assertTrue(cEv.getParents().isEmpty());

        IEvent cJEv = room.inject(new RoomMembershipEvent(user.getId(), RoomMembership.Join.get(), user.getId()));
        assertTrue(cJEv.getAuthorization().size() == 1);
        assertTrue(containsAll(cJEv.getAuthorization(), cEv.getId()));
        assertTrue(containsAll(cJEv.getParents(), cEv.getId()));

        IEvent plEv = room.inject(new RoomPowerLevelEvent(user.getId(), pls));
        assertTrue(containsAll(plEv.getAuthorization(), cEv.getId(), cJEv.getId()));
        assertTrue(containsAll(plEv.getParents(), cJEv.getId()));

        IEvent mEv = room.inject(new RoomMessageEvent(user.getId(), "a"));
        assertTrue(containsAll(mEv.getAuthorization(), cEv.getId(), cJEv.getId(), plEv.getId()));
        assertTrue(containsAll(mEv.getParents(), plEv.getId()));
    }

}
