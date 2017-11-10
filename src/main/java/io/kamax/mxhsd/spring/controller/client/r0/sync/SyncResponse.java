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

package io.kamax.mxhsd.spring.controller.client.r0.sync;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.api.sync.ISyncData;
import io.kamax.mxhsd.api.sync.ISyncRoomData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncResponse {

    private class RoomState {

        private List<JsonObject> events = new ArrayList<>();

        public List<JsonObject> getEvents() {
            return events;
        }

    }

    private class UnreadNotifications {

        //private long highlight_count = 0L;
        //private long notification_count = 0L;

    }

    private class Ephemeral {

        private List<Object> events = new ArrayList<>();

    }

    private class RoomTimeline {

        private List<JsonObject> events = new ArrayList<>();
        private boolean limited = false;
        private String prevBatch;

        public List<JsonObject> getEvents() {
            return events;
        }

        public boolean isLimited() {
            return limited;
        }

        public String getPrevBatch() {
            return prevBatch;
        }

    }

    private class InviteRoom {

        private RoomState inviteState = new RoomState();

        public InviteRoom(ISyncRoomData r) {
            inviteState.events.addAll(r.getState());
        }

        public RoomState getInviteState() {
            return inviteState;
        }

    }

    private class JoinRoom {

        private AccountData accountData = new AccountData();
        private Ephemeral ephemeral = new Ephemeral();
        private RoomState state = new RoomState();
        private RoomTimeline timeline = new RoomTimeline();
        private UnreadNotifications unreadNotifications = new UnreadNotifications();

        public JoinRoom(ISyncRoomData r) {
            state.events.addAll(r.getState());
            timeline.events.addAll(r.getTimeline().getEvents());
            timeline.limited = r.getTimeline().isFiltered();
            timeline.prevBatch = r.getTimeline().getPreviousBatchToken();
            accountData.events.addAll(r.getAccountData().getEvents());
        }

        public RoomState getState() {
            return state;
        }

        public RoomTimeline getTimeline() {
            return timeline;
        }

        public AccountData getAccountData() {
            return accountData;
        }

    }

    private class LeftRoom {
        private RoomState state = new RoomState();
        private RoomTimeline timeline = new RoomTimeline();

        public RoomState getState() {
            return state;
        }

        public RoomTimeline getTimeline() {
            return timeline;
        }
    }

    private class Rooms {

        private Map<String, InviteRoom> invite = new HashMap<>();
        private Map<String, JoinRoom> join = new HashMap<>();
        private Map<String, LeftRoom> left = new HashMap<>();

        public Map<String, InviteRoom> getInvite() {
            return invite;
        }

        public Map<String, JoinRoom> getJoin() {
            return join;
        }

    }

    private class AccountData {

        private List<JsonObject> events = new ArrayList<>();

    }

    private class DeviceLists {

        private List<Object> changed = new ArrayList<>();
        private List<Object> left = new ArrayList<>();

    }

    private class Presence {

        private List<Object> events = new ArrayList<>();

    }

    private class ToDevice {

        private List<Object> events = new ArrayList<>();

    }

    private AccountData accountData = new AccountData();
    private DeviceLists deviceLists = new DeviceLists();
    private String nextBatch;
    private Presence presence = new Presence();
    private Rooms rooms = new Rooms();
    private ToDevice toDevice = new ToDevice();

    public SyncResponse(ISyncData data) {
        nextBatch = data.getNextBatchToken();
        data.getInvitedRooms().forEach(r -> rooms.invite.put(r.getRoomId(), new InviteRoom(r)));
        data.getJoinedRooms().forEach(r -> rooms.join.put(r.getRoomId(), new JoinRoom(r)));
    }

    public String getNextBatch() {
        return nextBatch;
    }

    public Rooms getRooms() {
        return rooms;
    }

}
