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

package io.kamax.mxhsd.spring.management.controller;

import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.room.IRoomState;
import io.kamax.mxhsd.core.GlobalStateHolder;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/management/room", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RoomController extends JsonController {

    private final Logger log = LoggerFactory.getLogger(RoomController.class);

    private GlobalStateHolder global;

    @Autowired
    public RoomController(HomeserverService svc) {
        this.global = svc.getState();
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{roomId}/state")
    public String getCurrentState(@PathVariable String roomId) {
        IRoomState state = global.getRoomMgr().getRoom(roomId).getCurrentState();
        return GsonUtil.get().toJson(state);
    }

}
