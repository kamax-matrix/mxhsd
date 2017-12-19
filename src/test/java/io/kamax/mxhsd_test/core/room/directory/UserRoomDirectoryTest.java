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

package io.kamax.mxhsd_test.core.room.directory;

import io.kamax.mxhsd.api.room.RoomAlias;
import io.kamax.mxhsd.api.room.directory.IRoomAliasLookup;
import io.kamax.mxhsd.api.session.user.IUserRoomDirectory;
import io.kamax.mxhsd.api.session.user.IUserSession;
import io.kamax.mxhsd.core.room.directory.GlobalRoomDirectory;
import io.kamax.mxhsd_test.core.GenericHomeserverTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;

public class UserRoomDirectoryTest extends GenericHomeserverTest {

    private IUserSession s;
    private IUserRoomDirectory uDir;

    @Before
    public void before() {
        state.setRoomDir(new GlobalRoomDirectory(state));
        uDir = (s = login()).getRoomDirectory();
    }

    @Test
    public void basicAddRemove() {
        String id = createRoomHelper(s).getId();
        String alias = RoomAlias.from("test", state.getDomain()).getId();

        assertTrue(!uDir.lookup(alias).isPresent());
        uDir.add(alias, id);

        IRoomAliasLookup lookup = uDir.lookup(alias).orElseThrow(IllegalStateException::new);
        assertTrue(lookup.getId(), id.equals(lookup.getId()));
        assertTrue(alias.equals(lookup.getAlias()));

        uDir.remove(alias);
        Optional<IRoomAliasLookup> o = uDir.lookup(alias);
        assertTrue(!o.isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void failToAddExistingAlias() {
        String id = createRoomHelper(s).getId();
        String alias = RoomAlias.from("test", state.getDomain()).getId();
        uDir.add(alias, id);
        uDir.add(alias, id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failToRemoveNonExistingAlias() {
        String alias = RoomAlias.from("test", state.getDomain()).getId();
        uDir.remove(alias);
    }

}
