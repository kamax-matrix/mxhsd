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

import com.google.gson.JsonObject;
import io.kamax.mxhsd.ABuilder;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.EventKey;
import io.kamax.mxhsd.api.event.IProtoEvent;
import io.kamax.mxhsd.api.sync.ISyncRoomAccountData;
import io.kamax.mxhsd.api.sync.ISyncRoomData;
import io.kamax.mxhsd.api.sync.ISyncRoomTimeline;

import java.util.*;

public class SyncRoomData implements ISyncRoomData {

    public static class AccountData implements ISyncRoomAccountData {

        private List<JsonObject> events = new ArrayList<>();

        @Override
        public List<JsonObject> getEvents() {
            return events;
        }

    }

    public static class Timeline implements ISyncRoomTimeline {

        private boolean limited;
        private List<JsonObject> events = new ArrayList<>();
        private String prevBatchToken;

        @Override
        public List<JsonObject> getEvents() {
            return Collections.unmodifiableList(events);
        }

        @Override
        public boolean isFiltered() {
            return limited;
        }

        @Override
        public String getPreviousBatchToken() {
            return prevBatchToken;
        }

    }

    public static class Builder extends ABuilder<SyncRoomData> {

        private List<String> timelineIds = new ArrayList<>();

        private void copyKey(EventKey key, JsonObject origin, JsonObject destination) {
            GsonUtil.findElement(origin, key.get()).ifPresent(el -> destination.add(key.get(), el));
        }

        private JsonObject getFormatedEvent(IProtoEvent ev) {
            JsonObject origin = ev.getJson();
            JsonObject formated = new JsonObject();

            copyKey(EventKey.Id, origin, formated);
            copyKey(EventKey.Content, origin, formated);
            copyKey(EventKey.Timestamp, origin, formated);
            copyKey(EventKey.Sender, origin, formated);
            copyKey(EventKey.StateKey, origin, formated);
            copyKey(EventKey.Type, origin, formated);

            JsonObject unsigned = new JsonObject();
            unsigned.addProperty("age", 0);
            formated.add(EventKey.Unsigned.get(), unsigned);

            return formated;
        }

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

        public Builder setState(Collection<IProtoEvent> state) {
            obj.state = new ArrayList<>();
            return addState(state);
        }

        public Builder addState(Collection<IProtoEvent> state) {
            state.forEach(this::addState);
            return this;
        }

        public Builder addEvent(IProtoEvent event, boolean isState) {
            if (isState) addState(event);
            else addTimeline(event);
            return this;
        }

        public Builder addState(IProtoEvent state) {
            if (!timelineIds.contains(state.getId())) obj.state.add(getFormatedEvent(state));
            return this;
        }

        public Builder setTimeline(Collection<? extends IProtoEvent> timeline) {
            obj.timeline.events = new ArrayList<>();
            timeline.forEach(this::addTimeline);
            return this;
        }

        public Builder addTimeline(IProtoEvent entry) {
            obj.timeline.events.add(getFormatedEvent(entry));
            timelineIds.add(entry.getId());
            return this;
        }

        public Builder setMembership(String membership) {
            obj.membership = membership;
            return this;
        }

        public Builder setLimited(boolean isLimited) {
            obj.timeline.limited = isLimited;
            return this;
        }

        public Builder setPreviousBatchToken(String token) {
            obj.timeline.prevBatchToken = token;
            return this;
        }

        public Builder addAccountData(JsonObject event) {
            obj.accountData.events.add(event);
            return this;
        }

    }

    public static Builder build() {
        return new Builder();
    }

    private String roomId;
    private String membership;
    private List<JsonObject> state = new ArrayList<>();
    private Timeline timeline = new Timeline();
    private AccountData accountData = new AccountData();

    @Override
    public String getRoomId() {
        return roomId;
    }

    @Override
    public Optional<String> getMembership() {
        return Optional.ofNullable(membership);
    }

    @Override
    public List<JsonObject> getState() {
        return Collections.unmodifiableList(state);
    }

    @Override
    public Timeline getTimeline() {
        return timeline;
    }

    @Override
    public ISyncRoomAccountData getAccountData() {
        return accountData;
    }

}
