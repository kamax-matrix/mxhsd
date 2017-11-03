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

package io.kamax.mxhsd.spring.controller.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.room.IRoom;
import io.kamax.mxhsd.core.room.RoomCreateOptions;
import io.kamax.mxhsd.spring.controller.ClientAPIr0;
import io.kamax.mxhsd.spring.controller.JsonController;
import io.kamax.mxhsd.spring.service.HomeserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(path = ClientAPIr0.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomController extends JsonController {

    private IHomeServer hs;

    private Gson gson = GsonUtil.build();

    @Autowired
    public RoomController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = POST, path = "/createRoom")
    public String createRoom(HttpServletRequest req, @RequestParam("access_token") String token) {
        JsonObject o = getJsonObject(req);
        RoomCreateOptions options = new RoomCreateOptions(); // FIXME handle all options correctly

        // FIXME no hardcoding!
        GsonUtil.findArray(o, "invite").ifPresent(v ->
                v.forEach(i -> options.addInvitee(new MatrixID(i.getAsString()))));

        // FIXME no hardcoding!
        GsonUtil.findString(o, "preset").ifPresent(options::setPreset);

        IRoom room = hs.getUserSession(token).createRoom(options);

        JsonObject reply = new JsonObject();
        reply.addProperty("room_id", room.getId());
        return gson.toJson(reply);
    }

}
