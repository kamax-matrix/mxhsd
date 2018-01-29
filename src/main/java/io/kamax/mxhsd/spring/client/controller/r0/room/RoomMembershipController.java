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
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.room.event.RoomMembershipEvent;
import io.kamax.mxhsd.spring.client.controller.r0.ClientAPIr0;
import io.kamax.mxhsd.spring.common.controller.EmptyJsonResponse;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(path = ClientAPIr0.Room, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomMembershipController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(RoomMembershipController.class);

    private IHomeServer hs;

    @Autowired
    public RoomMembershipController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = POST, path = "/invite")
    public String inviteToRoom(HttpServletRequest req, @PathVariable String roomId, @RequestParam("access_token") String token) {
        log(logger, req);

        JsonObject o = getJsonObject(req);
        String sender = hs.getUserSession(token).getUser().getId().getId();
        String target = GsonUtil.getString(o, "user_id");

        hs.getUserSession(token).getRoom(roomId).inject(new RoomMembershipEvent(sender, RoomMembership.Invite.get(), target));

        return EmptyJsonResponse.stringify();
    }

    @RequestMapping(method = POST, path = "/leave")
    public String leaveRoom(HttpServletRequest req, @PathVariable String roomId, @RequestParam("access_token") String token) {
        log(logger, req);

        hs.getUserSession(token).leaveRoom(roomId);

        return EmptyJsonResponse.stringify();
    }

}
