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

package io.kamax.mxhsd.core.sync;

import io.kamax.mxhsd.api.sync.ISyncOptions;

import java.util.Optional;

public class SyncOptions implements ISyncOptions {

    private String filterId;
    private String since;
    private boolean fullState;
    private Long timeout;

    public SyncOptions() {
        fullState = false;
        timeout = 0L;
    }

    public SyncOptions setFilterId(String filterId) {
        this.filterId = filterId;

        return this;
    }

    @Override
    public Optional<String> getFilterId() {
        return Optional.ofNullable(filterId);
    }

    public SyncOptions setSince(String since) {
        this.since = since;

        return this;
    }

    @Override
    public Optional<String> getSince() {
        return Optional.ofNullable(since);
    }

    public SyncOptions setFullState(boolean fullState) {
        this.fullState = fullState;

        return this;
    }

    @Override
    public boolean isFullState() {
        return fullState;
    }

    public SyncOptions setTimeout(long timeout) {
        this.timeout = timeout;

        return this;
    }

    @Override
    public long getTimeout() {
        return Optional.ofNullable(timeout).orElse(0L);
    }

}
