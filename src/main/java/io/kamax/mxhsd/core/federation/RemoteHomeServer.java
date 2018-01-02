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

package io.kamax.mxhsd.core.federation;

import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.federation.IFederationClient;
import io.kamax.mxhsd.api.federation.IRemoteHomeServer;
import io.kamax.mxhsd.api.room.directory.IRoomAliasLookup;
import io.kamax.mxhsd.core.HomeserverState;
import io.kamax.mxhsd.core.room.directory.RoomAliasLookup;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RemoteHomeServer implements IRemoteHomeServer {

    private HomeserverState global;
    private String domain;
    private IFederationClient client;

    public RemoteHomeServer(HomeserverState global, String domain) {
        this.global = global;
        this.domain = domain;
        this.client = new HttpFederationClient(global, new FederationDomainResolver());
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public String getImplementationName() {
        throw new NotImplementedException("");
    }

    @Override
    public String getImplementationVersion() {
        throw new NotImplementedException("");
    }

    @Override
    public Optional<IRoomAliasLookup> lookup(String roomAlias) {
        Map<String, String> parms = new HashMap<>();
        parms.put("room_alias", roomAlias);
        JsonObject obj = client.query(domain, "directory", parms);
        String roomId = GsonUtil.getOrThrow(obj, "room_id");
        List<String> servers = GsonUtil.asList(GsonUtil.getArrayOrThrow(obj, "servers"), String.class);
        return Optional.of(new RoomAliasLookup(roomId, roomAlias, servers));
    }

    @Override
    public JsonObject makeJoin(String roomId, _MatrixID joiner) {
        return client.makeJoin(domain, roomId, joiner);
    }

    @Override
    public JsonObject sendJoin(ISignedEvent ev) {
        throw new NotImplementedException("");
    }

}
