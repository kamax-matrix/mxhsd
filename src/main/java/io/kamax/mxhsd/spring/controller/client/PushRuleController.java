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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.spring.controller.ClientAPIr0;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = ClientAPIr0.Base + "/pushrules", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PushRuleController {

    private Gson gson = GsonUtil.build();

    @RequestMapping(method = GET, path = "/")
    public String list() {
        JsonObject global = new JsonObject();
        global.addProperty("rule_id", "1");
        global.add("default", new JsonArray());
        global.add("actions", new JsonArray());
        global.add("enabled", new JsonArray());
        JsonObject reply = new JsonObject();
        reply.add("global", global);

        return gson.toJson(reply);
    }

}
