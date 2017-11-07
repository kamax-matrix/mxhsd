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

import io.kamax.mxhsd.ABuilder;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.sync.ISyncRoomData;

import java.util.*;

public class SyncRoomData implements ISyncRoomData {

    public static class Builder extends ABuilder<SyncRoomData> {

        private List<String> timelineIds = new ArrayList<>();

        @Override
        protected SyncRoomData buildObj() {
            return new SyncRoomData();
        }

        public String getRoomId() {
            return obj.roomId;
        }

        public Builder setRoomId(String roomId) {
            obj.roomId = roomId;
            return this;
        }

        public Builder setState(Collection<IEvent> state) {
            obj.state = new ArrayList<>();
            return addState(state);
        }

        public Builder addState(Collection<IEvent> state) {
            state.forEach(this::addState);
            return this;
        }

        public Builder addState(IEvent state) {
            if (!timelineIds.contains(state.getId())) obj.state.add(state);
            return this;
        }

        public Builder setTimeline(Collection<? extends IEvent> timeline) {
            obj.timeline = new ArrayList<>();
            timeline.forEach(this::addTimeline);
            return this;
        }

        public Builder addTimeline(IEvent entry) {
            obj.timeline.add(entry);
            timelineIds.add(entry.getId());
            return this;
        }

        public Builder setMembership(String membership) {
            obj.membership = membership;
            return this;
        }

    }

    public static Builder build() {
        return new Builder();
    }

    private String roomId;
    private String membership;
    private List<IEvent> state = new ArrayList<>();
    private List<IEvent> timeline = new ArrayList<>();

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public Optional<String> getMembership() {
        return Optional.ofNullable(membership);
    }

    @Override
    public List<IEvent> getState() {
        return Collections.unmodifiableList(state);
    }

    @Override
    public List<IEvent> getTimeline() {
        return Collections.unmodifiableList(timeline);
    }

}
