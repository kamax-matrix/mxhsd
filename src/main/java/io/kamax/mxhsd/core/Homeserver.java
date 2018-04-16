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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.exception.ForbiddenException;
import io.kamax.mxhsd.api.session.server.IServerSession;
import io.kamax.mxhsd.api.session.user.IUserSession;
import io.kamax.mxhsd.core.session.server.ServerSession;
import io.kamax.mxhsd.core.session.user.UserSession;
import io.kamax.mxhsd.core.user.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Homeserver implements IHomeServer {

    private final Logger log = LoggerFactory.getLogger(Homeserver.class);

    private GlobalStateHolder state;

    private Map<String, IUserSession> sessions = new ConcurrentHashMap<>(); // FIXME need session manager

    public Homeserver(GlobalStateHolder state) {
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
        log.info("Performing user authentication of {}", username);
        String rawUserId = state.getAuthMgr().login(getDomain(), username, password).getId();
        try {
            _MatrixID userId = MatrixID.from(rawUserId).valid();
            IDevice dev = state.getDevMgr().create(userId, Long.toString(System.currentTimeMillis())); // FIXME createOrFind() ?
            IUserSession session = new UserSession(state, new User(userId), dev);

            sessions.put(dev.getToken(), session);
            return session;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Matrix ID from auth backend: {}", rawUserId, e);
            throw new ForbiddenException(username + " is not a valid username");
        }
    }

    @Override
    public IUserSession getUserSession(String token) {
        return findUserSession(token).orElseGet(() -> {
            IDevice device = state.getDevMgr().get(token);
            IUserSession session = new UserSession(state, new User(device.getUser()), device);

            sessions.put(device.getToken(), session);
            return session;
        });
    }

    @Override
    public Optional<IUserSession> findUserSession(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public IServerSession getServerSession(String signature) {
        return new ServerSession(state);
    }

    @Override
    public JsonObject getPublicCrypto() {
        JsonObject keys = new JsonObject();
        state.getCryptoMgr().getKeys().forEach(key -> {
            JsonObject keyJson = new JsonObject();
            keyJson.addProperty("key", key.getPublicKey());
            keys.add(key.getId(), keyJson);
        });

        JsonObject oldKeys = new JsonObject();
        state.getCryptoMgr().getOldKeys().forEach(key -> {
            JsonObject keyJson = new JsonObject();
            keyJson.addProperty("key", key.getPublicKey());
            keyJson.addProperty("expired_ts", key.getExpiration().toEpochMilli());
            keys.add(key.getId(), keyJson);
        });

        JsonArray tlsKeys = new JsonArray();
        state.getCryptoMgr().getTlsKeys().forEach(key -> {
            JsonObject keyJson = new JsonObject();
            keyJson.addProperty("sha256", key.getFingerprint());
            tlsKeys.add(keyJson);
        });

        JsonObject obj = new JsonObject();
        obj.addProperty("server_name", state.getDomain());
        // FIXME remove one hour hardcoding
        obj.addProperty("valid_until_ts", Instant.now().plusSeconds(3600).toEpochMilli());
        obj.add("old_verify_keys", oldKeys);
        obj.add("verify_keys", keys);
        obj.add("tls_fingerprints", tlsKeys);

        JsonObject sign = state.getSignMgr().signMessageGson(MatrixJson.encodeCanonical(obj));
        obj.add("signatures", sign);
        return obj;
    }

}
