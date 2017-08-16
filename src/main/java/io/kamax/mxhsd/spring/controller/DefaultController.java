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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@Controller
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DefaultController {

    private Logger log = LoggerFactory.getLogger(DefaultController.class);

    @RequestMapping("/**")
    public String catchAll(HttpServletRequest req, HttpServletResponse res) {
        StringBuffer url = req.getRequestURL();

        if (!StringUtils.isBlank(req.getQueryString())) {
            url.append("?").append(req.getQueryString());
        }

        StringBuffer postData = new StringBuffer();
        Enumeration<String> postParms = req.getParameterNames();
        while (postParms.hasMoreElements()) {
            String parm = postParms.nextElement();
            if (postData.length() > 0) {
                postData.append("&");
            }
            postData.append(parm).append("=").append(req.getParameter(parm));
        }

        log.warn("Requested unsupported URL: {}", url);
        if (postData.length() > 0) {
            log.warn("POST data: {}", postData);
        }

        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return DefaultExceptionHandler.handle("M_NOT_IMPLEMENTED", "Not implemented");
    }

}
