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

package io.kamax.mxhsd.spring.client.controller;

import io.kamax.mxhsd.spring.common.controller.JsonController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static io.kamax.matrix.json.GsonUtil.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = ClientAPI.Base, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class VersionController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(VersionController.class);

    private final String versions;

    public VersionController() {
        this.versions = get().toJson(makeObj("versions", asArray("r0.3.0")));
    }

    @RequestMapping(method = GET, path = "/versions")
    public String getVersions(HttpServletRequest req) {
        log(logger, req);

        return versions;
    }

}
