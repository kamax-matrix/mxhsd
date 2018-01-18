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

package io.kamax.mxhsd_test.core;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.IUserRoom;
import io.kamax.mxhsd.api.room.event.RoomCreateEvent;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.api.session.user.IUserSession;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class RoomManagerTest extends GenericHomeserverTest {

    @Test
    public void createPrivateRoom() {
        IUserSession session = login();
        IUserRoom room = createRoomHelper(session);
        assertTrue(StringUtils.isNotBlank(room.getId()));
        assertNotNull(room.getCurrentState());
        assertNotNull(room.getCreation());
        RoomCreateEvent ev = new RoomCreateEvent(room.getCreation().getJson());
        assertTrue(StringUtils.equals(session.getUser().getId().getId(), ev.getCreator()));
    }

    @Test
    public void createDirectMessageRoom() {
        IUserSession session = login();
        RoomCreateOptions opts = new RoomCreateOptions();
        opts.setCreator(session.getUser().getId());
        opts.setPreset("trusted_private_chat");
        opts.addInvitee(MatrixID.asValid("@test02:localhost"));
        IUserRoom room = session.createRoom(opts);
        assertTrue(StringUtils.isNotBlank(room.getId()));
        // FIXME check preset events and invite events
    }

    @Test
    public void leaveRoom() {
        IUserSession session = login();
        String mxid = session.getUser().getId().getId();
        IUserRoom room = createRoomHelper(session);
        room.inject(new RoomMembershipEvent(mxid, RoomMembership.Leave.get(), mxid));
        // FIXME check the returned event content
        IRoomState state = room.getCurrentState();
        assertTrue(!state.getMembership(mxid).isPresent());
    }

}
