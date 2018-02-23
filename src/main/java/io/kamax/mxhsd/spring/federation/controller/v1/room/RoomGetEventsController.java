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
import io.kamax.mxhsd.api.event.ISignedEvent;
import io.kamax.mxhsd.api.exception.InvalidRequestException;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import io.kamax.mxhsd.spring.federation.controller.v1.FederationAPIv1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = FederationAPIv1.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomGetEventsController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(RoomJoinController.class);

    private IHomeServer hs;

    @Autowired
    public RoomGetEventsController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @PostMapping("/get_missing_events/{roomId:.+}")
    public String getMissingEvents(
            HttpServletRequest req,
            @PathVariable String roomId
    ) {
        JsonObject body = getJsonObject(req);

        List<String> earliestEv = GsonUtil.findArray(body, "earliest_events")
                .map(a -> GsonUtil.asList(a, String.class))
                .orElseThrow(() -> new InvalidRequestException("Missing key: earliest_events"));

        List<String> latestEv = GsonUtil.findArray(body, "latest_events")
                .map(a -> GsonUtil.asList(a, String.class))
                .orElseThrow(() -> new InvalidRequestException("Missing key: latest_events"));

        // TODO check if this is mandatory
        long limit = GsonUtil.findLong(body, "limit").orElseThrow(() -> new InvalidRequestException("Missing key: limit"));

        // TODO check if this is mandatory
        long minDepth = GsonUtil.findLong(body, "min_depth").orElseThrow(() -> new InvalidRequestException("Missing key: min_depth"));

        List<ISignedEvent> events = hs.getServerSession("").getRoom(roomId).getEventsRange(earliestEv, latestEv, limit, minDepth);
        JsonArray evArray = GsonUtil.asArray(events.stream().map(INakedEvent::getJson).collect(Collectors.toList()));
        return toJson(logger, GsonUtil.makeObj("events", evArray));
    }

}
