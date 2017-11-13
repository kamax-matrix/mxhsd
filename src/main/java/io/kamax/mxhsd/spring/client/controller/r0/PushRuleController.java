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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = ClientAPIr0.Base + "/pushrules", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PushRuleController extends JsonController {

    private Gson gson = GsonUtil.build();

    // FIXME clean up
    @RequestMapping(method = GET, path = "/")
    public String list(HttpServletRequest req) {
        log(req);

        JsonObject global = new JsonObject();
        global.add("content", new JsonArray());
        global.add("override", new JsonArray());
        global.add("room", new JsonArray());
        global.add("sender", new JsonArray());
        global.add("underride", new JsonArray());
        JsonObject reply = new JsonObject();
        reply.add("global", global);

        return toJson(reply);
    }

}
