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
import io.kamax.mxhsd.api.user.IFilter;
import io.kamax.mxhsd.spring.controller.ClientAPIr0;
import io.kamax.mxhsd.spring.service.HomeserverService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(path = ClientAPIr0.Base + "/user/{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class UserController {

    private IHomeServer hs;
    private Gson gson;

    @Autowired
    public UserController(HomeserverService svc) {
        this.hs = svc.get();
        gson = GsonUtil.build();
    }

    @RequestMapping(method = POST, path = "/filter")
    public String createFilter(HttpServletRequest req, @PathVariable String userId, @RequestParam("access_token") String token) throws IOException {
        String body = IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
        IFilter filter = hs.getUserSession(token).getForUser(new MatrixID(userId)).getUser().createFilter(body);

        JsonObject reply = new JsonObject();
        reply.addProperty("filter_id", filter.getId());
        return gson.toJson(reply);
    }

}
