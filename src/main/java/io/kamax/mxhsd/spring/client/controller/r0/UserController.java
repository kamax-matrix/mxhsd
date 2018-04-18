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

package io.kamax.mxhsd.spring.client.controller.r0;

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.user.IUserFilter;
import io.kamax.mxhsd.spring.common.controller.EmptyJsonResponse;
import io.kamax.mxhsd.spring.common.controller.InvalidRequestException;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping(path = ClientAPIr0.Base + "/user/{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class UserController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    private IHomeServer hs;

    @Autowired
    public UserController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = POST, path = "/filter")
    public String createFilter(HttpServletRequest req, @PathVariable String userId) {
        log(logger, req);

        JsonObject body = GsonUtil.parseObj(getBody(req));
        IUserFilter filter = hs.getUserSession(getAccessToken(req))
                .getForUser(MatrixID.asAcceptable(userId))
                .getUser()
                .createFilter(body);

        JsonObject reply = new JsonObject();
        reply.addProperty("filter_id", filter.getId());
        return toJson(logger, reply);
    }

    @RequestMapping(method = GET, path = "/filter/{filterId:.+}")
    public String getFilter(
            HttpServletRequest req,
            @PathVariable String userId,
            @PathVariable String filterId
    ) {
        log(logger, req);

        IUserFilter filter = hs.getUserSession(getAccessToken(req)).getForUser(MatrixID.asAcceptable(userId)).getUser()
                .findFilter(filterId).orElseThrow(() -> new InvalidRequestException("M_UNKNOWN", "Invalid filter ID"));

        return GsonUtil.get().toJson(filter.getContent());
    }

    // Required so Riot can create 1:1 room
    // TODO implement
    @RequestMapping(method = PUT, path = "/account_data/{type:.+}")
    public String setAccountData(
            HttpServletRequest req,
            @PathVariable String userId,
            @PathVariable String type
    ) {
        log(logger, req);

        return EmptyJsonResponse.stringify();
    }

    // Riot keeps requesting this, no idea what it is for
    @RequestMapping(method = POST, path = "/openid/request_token")
    public String openIdRequestToken() {
        JsonObject json = new JsonObject();
        json.addProperty("access_token", "dummy");
        json.addProperty("token_type", "Bearer");
        json.addProperty("expires_in", Integer.MAX_VALUE); // a long time
        json.addProperty("matrix_server_name", "example.org");
        return GsonUtil.get().toJson(json);
    }

}
