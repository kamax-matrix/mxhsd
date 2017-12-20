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

package io.kamax.mxhsd.spring.federation.controller.v1.join;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.exception.InvalidRequestException;
import io.kamax.mxhsd.api.room.RemoteJoinRoomState;
import io.kamax.mxhsd.core.event.SignedEvent;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import io.kamax.mxhsd.spring.federation.controller.v1.FederationAPIv1;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = FederationAPIv1.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomJoinController extends JsonController {

    private IHomeServer hs;

    @Autowired
    public RoomJoinController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @GetMapping("/make_join/{roomId:.+}/{mxIdRaw:.+}")
    public String makeJoin(
            HttpServletRequest req,
            @PathVariable("roomId") String roomId,
            @PathVariable("mxIdRaw") String mxIdRaw
    ) {
        log(req);

        _MatrixID mxId = MatrixID.asValid(mxIdRaw);
        JsonObject event = hs.getServerSession("").getRoom(roomId).makeJoin(mxId);
        return toJson(GsonUtil.getObj("event", event));
    }

    @PutMapping("/send_join/{roomId:.+}/{eventId:.+}")
    public String sendJoin(
            HttpServletRequest req,
            @PathVariable String roomId,
            @PathVariable String eventId
    ) {
        log(req);

        ISignedEvent ev = new SignedEvent(getBody(req));
        toJson(ev);
        if (!StringUtils.equals(eventId, ev.getId())) {
            throw new InvalidRequestException("Event ID in the path request [" + eventId + "] and in the event [" + ev.getId() + "] do not match");
        }

        RemoteJoinRoomState state = hs.getServerSession("").getRoom(roomId).injectJoin(ev);
        JsonObject obj = new JsonObject();
        obj.add("auth_chain", new JsonArray());
        obj.add("state", GsonUtil.asArrayObj(state.getState()));
        return toJson(state);
    }

}
