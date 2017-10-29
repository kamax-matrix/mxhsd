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

import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.device.IDevice;
import io.kamax.mxhsd.api.device.IDeviceManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DeviceManager implements IDeviceManager {

    private Logger log = LoggerFactory.getLogger(DeviceManager.class);

    private Map<String, IDevice> devByToken; // FIXME use some caching mechanism instead
    private Map<String, IDevice> devById; // FIXME use some caching mechanism instead

    public DeviceManager() {
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

        String token;
        do {
            token = RandomStringUtils.randomAlphanumeric(128);
        } while (devByToken.containsKey(token));

        IDevice dev = new Device(id, token, name, user);
        devById.put(id, dev);
        devByToken.put(token, dev);

        log.info("Created new device {} for user {}", id, user.getId());

        return dev;
    }

}
