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
import io.kamax.matrix.sign.KeyManager;
import io.kamax.matrix.sign.SignatureManager;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.session.IUserSession;
import io.kamax.mxhsd.core.Homeserver;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.device.DeviceManager;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import io.kamax.mxhsd.core.room.RoomManager;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;

import static junit.framework.TestCase.assertTrue;

public class GenericHomeserverTest {

    protected HomeserverState state;
    protected Homeserver hs;
    protected final String username = "test";
    protected final String password = username;

    public IUserSession login(String username, String password) {
        return hs.login(username, password.toCharArray());
    }

    public IUserSession login() {
        return login(username, password);
    }

    @Before
    public void beforeClass() {
        state = new HomeserverState();
        state.setDomain("localhost:8779");
        state.setDevMgr(new DeviceManager());
        state.setAuthMgr((domain, user, password) -> new MatrixID(user, domain));
        state.setKeyMgr(new KeyManager("data/sign.key"));
        state.setSignMgr(new SignatureManager(state.getKeyMgr(), state.getDomain()));
        state.setEvMgr(new EventManager(state));
        state.setRoomMgr(new RoomManager(state));
        hs = new Homeserver(state);

        assertTrue(StringUtils.isNotBlank(hs.getDomain()));
    }

    public IRoom createRoomHelper(IUserSession session) {
        RoomCreateOptions opts = new RoomCreateOptions();
        opts.setCreator(session.getUser().getId());
        opts.setPreset("private_chat");
        return session.createRoom(opts);
    }

}
