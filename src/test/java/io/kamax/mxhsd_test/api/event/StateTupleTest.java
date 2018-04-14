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

package io.kamax.mxhsd_test.api.event;

import io.kamax.mxhsd.api.event.StateTuple;
import io.kamax.mxhsd.api.room.RoomEventType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class StateTupleTest {

    @Test
    public void equals() {
        StateTuple o1 = StateTuple.of(RoomEventType.Creation);
        StateTuple o2 = StateTuple.of(RoomEventType.Creation);
        assertEquals(o1, o2);
    }

    @Test
    public void asMapKey() {
        StateTuple o1 = StateTuple.of(RoomEventType.Creation);
        String value = "value";

        Map<StateTuple, String> map = new HashMap<>();
        map.put(o1, value);

        StateTuple o2 = StateTuple.of(RoomEventType.Creation);
        assertTrue(map.containsKey(o2));
        assertTrue(StringUtils.equals(value, map.get(o2)));
    }

}
