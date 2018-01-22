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

package io.kamax.mxhsd.core.federation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.kamax.mxhsd.api.federation.IRemoteHomeServer;
import io.kamax.mxhsd.api.federation.IRemoteHomeServerManager;
import io.kamax.mxhsd.core.HomeserverState;

import java.util.concurrent.TimeUnit;

public class RemoteHomeServerManager implements IRemoteHomeServerManager {

    private HomeserverState global;
    private LoadingCache<String, RemoteHomeServer> cache;

    public RemoteHomeServerManager(HomeserverState global) {
        this.global = global;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(1000) // FIXME make it configurable
                .expireAfterAccess(3600, TimeUnit.SECONDS) // FIXME make it configurable
                .build(new CacheLoader<String, RemoteHomeServer>() {

                    @Override
                    public RemoteHomeServer load(String key) throws Exception {
                        return new RemoteHomeServer(global, key);
                    }

                });
    }

    @Override
    public IRemoteHomeServer get(String domain) {
        return cache.getUnchecked(domain);
    }

}
