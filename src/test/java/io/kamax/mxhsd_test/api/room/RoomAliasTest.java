/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Maxime Dor
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

package io.kamax.mxhsd_test.api.room;

import io.kamax.matrix.room.RoomAlias;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class RoomAliasTest {

    private void test(String localpart, String domain) {
        String id = "#" + localpart + ":" + domain;
        RoomAlias alias = RoomAlias.from(id);
        assertTrue(StringUtils.equals(localpart, alias.getLocalpart()));
        assertTrue(StringUtils.equals(domain, alias.getDomain()));
    }

    @Test
    public void basic() {
        test("room", "domain.tld");
    }

    @Test
    public void withPort() {
        test("room", "domain.tld:123");
    }

}
