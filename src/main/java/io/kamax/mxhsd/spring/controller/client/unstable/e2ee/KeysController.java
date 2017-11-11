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

package io.kamax.mxhsd.spring.controller.client.unstable.e2ee;

import io.kamax.mxhsd.spring.controller.JsonController;
import io.kamax.mxhsd.spring.controller.client.unstable.ClientAPIunstable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
@RequestMapping(path = ClientAPIunstable.Base + "/keys", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class KeysController extends JsonController {

    @RequestMapping(method = POST, path = "/upload/{keyId:.+}")
    public String keyUpload(
            HttpServletRequest req
    ) {
        log(req);

        return "{\"one_time_key_counts\":{\"signed_curve25519\":0}}";
    }

}
