/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxhsd;

import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class Caches {

    private static RuntimeException extract(Throwable t) {
        if (!(t instanceof RuntimeException)) {
            return new RuntimeException(t);
        } else {
            return (RuntimeException) t;
        }
    }

    private static RuntimeException extractCause(Throwable t) {
        if (Objects.isNull(t.getCause())) {
            return extract(t);
        } else {
            return extract(t.getCause());
        }
    }

    public static <K, V> V get(LoadingCache<K, V> cache, K key) {
        try {
            return cache.get(key);
        } catch (UncheckedExecutionException | ExecutionException e) {
            throw extractCause(e);
        }
    }

}
