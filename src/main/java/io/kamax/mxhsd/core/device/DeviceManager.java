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

package io.kamax.mxhsd.core.device;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.device.IDeviceManager;
import io.kamax.mxhsd.api.exception.InvalidTokenException;
import io.kamax.mxhsd.core.GlobalStateHolder;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DeviceManager implements IDeviceManager {

    private final static String UserIdClaim = "user_id";
    private final static String DeviceIdClaim = "device_id";
    private final static String DeviceNameClaim = "device_name";

    private Logger log = LoggerFactory.getLogger(DeviceManager.class);

    private GlobalStateHolder global;
    private Algorithm jwtAlgo;
    private JWTVerifier jwtVerifier;

    private Map<String, IDevice> devByToken; // FIXME use some caching mechanism instead
    private Map<String, IDevice> devById; // FIXME use some caching mechanism instead

    public DeviceManager(GlobalStateHolder global, String jwtSecret) {
        if (StringUtils.isBlank(jwtSecret)) {
            throw new IllegalArgumentException("JWT secret must be set to a non-white/empty value");
        }

        this.global = Objects.requireNonNull(global);

        try {
            jwtAlgo = Algorithm.HMAC256(jwtSecret);
            jwtVerifier = JWT.require(jwtAlgo)
                    .withIssuer(global.getDomain())
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to setup JWT algorithm", e);
        }

        devByToken = new HashMap<>();
        devById = new HashMap<>();
    }

    // FIXME accept device ID. Destroy and recreate Access token for such device if exists.
    @Override
    public synchronized IDevice create(_MatrixID user, String name) { // FIXME use RWLock
        String id;
        do {
            id = RandomStringUtils.randomAlphabetic(12);
        } while (devById.containsKey(id));

        String token = JWT.create()
                .withIssuer(global.getDomain())
                .withExpiresAt(Date.from(Instant.ofEpochMilli(Long.MAX_VALUE)))
                .withClaim(DeviceIdClaim, id)
                .withClaim(DeviceNameClaim, name)
                .withClaim(UserIdClaim, Objects.requireNonNull(user).getId())
                .sign(jwtAlgo);

        IDevice dev = new Device(id, token, name, user);
        devById.put(id, dev);
        devByToken.put(token, dev);

        log.info("Created new device {} for user {}", id, user.getId());

        return dev;
    }

    @Override
    public IDevice get(String token) {
        if (StringUtils.isBlank(token)) {
            throw new InvalidTokenException();
        }

        try {
            DecodedJWT jwt = jwtVerifier.verify(token);

            String deviceId = jwt.getClaim(DeviceIdClaim).asString();
            String deviceName = jwt.getClaim(DeviceNameClaim).asString();
            String userId = jwt.getClaim(UserIdClaim).asString();

            if (StringUtils.isAnyBlank(deviceId, userId)) {
                throw new InvalidTokenException();
            }

            return new Device(deviceId, token, deviceName, MatrixID.asAcceptable(userId));
        } catch (JWTVerificationException e) {
            log.warn("Invalid token {}: {}", token, e.getMessage());
            throw new InvalidTokenException();
        }
    }

}
