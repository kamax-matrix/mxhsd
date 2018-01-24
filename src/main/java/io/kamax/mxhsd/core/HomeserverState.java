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

import io.kamax.matrix.sign.KeyManager;
import io.kamax.matrix.sign.SignatureManager;
import io.kamax.mxhsd.api.auth.IAuthProvider;
import io.kamax.mxhsd.api.crypto.ICryptoManager;
import io.kamax.mxhsd.api.device.IDeviceManager;
import io.kamax.mxhsd.api.event.IEventManager;
import io.kamax.mxhsd.api.federation.IFederationNotifier;
import io.kamax.mxhsd.api.federation.IRemoteHomeServerManager;
import io.kamax.mxhsd.api.room.IRoomManager;
import io.kamax.mxhsd.api.room.directory.ICoreRoomDirectory;

public class HomeserverState {

    private String appName;
    private String appVersion;
    private String domain;
    private IAuthProvider authMgr;
    private IDeviceManager devMgr;
    private KeyManager keyMgr;
    private SignatureManager signMgr;
    private ICryptoManager cryptoMgr;
    private IEventManager evMgr;
    private IRoomManager roomMgr;
    private IFederationNotifier fedNotif;
    private IRemoteHomeServerManager hsMgr;
    private ICoreRoomDirectory roomDir;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public IAuthProvider getAuthMgr() {
        return authMgr;
    }

    public void setAuthMgr(IAuthProvider authMgr) {
        this.authMgr = authMgr;
    }

    public IDeviceManager getDevMgr() {
        return devMgr;
    }

    public void setDevMgr(IDeviceManager devMgr) {
        this.devMgr = devMgr;
    }

    public KeyManager getKeyMgr() {
        return keyMgr;
    }

    public void setKeyMgr(KeyManager keyMgr) {
        this.keyMgr = keyMgr;
    }

    public SignatureManager getSignMgr() {
        return signMgr;
    }

    public void setSignMgr(SignatureManager signMgr) {
        this.signMgr = signMgr;
    }

    public IFederationNotifier getFedNotif() {
        return fedNotif;
    }

    public void setFedNotif(IFederationNotifier fedNotif) {
        this.fedNotif = fedNotif;
    }

    public ICryptoManager getCryptoMgr() {
        return cryptoMgr;
    }

    public void setCryptoMgr(ICryptoManager cryptoMgr) {
        this.cryptoMgr = cryptoMgr;
    }

    public IEventManager getEvMgr() {
        return evMgr;
    }

    public void setEvMgr(IEventManager evMgr) {
        this.evMgr = evMgr;
    }

    public IRoomManager getRoomMgr() {
        return roomMgr;
    }

    public void setRoomMgr(IRoomManager roomMgr) {
        this.roomMgr = roomMgr;
    }

    public IRemoteHomeServerManager getHsMgr() {
        return hsMgr;
    }

    public void setHsMgr(IRemoteHomeServerManager hsMgr) {
        this.hsMgr = hsMgr;
    }

    public ICoreRoomDirectory getRoomDir() {
        return roomDir;
    }

    public void setRoomDir(ICoreRoomDirectory roomDir) {
        this.roomDir = roomDir;
    }

}
