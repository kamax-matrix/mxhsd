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

package io.kamax.mxhsd.spring.controller.client.r0;

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.user.IUserFilter;
import io.kamax.mxhsd.spring.controller.EmptyJsonResponse;
import io.kamax.mxhsd.spring.controller.InvalidRequestException;
import io.kamax.mxhsd.spring.controller.JsonController;
import io.kamax.mxhsd.spring.service.HomeserverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(path = ClientAPIr0.Base + "/user/{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class UserController extends JsonController {

    private IHomeServer hs;

    @Autowired
    public UserController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = POST, path = "/filter")
    public String createFilter(HttpServletRequest req, @PathVariable String userId, @RequestParam("access_token") String token) throws IOException {
        log(req);

        String body = getBody(req);
        IUserFilter filter = hs.getUserSession(token).getForUser(new MatrixID(userId)).getUser().createFilter(body);

        JsonObject reply = new JsonObject();
        reply.addProperty("filter_id", filter.getId());
        return toJson(reply);
    }

    @RequestMapping(method = GET, path = "/filter/{filterId:.+}")
    public String getFilter(
            HttpServletRequest req,
            @RequestParam("access_token") String token,
            @PathVariable String userId,
            @PathVariable String filterId
    ) {
        log(req);

        IUserFilter filter = hs.getUserSession(token).getForUser(new MatrixID(userId)).getUser()
                .findFilter(filterId).orElseThrow(() -> new InvalidRequestException("M_UKNOWN", "Invalid filter ID"));

        return filter.getContent();
    }

    // Required so Riot can create 1:1 room
    // TODO implement
    @RequestMapping(method = PUT, path = "/account_data/{type:.+}")
    public String setAccountData(
            HttpServletRequest req,
            @RequestParam("access_token") String token,
            @PathVariable String userId,
            @PathVariable String type
    ) {
        log(req);

        return EmptyJsonResponse.stringify();
    }

}
