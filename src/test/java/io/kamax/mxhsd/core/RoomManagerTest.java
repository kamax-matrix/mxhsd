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

package io.kamax.mxhsd.core;

import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.session.IUserSession;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class RoomManagerTest extends GenericHomeserverTest {

    @Test
    public void createRoom() {
        IUserSession session = hs.login("test", "test".toCharArray());
        RoomCreateOptions opts = new RoomCreateOptions(session.getUser().getId());
        IRoom room = session.createRoom(opts);
        assertTrue(StringUtils.isNotBlank(room.getId()));
    }

}
