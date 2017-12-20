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

package io.kamax.mxhsd.spring.client.controller.r0.room;

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.room.IUserRoom;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import io.kamax.mxhsd.spring.client.controller.r0.ClientAPIr0;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(path = ClientAPIr0.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomController extends JsonController {

    private IHomeServer hs;

    @Autowired
    public RoomController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = POST, path = "/createRoom")
    public String createRoom(HttpServletRequest req, @RequestParam("access_token") String token) {
        log(req);

        JsonObject o = getJsonObject(req);
        RoomCreateOptions options = new RoomCreateOptions(); // FIXME handle all options correctly

        // FIXME no hardcoding!
        GsonUtil.findArray(o, "invite").ifPresent(v ->
                v.forEach(i -> options.addInvitee(MatrixID.asAcceptable(i.getAsString()))));

        // FIXME no hardcoding!
        GsonUtil.findString(o, "preset").ifPresent(options::setPreset);

        IUserRoom room = hs.getUserSession(token).createRoom(options);

        JsonObject reply = new JsonObject();
        reply.addProperty("room_id", room.getId());
        return toJson(reply);
    }

    @RequestMapping(method = POST, path = "/join/{roomIdOrAlias:.+}")
    public String joinByRoomIdOrAlias(
            HttpServletRequest req,
            @PathVariable String roomIdOrAlias,
            @RequestParam("access_token") String token
    ) {
        log(req);

        IUserRoom r = hs.getUserSession(token).joinRoom(roomIdOrAlias);

        JsonObject json = new JsonObject();
        json.addProperty("room_id", r.getId());

        return toJson(json);
    }


}
