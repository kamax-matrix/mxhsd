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
