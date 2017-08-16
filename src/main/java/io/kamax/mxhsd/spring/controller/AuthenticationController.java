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

package io.kamax.mxhsd.spring.controller;

import com.google.gson.JsonObject;
import io.kamax.mxhsd.api.exception.NoJsonException;
import io.kamax.mxhsd.core.JsonUtil;
import io.kamax.mxhsd.spring.service.HomeserverService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RestController
@RequestMapping(path = APIr0.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AuthenticationController {

    @Autowired
    private HomeserverService svc;

    private String getJson(HttpServletRequest req) {
        try {
            String data = IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
            if (StringUtils.isBlank(data)) {
                throw new NoJsonException("Document is empty");
            }
            return data;
        } catch (IOException e) {
            throw new NoJsonException(e);
        }
    }

    @RequestMapping(method = POST, path = "/login")
    public String login(HttpServletRequest req) throws IOException {
        JsonObject obj = JsonUtil.parse(getJson(req)).getAsJsonObject();
        String username = JsonUtil.getOrThrow(obj, "username");
        char[] password = JsonUtil.getOrThrow(obj, "password").toCharArray();
        return svc.get().login(username, password);
    }

    @RequestMapping(method = POST, path = "/tokenrefresh")
    public String tokenRefresh() {
        throw new NotImplementedException("tokenrefresh");
    }

    @RequestMapping(method = POST, path = "/logout")
    public String logout(@RequestParam("access_token") String accessToken) {
        svc.get().getUserSession(accessToken).logout();

        return EmptyJsonResponse.stringify();
    }

}
