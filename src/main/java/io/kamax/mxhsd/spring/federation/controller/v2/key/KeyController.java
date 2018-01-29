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

package io.kamax.mxhsd.spring.federation.controller.v2.key;

import io.kamax.mxhsd.api.IHomeServer;
import io.kamax.mxhsd.spring.common.controller.JsonController;
import io.kamax.mxhsd.spring.common.service.HomeserverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "/_matrix/key/v2", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class KeyController extends JsonController {

    private final Logger logger = LoggerFactory.getLogger(KeyController.class);

    private IHomeServer hs;

    @Autowired
    public KeyController(HomeserverService svc) {
        this.hs = svc.get();
    }

    @GetMapping("/server")
    public String getKeys(HttpServletRequest req) {
        log(logger, req);

        return toJson(logger, hs.getPublicCrypto());
    }

    @GetMapping("/server/{keyId:.+}")
    public String getKey(HttpServletRequest req, @PathVariable String keyId) {
        log(logger, req);

        return toJson(logger, hs.getPublicCrypto());
    }

}
