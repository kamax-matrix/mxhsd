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
import io.kamax.mxhsd.api.room.RoomID;
import io.kamax.mxhsd.api.room.directory.IRoomAliasLookup;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.event.EventManager;
import io.kamax.mxhsd.core.room.directory.RoomDirectory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class RoomDirectoryTest {

    private static HomeserverState state;
    private RoomDirectory dir;

    @BeforeClass
    public static void beforeClass() {
        state = new HomeserverState();
        state.setDomain("localhost");
        state.setEvMgr(new EventManager(state));
    }

    @Before
    public void before() {
        dir = new RoomDirectory(state);
    }

    @Test
    public void basicAddRemove() {
        String id = RoomID.from("a", state.getDomain()).getId();
        String alias = RoomAlias.from("test", state.getDomain()).getId();

        assertTrue(!dir.lookup(alias).isPresent());
        dir.add(alias, id);

        IRoomAliasLookup lookup = dir.lookup(alias).orElseThrow(IllegalStateException::new);
        assertTrue(lookup.getId(), id.equals(lookup.getId()));
        assertTrue(alias.equals(lookup.getAlias()));

        dir.remove(alias);
        assertTrue(!dir.lookup(alias).isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void failToAddExistingAlias() {
        String id = RoomID.from("a", state.getDomain()).getId();
        String alias = RoomAlias.from("test", state.getDomain()).getId();
        dir.add(alias, id);
        dir.add(alias, id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failToRemoveNonExistingAlias() {
        String alias = RoomAlias.from("test", state.getDomain()).getId();
        dir.remove(alias);
    }

}
