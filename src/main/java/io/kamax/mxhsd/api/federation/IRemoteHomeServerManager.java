package io.kamax.mxhsd.api.federation;

import java.util.Collection;
import java.util.List;

public interface IRemoteHomeServerManager {

    IRemoteHomeServer get(String domain);

    List<IRemoteHomeServer> get(Collection<String> domains);

}
