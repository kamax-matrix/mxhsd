package io.kamax.mxhsd.api.room;

import java.util.Set;

public interface IRoomStateSnapshot {

    Set<String> getStateEventIds();

    Set<String> getAuthChain();

}
