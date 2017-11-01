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

import io.kamax.matrix.MatrixID;
import io.kamax.matrix.sign.KeyManager;
import io.kamax.matrix.sign.SignatureManager;
import io.kamax.mxhsd.core.device.DeviceManager;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd.core.room.RoomManager;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;

import static junit.framework.TestCase.assertTrue;

public class GenericHomeserverTest {

    protected Homeserver hs;

    @Before
    public void beforeClass() {
        HomeserverState state = new HomeserverState();
        state.setDomain("localhost");
        state.setDevMgr(new DeviceManager());
        state.setAuthMgr((domain, user, password) -> new MatrixID(user, domain));
        state.setKeyMgr(new KeyManager("data/sign.key"));
        state.setSignMgr(new SignatureManager(state.getKeyMgr(), state.getDomain()));
        state.setEvMgr(new EventManager(state));
        state.setRoomMgr(new RoomManager(state));
        hs = new Homeserver(state);

        assertTrue(StringUtils.isNotBlank(hs.getDomain()));
    }

}