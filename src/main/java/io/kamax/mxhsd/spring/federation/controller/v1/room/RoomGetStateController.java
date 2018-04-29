/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2018 Maxime Dor
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

package io.kamax.mxhsd.spring.federation.controller.v1.room;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.event.INakedEvent;
import io.kamax.mxhsd.api.room.IRoomStateSnapshotIds;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import io.kamax.mxhsd.spring.federation.controller.v1.FederationAPIv1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = FederationAPIv1.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomGetStateController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(RoomGetStateController.class);

    private IHomeServer hs;

    public RoomGetStateController(HomeserverService svc) {
        this.hs = svc.get();
    }

    private IRoomStateSnapshotIds getSnapshot(String roomId, String eventId) {
        return hs.getServerSession("").getRoom(roomId).getSnapshot(eventId);
    }

    @GetMapping("/state_ids/{roomId:.+}/")
    public String getStateIdsForEvent(
            HttpServletRequest req,
            @PathVariable String roomId,
            @RequestParam("event_id") String eventId
    ) {
        log(logger, req);

        IRoomStateSnapshotIds snap = getSnapshot(roomId, eventId);
        JsonArray states = GsonUtil.asArray(snap.getStateEventIds());
        JsonArray chain = GsonUtil.asArray(snap.getAuthChainIds());

        JsonObject obj = new JsonObject();
        obj.add("pdu_ids", states);
        obj.add("auth_chain_ids", chain);

        return toJson(logger, obj);
    }

    @GetMapping("/state/{roomId:.+}/")
    public String getStateForEvent(
            HttpServletRequest req,
            @PathVariable String roomId,
            @RequestParam("event_id") String eventId
    ) {
        log(logger, req);

        IRoomStateSnapshotIds snap = getSnapshot(roomId, eventId);
        JsonArray states = GsonUtil.asArray(
                hs.getServerSession("").getEventMgr().getEvents(snap.getStateEventIds()).stream()
                        .map(INakedEvent::getJson)
                        .collect(Collectors.toList())
        );
        JsonArray chain = GsonUtil.asArray(
                hs.getServerSession("").getEventMgr().getEvents(snap.getStateEventIds()).stream()
                        .map(INakedEvent::getJson)
                        .collect(Collectors.toList())
        );

        JsonObject obj = new JsonObject();
        obj.add("pdus", states);
        obj.add("auth_chain", chain);

        return toJson(logger, obj);
    }

}
