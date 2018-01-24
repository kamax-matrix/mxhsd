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
import io.kamax.mxhsd.core.crypto.CryptoManager;
import io.kamax.mxhsd.core.device.DeviceManager;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd.core.federation.FederationNotifier;
import io.kamax.mxhsd.core.federation.RemoteHomeServerManager;
import io.kamax.mxhsd.core.room.RoomManager;
import io.kamax.mxhsd.core.room.directory.GlobalRoomDirectory;
import io.kamax.mxhsd.spring.common.config.InfoBuildConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HomeserverService {

    private HomeserverState state;
    private IHomeServer srv;

    @Autowired
    public HomeserverService(InfoBuildConfig info, IHomeserverConfig cfg) {
        state = new HomeserverState();
        state.setAppName(info.getName());
        state.setAppVersion(info.getVersion());
        state.setDomain(cfg.getDomain());
        state.setKeyMgr(KeyManager.fromFile("data/sign.key"));
        state.setSignMgr(new SignatureManager(state.getKeyMgr(), state.getDomain()));
        state.setCryptoMgr(new CryptoManager(state));
        state.setEvMgr(new EventManager(state));
        state.setRoomMgr(new RoomManager(state));
        state.setFedNotif(new FederationNotifier(state));
        state.setAuthMgr(new DumbAuthProvider());
        state.setDevMgr(new DeviceManager());
        state.setHsMgr(new RemoteHomeServerManager(state));
        state.setRoomDir(new GlobalRoomDirectory(state));
        srv = new Homeserver(state);
    }

    public IHomeServer get() {
        return srv;
    }

    public HomeserverState getState() {
        return state;
    }

}
