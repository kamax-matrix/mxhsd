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

package io.kamax.mxhsd.core.federation;

import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.federation.ITransaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Transaction implements ITransaction {

    private String id;
    private String origin;
    private Instant ts;
    private List<ISignedEvent> pdus;

    public Transaction(String id, String origin, Instant ts, Collection<ISignedEvent> pdus) {
        this.id = id;
        this.origin = origin;
        this.ts = ts;
        this.pdus = Collections.unmodifiableList(new ArrayList<>(pdus));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    @Override
    public Instant getOriginTimestamp() {
        return ts;
    }

    @Override
    public Collection<ISignedEvent> getPdus() {
        return pdus;
    }

}
