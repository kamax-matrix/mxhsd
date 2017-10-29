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

public class Device implements IDevice {

    private String id;
    private String token;
    private String name;
    private _MatrixID user;

    public Device(String id, String token, String name, _MatrixID user) {
        this.id = id;
        this.token = token;
        this.name = name;
        this.user = user;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public _MatrixID getUser() {
        return user;
    }

    @Override
    public String getToken() {
        return token;
    }

}
