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

package io.kamax.mxhsd.spring.controller.client.r0.voip;

import io.kamax.mxhsd.spring.controller.EmptyJsonResponse;
import io.kamax.mxhsd.spring.controller.JsonController;
import io.kamax.mxhsd.spring.controller.client.r0.ClientAPIr0;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = ClientAPIr0.Base + "/voip", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class TurnServersController extends JsonController {

    // TODO implement
    @RequestMapping(method = GET, path = "/turnServer")
    public String getTurnUri(HttpServletRequest req) {
        log(req);

        return EmptyJsonResponse.stringify();
    }

}
