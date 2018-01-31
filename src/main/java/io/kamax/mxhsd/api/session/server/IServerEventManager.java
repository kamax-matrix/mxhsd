package io.kamax.mxhsd.api.session.server;

import io.kamax.mxhsd.api.event.ISignedEvent;

import java.util.Collection;
import java.util.List;

public interface IServerEventManager {

    List<ISignedEvent> getEvents(Collection<String> ids);

}
