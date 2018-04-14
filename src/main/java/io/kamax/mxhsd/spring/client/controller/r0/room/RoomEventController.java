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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.room.IRoomEventChunk;
import io.kamax.mxhsd.core.event.NakedContentEvent;
import io.kamax.mxhsd.spring.client.controller.r0.ClientAPIr0;
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

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping(path = ClientAPIr0.Room, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomEventController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(RoomEventController.class);

    private IHomeServer hs;

    @Autowired
    public RoomEventController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = PUT, path = "/send/{eventType}/{txnId:.+}")
    public String inject(
            HttpServletRequest req,
            @PathVariable String roomId,
            @PathVariable String eventType,
            @PathVariable String txnId // FIXME use!
    ) {
        log(logger, req);

        String token = getAccessToken(req);
        JsonObject json = getJsonObject(req);
        NakedContentEvent ev = new NakedContentEvent(eventType, hs.getUserSession(token).getUser().getId().getId(), json);
        ev.getUnsigned().addProperty("transaction_id", txnId);
        IEvent fullEv = hs.getUserSession(token).getRoom(roomId).inject(ev);

        JsonObject response = new JsonObject();
        response.addProperty("event_id", fullEv.getId()); // FIXME no hardcoding, use enum
        long after = System.currentTimeMillis();

        return toJson(logger, response);
    }

    @RequestMapping(method = GET, path = "/messages")
    public String paginateRoomEvents(
            HttpServletRequest req,
            @PathVariable String roomId,
            @RequestParam("dir") String direction,
            @RequestParam String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "limit", required = false) int limit,
            @RequestParam(value = "filter", required = false) String filter
    ) {
        log(logger, req);

        IRoomEventChunk page = hs.getUserSession(getAccessToken(req)).getRoom(roomId).getEventsChunk(from, limit);

        JsonArray evs = new JsonArray();
        page.getEvents().forEach(evs::add);
        JsonObject json = new JsonObject();
        json.addProperty("start", page.getStartToken());
        json.addProperty("end", page.getEndToken());
        json.add("chunk", evs);

        return toJson(logger, json);
    }

}
