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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SyncData implements ISyncData {

    public static class Builder extends ABuilder<SyncData> {

        @Override
        protected SyncData buildObj() {
            return new SyncData();
        }

        public Builder setToken(String token) {
            obj.token = token;
            return this;
        }

        public Builder setInvited(Collection<SyncRoomData> invited) {
            obj.invited.clear();
            invited.forEach(this::addInvited);
            return this;
        }

        public Builder addInvited(SyncRoomData invited) {
            obj.invited.add(invited);
            return this;
        }

        public Builder setJoined(Collection<SyncRoomData> joined) {
            obj.joined.clear();
            joined.forEach(this::addJoined);
            return this;
        }

        public Builder addJoined(SyncRoomData joined) {
            obj.joined.add(joined);
            return this;
        }


        public Builder setLeft(Collection<SyncRoomData> left) {
            obj.left.clear();
            left.forEach(this::addLeft);
            return this;
        }

        public Builder addLeft(SyncRoomData left) {
            obj.left.add(left);
            return this;
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
