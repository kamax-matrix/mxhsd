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

package io.kamax.mxhsd_test.core.room;

import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.matrix.sign.KeyManager;
import io.kamax.matrix.sign.KeyMemoryStore;
import io.kamax.matrix.sign.SignatureManager;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.api.room.event.RoomPowerLevelEvent;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import io.kamax.mxhsd.core.room.RoomManager;
import io.kamax.mxhsd.core.room.RoomPowerLevels;
import io.kamax.mxhsd.core.room.RoomStateResolutionAlgorithmV1;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/*
 * Current state resolution algorithm at time of writing, vulnerable to state resets
 * Issue to track state resets: https://github.com/matrix-org/synapse/issues/1953
 */
public class RoomStateResolutionAlgoV1Test {

    private static HomeserverState internals;

    @BeforeClass
    public static void beforeClass() {
        internals = new HomeserverState();
        internals.setDomain("localhost");
        internals.setKeyMgr(new KeyManager(new KeyMemoryStore("")));
        internals.setSignMgr(new SignatureManager(internals.getKeyMgr(), internals.getDomain()));
        internals.setEvMgr(new EventManager(internals));
        internals.setRoomMgr(new RoomManager(internals));
    }

    private final Logger log = LoggerFactory.getLogger(RoomStateResolutionAlgoV1Test.class);

    private RoomPowerLevels getPl(IRoom r) {
        return getPl(r.getCurrentState());
    }

    private RoomPowerLevels getPl(IRoomState s) {
        return s.getPowerLevels().orElseThrow(RuntimeException::new);
    }

    /*
     * We will trigger a Power level state reset to validate this algorithm which should remain compatible with synapse
     * Comment that inspired this test case: https://github.com/matrix-org/synapse/issues/1953#issuecomment-334248591
     *
     * This test case will produce the following flow of events:
     * - Room creation
     * - Join of User A (room creator)
     * - Initial Power levels
     * - Invite of User B
     * - Join of User B
     *
     * At this stage, User B has no PL defined, which defaults to 0.
     * We'll use this state as reference A
     *
     * Then we produce the following events:
     * - User A gives PL 100 to User B, which state we'll use as reference B
     * - User B changes the invite PL
     *
     * At this stage, User B has PL 100, invite is set to 10
     * We'll use this state as reference C
     *
     * We pass both states to the algorithm which should output reference A if the state reset happened because:
     * - Reference C requires reference B, which is not evaluated
     * - Reference A will find reference C invalid, since User B has PL 0 and thus cannot set new PLs
     */
    @Test
    public void withStateResetOnPowerLevels() {
        // We create the users involved in this test
        _MatrixID userA = MatrixID.from("a", internals.getDomain()).valid();
        _MatrixID userB = MatrixID.from("b", internals.getDomain()).valid();

        // We create a new room to facilitate the creation of events
        // The room will be auto-populated with default events
        RoomCreateOptions opts = new RoomCreateOptions();
        opts.setCreator(userA);
        IRoom room = internals.getRoomMgr().createRoom(opts);

        // we create a dedicated instance of the algorithm
        RoomStateResolutionAlgorithmV1 algo = new RoomStateResolutionAlgorithmV1(internals, room.getId(), id -> internals.getEvMgr().get(id).get());

        // We invite User B
        room.inject(new RoomMembershipEvent(userA.getId(), RoomMembership.Invite.get(), userB.getId()));
        // User B joins
        ISignedEvent eventA = room.inject(new RoomMembershipEvent(userB.getId(), RoomMembership.Join.get(), userB.getId()));

        // Reference A
        IRoomState stateA = room.getStateFor(eventA.getId());
        RoomPowerLevels aPls = getPl(stateA);

        // We validate the expected PLs for reference A
        assertEquals(0L, aPls.getForUser(userB.getId()));
        assertEquals(0L, (long) aPls.getInvite().orElse(-1L));

        // We give PL 100 to User B
        RoomPowerLevels makeUserBAdminPl = RoomPowerLevels.Builder
                .from(getPl(room))
                .addUser(userB.getId(), 100)
                .get();
        room.inject(new RoomPowerLevelEvent(userA.getId(), makeUserBAdminPl));

        // User B sets invite PL to 10
        RoomPowerLevels changeInvitePl = RoomPowerLevels.Builder
                .from(getPl(room))
                .setInvite(10)
                .get();
        ISignedEvent eventB = room.inject(new RoomPowerLevelEvent(userB.getId(), changeInvitePl));

        // Reference C
        IRoomState stateC = room.getStateFor(eventB.getId());
        assertTrue(!StringUtils.equals(stateA.getPowerLevelsEventId(), stateC.getPowerLevelsEventId()));
        RoomPowerLevels cPls = getPl(stateC);

        // We validate the expected PLs for reference C
        assertEquals(100L, cPls.getForUser(userB.getId()));
        assertEquals(10L, (long) cPls.getInvite().orElse(-1L));

        // We compute the final state, which we expect to be equivalent to A
        IRoomState finalState = algo.resolve(Arrays.asList(stateA, stateC));
        RoomPowerLevels finalPls = getPl(finalState);

        // Some debug statements
        log.debug("State A: {}", GsonUtil.getPrettyForLog(stateA));
        log.debug("State C: {}", GsonUtil.getPrettyForLog(stateC));
        log.debug("Final state: {}", GsonUtil.getPrettyForLog(finalState));

        // The Power levels event should be the one of reference A, since the state should have reset
        assertTrue(StringUtils.equals(finalState.getPowerLevelsEventId(), stateA.getPowerLevelsEventId()));

        // User B and Invite PL should be back to 0
        assertEquals(0L, finalPls.getForUser(userB.getId()));
        assertEquals(0L, (long) finalPls.getInvite().orElse(-1L));
    }

}
