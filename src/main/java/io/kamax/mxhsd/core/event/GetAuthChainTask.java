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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetAuthChainTask extends RecursiveTask<Set<String>> {

    private Collection<String> from;
    private Function<String, ISignedEvent> fetcher;

    public GetAuthChainTask(Collection<String> from, Function<String, ISignedEvent> fetcher) {
        this.from = from;
        this.fetcher = fetcher;
    }

    @Override
    protected Set<String> compute() {
        Set<String> ids = new HashSet<>();

        invokeAll(from.stream().map(id -> {
            ISignedEvent ev = fetcher.apply(id);
            return new GetAuthChainTask(ev.getAuthorization().stream()
                    .map(IEventReference::getEventId)
                    .collect(Collectors.toList()), fetcher);
        }).collect(Collectors.toList())).forEach(t -> ids.addAll(t.compute()));

        return ids;
    }

}
