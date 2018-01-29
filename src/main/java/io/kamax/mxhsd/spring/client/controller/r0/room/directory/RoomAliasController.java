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

package io.kamax.mxhsd.spring.client.controller.r0.room.directory;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.exception.NotFoundException;
import io.kamax.mxhsd.api.room.directory.IRoomAliasLookup;
import io.kamax.mxhsd.spring.client.controller.r0.ClientAPIr0;
import io.kamax.mxhsd.spring.common.controller.EmptyJsonResponse;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = ClientAPIr0.Directory + "/room", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomAliasController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(RoomAliasController.class);

    private final String aliasVariable = "/{alias:.+}";

    private IHomeServer hs;

    public RoomAliasController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @GetMapping(aliasVariable)
    public String resolveAlias(
            HttpServletRequest req,
            @RequestParam("access_token") String token,
            @PathVariable String alias) {
        log(logger, req);

        IRoomAliasLookup lookup = hs.getUserSession(token).getRoomDirectory().lookup(alias)
                .orElseThrow(() -> new NotFoundException(alias));

        JsonObject o = new JsonObject();
        o.addProperty("room_id", lookup.getId());
        o.add("servers", GsonUtil.asArrayObj(lookup.getServers()));

        return toJson(logger, o);
    }

    @PutMapping(aliasVariable)
    public String addAlias(
            HttpServletRequest req,
            @RequestParam("access_token") String token,
            @PathVariable String alias) {
        log(logger, req);

        String roomId = GsonUtil.getOrThrow(getJsonObject(req), "room_id");
        hs.getUserSession(token).getRoomDirectory().add(alias, roomId);
        return EmptyJsonResponse.stringify();
    }

    @DeleteMapping(aliasVariable)
    public String removeAlias(
            HttpServletRequest req,
            @RequestParam("access_token") String token,
            @PathVariable String alias) {
        log(logger, req);

        hs.getUserSession(token).getRoomDirectory().remove(alias);
        return EmptyJsonResponse.stringify();
    }

}
