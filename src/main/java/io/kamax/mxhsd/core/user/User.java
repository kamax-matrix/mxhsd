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

import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.user.IFilter;
import io.kamax.mxhsd.api.user.IUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class User implements IUser {

    private _MatrixID id;
    private Map<String, IFilter> filters;

    public User(_MatrixID id) {
        this.id = id;
        filters = new HashMap<>();
    }

    @Override
    public _MatrixID getId() {
        return id;
    }

    @Override
    public synchronized IFilter createFilter(String content) { // FIXME use RWLock
        return filters.computeIfAbsent(Long.toString(System.currentTimeMillis()), new Function<String, IFilter>() {
            @Override
            public IFilter apply(String s) {
                return new Filter(s, content);
            }
        });
    }

    @Override
    public synchronized Optional<IFilter> findFilter(String id) { // FIXME use RWLock
        return Optional.ofNullable(filters.get(id));
    }

}
