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

package io.kamax.mxhsd.core.user;

import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxhsd.api.user.IUser;
import io.kamax.mxhsd.api.user.IUserFilter;
import io.kamax.mxhsd.core.GlobalStateHolder;

import java.util.Optional;

public class User implements IUser {

    private GlobalStateHolder global;
    private _MatrixID id;

    public User(GlobalStateHolder global, _MatrixID id) {
        this.global = global;
        this.id = id;
    }

    @Override
    public _MatrixID getId() {
        return id;
    }

    @Override
    public synchronized IUserFilter createFilter(JsonObject content) { // FIXME use RWLock
        String filterId = global.getStore().putFilter(id.getId(), GsonUtil.get().toJson(content));
        return new UserFilter(filterId, content);
    }

    @Override
    public synchronized Optional<IUserFilter> findFilter(String filterId) { // FIXME use RWLock
        return global.getStore().findFilter(id.getId(), filterId)
                .map(raw -> new UserFilter(filterId, GsonUtil.parseObj(raw)));
    }

}
