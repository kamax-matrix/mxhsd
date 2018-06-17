package io.kamax.mxhsd.api.event;

import java.util.List;

public interface EventLookup {

    boolean hasAll();

    List<IProcessedEvent> getFound();

    List<String> getMissing();

}
