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

package io.kamax.mxhsd.api.federation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.room.directory.IFederatedRoomAliasLookup;

import java.util.Map;
import java.util.Optional;

public interface IRemoteHomeServer {

    String getDomain();

    String getImplementationName();

    String getImplementationVersion();

    Optional<IFederatedRoomAliasLookup> lookup(String roomAlias);

    JsonObject makeJoin(String roomId, _MatrixID joiner);

    JsonObject sendJoin(ISignedEvent ev);

    void pushTransaction(ITransaction t);

    JsonObject send(String method, String path, Map<String, String> parameters, JsonElement payload);

}
