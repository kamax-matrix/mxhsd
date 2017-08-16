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

import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.IHomeserverConfig;
import io.kamax.mxhsd.api.IUserSession;
import io.kamax.mxhsd.api.auth.IAuthProvider;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.exception.InvalidTokenException;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Homeserver implements IHomeServer {

    private Logger log = LoggerFactory.getLogger(Homeserver.class);

    private IHomeserverConfig cfg;
    private IAuthProvider authMgr;

    private Map<String, IUserSession> sessions = new ConcurrentHashMap<>();

    public Homeserver(IHomeserverConfig cfg, IAuthProvider authMgr) {
        this.cfg = cfg;
        this.authMgr = authMgr;
    }

    @Override
    public String login(String username, char[] password) {
        _MatrixID mxid = authMgr.login(cfg.getDomain(), username, password);
        if (!mxid.isValid()) {
            log.warn("Invalid Matrix ID from auth backend: {}", mxid);
            throw new ForbiddenException("authentication returned invalid Matrix ID");
        }

        String token;
        do {
            token = RandomStringUtils.randomAlphanumeric(128);
        } while (sessions.containsKey(token));

        IUserSession session = new UserSession();

        sessions.put(token, session);
        return token;
    }

    @Override
    public IUserSession getUserSession(String token) {
        IUserSession session = sessions.get(token);
        if (session == null) {
            throw new InvalidTokenException();
        }

        return session;
    }

}
