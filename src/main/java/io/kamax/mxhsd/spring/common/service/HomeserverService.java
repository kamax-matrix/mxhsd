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

import io.kamax.matrix.crypto.KeyManager;
import io.kamax.matrix.crypto.SignatureManager;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.IHomeserverConfig;
import io.kamax.mxhsd.api.store.IStore;
import io.kamax.mxhsd.core.GlobalStateHolder;
import io.kamax.mxhsd.core.Homeserver;
import io.kamax.mxhsd.core.crypto.CryptoManager;
import io.kamax.mxhsd.core.device.DeviceManager;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd.core.federation.FederationDomainResolver;
import io.kamax.mxhsd.core.federation.FederationNotifier;
import io.kamax.mxhsd.core.federation.HttpFederationClient;
import io.kamax.mxhsd.core.federation.RemoteHomeServerManager;
import io.kamax.mxhsd.core.room.RoomManager;
import io.kamax.mxhsd.core.room.directory.GlobalRoomDirectory;
import io.kamax.mxhsd.spring.common.config.CryptoConfig;
import io.kamax.mxhsd.spring.common.config.InfoBuildConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class HomeserverService {

    @Autowired
    private InfoBuildConfig info;

    @Autowired
    private IHomeserverConfig cfg;

    @Autowired
    private CryptoConfig cryptCfg;

    @Autowired
    private IStore store;

    private GlobalStateHolder state;
    private IHomeServer srv;

    @PostConstruct
    public void init() {
        state = new GlobalStateHolder();
        state.setAppName(info.getName());
        state.setAppVersion(info.getVersion());
        state.setDomain(cfg.getDomain());
        state.setStore(store);
        state.setKeyMgr(KeyManager.fromFile(cryptCfg.getSeed().get("ed25519")));
        state.setSignMgr(new SignatureManager(state.getKeyMgr(), state.getDomain()));
        state.setCryptoMgr(new CryptoManager(state));
        state.setEvMgr(new EventManager(state));
        state.setRoomMgr(new RoomManager(state));
        state.setFedResolv(new FederationDomainResolver());
        state.setFedClient(new HttpFederationClient(state, state.getFedResolv()));
        state.setFedNotif(new FederationNotifier(state));
        state.setAuthMgr(new DumbAuthProvider());
        state.setDevMgr(new DeviceManager(state, cryptCfg.getSeed().get("jwt")));
        state.setHsMgr(new RemoteHomeServerManager(state));
        state.setRoomDir(new GlobalRoomDirectory(state));
        srv = new Homeserver(state);
    }

    public IHomeServer get() {
        return srv;
    }

    public GlobalStateHolder getState() {
        return state;
    }

}
