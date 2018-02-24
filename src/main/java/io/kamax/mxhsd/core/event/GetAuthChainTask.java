/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Maxime Dor
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

package io.kamax.mxhsd.core.event;

import io.kamax.mxhsd.api.event.IEventReference;
import io.kamax.mxhsd.api.event.ISignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetAuthChainTask extends RecursiveTask<Set<String>> {

    private final Logger logger = LoggerFactory.getLogger(GetAuthChainTask.class);

    private Set<String> toProcess;
    private Function<String, ISignedEvent> fetcher;

    public GetAuthChainTask(Collection<String> toProcess, Function<String, ISignedEvent> fetcher) {
        this.toProcess = new HashSet<>(toProcess);
        this.fetcher = fetcher;
    }

    @Override
    protected Set<String> compute() {
        Set<String> ids = new HashSet<>();
        if (toProcess.isEmpty()) {
            logger.debug("Ignoring empty source events");
            return ids;
        }

        do {
            Set<String> toProcessNext = new HashSet<>();
            toProcess.forEach(id -> {
                ISignedEvent ev = fetcher.apply(id);
                toProcessNext.addAll(ev.getAuthorization().stream()
                        .map(IEventReference::getEventId)
                        // We only want unknown events
                        .filter(authId -> !authId.equals(id) && !ids.contains(id))
                        .collect(Collectors.toList()));
            });
            ids.addAll(toProcess);
            toProcess = toProcessNext;
        } while (toProcess.size() > 0);

        return ids.stream().sorted().collect(Collectors.toSet());
    }

}
