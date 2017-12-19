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

package io.kamax.mxhsd_test.core;

import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomEventChunk;
import io.kamax.mxhsd.api.session.user.IUserSession;
import io.kamax.mxhsd.api.sync.ISyncData;
import io.kamax.mxhsd.api.sync.ISyncRoomData;
import io.kamax.mxhsd.core.sync.SyncOptions;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class UserSyncTest extends GenericHomeserverTest {

    @Test
    public void syncAfterRoomCreate() {
        IUserSession session = login();
        IRoom room = createRoomHelper(session);

        // We should have at least two events in the stream (creation, join)
        assertTrue(room.getCurrentState().getStreamIndex() > 0);

        ISyncData syncData = session.fetchData(new SyncOptions());

        // We got data
        assertNotNull(syncData);

        // We got a next batch token
        assertTrue(StringUtils.isNotBlank(syncData.getNextBatchToken()));

        // No room we have been invited to, should be empty
        assertTrue(syncData.getInvitedRooms().isEmpty());

        // We joined one room
        assertTrue(syncData.getJoinedRooms().size() == 1);

        // We filter out rooms that do not match the expected room ID
        List<ISyncRoomData> roomDataList = syncData.getJoinedRooms().stream()
                .filter(d -> StringUtils.equals(room.getId(), d.getRoomId())) // with the right room ID
                .collect(Collectors.toList());

        // There is exactly one room we joined with our ID
        assertTrue(roomDataList.size() == 1);

        ISyncRoomData roomData = roomDataList.get(0);

        // We got at least one timeline event
        assertTrue(!roomData.getTimeline().getEvents().isEmpty());

        // We have a previous batch token
        assertTrue(StringUtils.isNotBlank(roomData.getTimeline().getPreviousBatchToken()));
    }

    @Test
    public void getMessagesAfterRoomCreate() {
        IUserSession session = login();
        IRoom room = createRoomHelper(session);
        ISyncData syncData = session.fetchData(new SyncOptions());

        ISyncRoomData roomData = syncData.getJoinedRooms().stream()
                .filter(d -> StringUtils.equals(room.getId(), d.getRoomId()))
                .collect(Collectors.toList()).get(0);

        String prevToken = roomData.getTimeline().getPreviousBatchToken();

        IRoomEventChunk chunk = session.getRoom(room.getId()).getEventsChunk(prevToken, 10);
        assertNotNull(chunk);

        assertTrue(StringUtils.equals(prevToken, chunk.getStartToken()));

        if (chunk.getEvents().isEmpty()) {
            assertTrue(StringUtils.equals(chunk.getStartToken(), chunk.getEndToken()));
        } else {
            assertTrue(!StringUtils.equals(chunk.getStartToken(), chunk.getEndToken()));
        }
    }

}
