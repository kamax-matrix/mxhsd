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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.api.session.user.IUserSession;
import io.kamax.mxhsd.spring.common.controller.EmptyJsonResponse;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(path = ClientAPIr0.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AuthenticationController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    private Gson gson = GsonUtil.build();

    private IHomeServer hs;

    @Autowired
    public AuthenticationController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @RequestMapping(method = GET, path = "/login")
    public String getLogin(HttpServletRequest req) {
        log(logger, req);

        return "{\"flows\":[{\"type\":\"m.login.password\"}]}";
    }

    @RequestMapping(method = POST, path = "/login")
    public String postLogin(HttpServletRequest req) throws IOException {
        log(logger, req);

        JsonObject obj = GsonUtil.parse(getJson(req)).getAsJsonObject();
        String username = GsonUtil.getOrThrow(obj, "user");
        char[] password = GsonUtil.getOrThrow(obj, "password").toCharArray();
        IUserSession session = hs.login(username, password);

        JsonObject reply = new JsonObject();
        reply.addProperty("user_id", session.getUser().getId().getId());
        reply.addProperty("access_token", session.getDevice().getToken());
        reply.addProperty("home_server", hs.getDomain());
        reply.addProperty("device_id", session.getDevice().getId());

        return toJson(logger, reply);
    }

    @RequestMapping(method = POST, path = "/tokenrefresh")
    public String tokenRefresh(HttpServletRequest req) {
        log(logger, req);

        throw new NotImplementedException("tokenrefresh");
    }

    @RequestMapping(method = POST, path = "/logout")
    public String logout(HttpServletRequest req) {
        log(logger, req);

        hs.findUserSession(getAccessToken(req)).ifPresent(IUserSession::logout);

        return EmptyJsonResponse.stringify();
    }

}
