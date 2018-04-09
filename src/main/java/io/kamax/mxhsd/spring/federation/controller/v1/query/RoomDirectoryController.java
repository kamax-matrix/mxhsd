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

package io.kamax.mxhsd.spring.federation.controller.v1.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.exception.NotFoundException;
import io.kamax.mxhsd.api.room.directory.IFederatedRoomAliasLookup;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import io.kamax.mxhsd.spring.federation.controller.v1.FederationAPIv1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = FederationAPIv1.Query, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomDirectoryController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(RoomDirectoryController.class);

    private IHomeServer hs;

    @Autowired
    public RoomDirectoryController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = GET, path = "/directory")
    public String queryRoomAlias(HttpServletRequest req, @RequestParam("room_alias") String roomAlias) {
        log(logger, req);

        IFederatedRoomAliasLookup lookup = hs.getServerSession("").getDirectory().lookup(roomAlias)
                .orElseThrow(() -> new NotFoundException("No room with alias " + roomAlias + " exists"));

        JsonArray servers = new JsonArray();
        lookup.getServers().forEach(servers::add);
        JsonObject body = new JsonObject();
        body.addProperty("room_id", lookup.getId());
        body.add("servers", servers);

        return toJson(logger, body);
    }

}
