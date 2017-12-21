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
import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.exception.InvalidTokenException;
import io.kamax.mxhsd.api.session.server.IServerSession;
import io.kamax.mxhsd.api.session.user.IUserSession;
import io.kamax.mxhsd.api.user.IHomeserverUser;
import io.kamax.mxhsd.core.session.server.ServerSession;
import io.kamax.mxhsd.core.session.user.UserSession;
import io.kamax.mxhsd.core.user.HomeserverUser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Homeserver implements IHomeServer {

    private final Logger log = LoggerFactory.getLogger(Homeserver.class);

    private HomeserverState state;

    private Map<String, IUserSession> sessions = new ConcurrentHashMap<>(); // FIXME need session manager

    public Homeserver(HomeserverState state) {
        if (StringUtils.isAnyBlank(state.getAppName(), state.getAppVersion())) {
            throw new IllegalArgumentException("Application name and version must be set");
        }

        this.state = state;

        log.info("Homeserver is {}/{}", state.getAppName(), state.getAppVersion());
    }

    @Override
    public String getDomain() {
        return state.getDomain();
    }

    @Override
    public IUserSession login(String username, char[] password) {
        _MatrixID mxid = new MatrixID(state.getAuthMgr().login(getDomain(), username, password).getId());
        if (!mxid.isValid()) {
            log.warn("Invalid Matrix ID from auth backend: {}", mxid);
            throw new ForbiddenException("authentication returned invalid Matrix ID");
        }

        IDevice dev = state.getDevMgr().create(mxid, Long.toString(System.currentTimeMillis())); // FIXME createOrFind() ?
        IHomeserverUser user = new HomeserverUser(mxid);
        IUserSession session = new UserSession(state, user, dev);

        sessions.put(dev.getToken(), session);
        return session;
    }

    @Override
    public IUserSession getUserSession(String token) {
        return findUserSession(token).orElseThrow(InvalidTokenException::new);
    }

    @Override
    public Optional<IUserSession> findUserSession(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public IServerSession getServerSession(String signature) {
        return new ServerSession(state);
    }

}
