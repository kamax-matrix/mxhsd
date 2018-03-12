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

package io.kamax.mxhsd.core.room.directory;

import io.kamax.matrix.room.RoomAliasLookup;
import io.kamax.mxhsd.api.room.directory.IFederatedRoomAliasLookup;

import java.util.Collection;

public class FederatedRoomAliasLookup extends RoomAliasLookup implements IFederatedRoomAliasLookup {

    private String source;

    public FederatedRoomAliasLookup(String source, String id, String alias, Collection<String> servers) {
        super(id, alias, servers);
        this.source = source;
    }

    @Override
    public String getSource() {
        return source;
    }

}
