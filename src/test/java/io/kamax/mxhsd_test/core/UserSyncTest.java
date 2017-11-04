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

import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.session.IUserSession;
import io.kamax.mxhsd.api.sync.ISyncData;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import io.kamax.mxhsd.core.sync.SyncOptions;
import org.apache.commons.lang3.StringUtils;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class UserSyncTest extends GenericHomeserverTest {

    //@Test
    public void syncAfterRoomCreate() {
        IUserSession session = hs.login("john", "john".toCharArray());

        RoomCreateOptions opts = new RoomCreateOptions();
        opts.setCreator(session.getUser().getId());
        opts.setPreset("private_chat");
        IRoom room = session.createRoom(opts);

        SyncOptions syncOptions = new SyncOptions();
        syncOptions.setTimeout(0);
        ISyncData syncData = session.fetchData(syncOptions);

        assertNotNull(syncData);
        assertTrue(StringUtils.isNotBlank(syncData.getNextBatchToken()));
    }

}
