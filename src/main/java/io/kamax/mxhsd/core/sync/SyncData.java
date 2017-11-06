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
import io.kamax.mxhsd.api.sync.ISyncData;
import io.kamax.mxhsd.api.sync.ISyncRoomData;

import java.util.*;

public class SyncData implements ISyncData {

    public static class Builder extends ABuilder<SyncData> {

        private Map<String, SyncRoomData.Builder> invited = new HashMap<>();
        private Map<String, SyncRoomData.Builder> joined = new HashMap<>();
        private Map<String, SyncRoomData.Builder> left = new HashMap<>();

        private SyncRoomData.Builder get(Map<String, SyncRoomData.Builder> map, String id) {
            return map.computeIfAbsent(id, id1 -> new SyncRoomData.Builder().setRoomId(id1));
        }

        @Override
        protected SyncData buildObj() {
            return new SyncData();
        }

        @Override
        public SyncData get() {
            invited.values().forEach(v -> obj.invited.add(v.get()));
            joined.values().forEach(v -> obj.joined.add(v.get()));
            left.values().forEach(v -> obj.left.add(v.get()));

            return super.get();
        }

        public Builder setToken(String token) {
            obj.token = token;
            return this;
        }

        public Builder setInvited(Collection<SyncRoomData.Builder> invited) {
            invited.forEach(this::addInvited);
            return this;
        }

        public Builder addInvited(SyncRoomData.Builder invited) {
            this.invited.put(invited.getRoomId(), invited);
            return this;
        }

        public SyncRoomData.Builder getInvited(String id) {
            return get(invited, id);
        }

        public Builder setJoined(Collection<SyncRoomData.Builder> joined) {
            joined.forEach(this::addJoined);
            return this;
        }

        public Builder addJoined(SyncRoomData.Builder joined) {
            this.joined.put(joined.getRoomId(), joined);
            return this;
        }

        public SyncRoomData.Builder getJoined(String id) {
            return get(joined, id);
        }

        public Builder setLeft(Collection<SyncRoomData.Builder> left) {
            left.forEach(this::addLeft);
            return this;
        }

        public Builder addLeft(SyncRoomData.Builder left) {
            this.left.put(left.getRoomId(), left);
            return this;
        }

        public SyncRoomData.Builder getLeft(String id) {
            return get(left, id);
        }

    }

    public static Builder build() {
        return new Builder();
    }

    private String token;
    private List<ISyncRoomData> invited = new ArrayList<>();
    private List<ISyncRoomData> joined = new ArrayList<>();
    private List<ISyncRoomData> left = new ArrayList<>();

    @Override
    public String getNextBatchToken() {
        return token;
    }

    @Override
    public List<ISyncRoomData> getInvitedRooms() {
        return Collections.unmodifiableList(invited);
    }

    @Override
    public List<ISyncRoomData> getJoinedRooms() {
        return Collections.unmodifiableList(joined);
    }

    @Override
    public List<ISyncRoomData> getLeftRooms() {
        return Collections.unmodifiableList(left);
    }

}
