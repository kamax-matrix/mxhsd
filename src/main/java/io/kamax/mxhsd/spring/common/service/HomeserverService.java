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

package io.kamax.mxhsd.spring.common.service;

import io.kamax.matrix.sign.KeyManager;
import io.kamax.matrix.sign.SignatureManager;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.IHomeserverConfig;
import io.kamax.mxhsd.core.Homeserver;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.device.DeviceManager;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd.core.room.RoomManager;
import io.kamax.mxhsd.core.room.directory.GlobalRoomDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HomeserverService {

    private IHomeServer srv;

    @Autowired
    public HomeserverService(IHomeserverConfig cfg) {
        HomeserverState state = new HomeserverState();
        state.setDomain(cfg.getDomain());
        state.setKeyMgr(new KeyManager("data/sign.key"));
        state.setSignMgr(new SignatureManager(state.getKeyMgr(), state.getDomain()));
        state.setEvMgr(new EventManager(state));
        state.setRoomMgr(new RoomManager(state));
        state.setAuthMgr(new DumbAuthProvider());
        state.setDevMgr(new DeviceManager());
        state.setRoomDir(new GlobalRoomDirectory(state));
        srv = new Homeserver(state);
    }

    public IHomeServer get() {
        return srv;
    }

}
