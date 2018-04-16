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

package io.kamax.mxhsd.spring.management.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.core.GlobalStateHolder;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/management/federation/client", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class FederationClientController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(FederationClientController.class);

    private GlobalStateHolder state;

    @Autowired
    public FederationClientController(HomeserverService svc) {
        this.state = svc.getState();
    }

    @PostMapping("/send")
    public String send(HttpServletRequest req) {
        log(logger, req);

        JsonObject body = getJsonObject(req);
        String domain = GsonUtil.getString(body, "domain");
        String method = GsonUtil.getString(body, "method");
        String path = GsonUtil.getString(body, "path");
        Map<String, String> parameters = new HashMap<>();
        GsonUtil.findObj(body, "parameters").orElse(new JsonObject())
                .entrySet().forEach(e -> parameters.put(e.getKey(), e.getValue().getAsString()));
        JsonElement payload = body.get("body");

        try {
            return toJson(logger, state.getHsMgr().get(domain).send(method, path, parameters, payload));
        } catch (RuntimeException e) {
            return toJson(logger, GsonUtil.makeObj("error", e.getMessage()));
        }
    }

}
