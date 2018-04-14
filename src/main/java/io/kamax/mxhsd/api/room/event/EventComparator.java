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

package io.kamax.mxhsd.api.room.event;

import com.google.common.collect.Streams;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IProcessedEvent;

import java.util.Comparator;

public class EventComparator {

    private final static Comparator<IProcessedEvent> byPreviousEvents = (o1, o2) -> {
        boolean isO1Parent = Streams.concat(o2.getAuthorization().stream(), o2.getParents().stream())
                .anyMatch(ref -> o1.getId().equals(ref.getEventId()));
        if (isO1Parent) return -1;

        boolean isO2Parent = Streams.concat(o1.getAuthorization().stream(), o1.getParents().stream())
                .anyMatch(ref -> o2.getId().equals(ref.getEventId()));
        if (isO2Parent) return 1;

        return 0;
    };

    public static Comparator<IProcessedEvent> forProcessed() {
        return byPreviousEvents.thenComparingLong(IEvent::getDepth)
                .thenComparing(IEvent::getTimestamp);
    }

}
