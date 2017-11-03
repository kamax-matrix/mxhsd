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
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.exception.NoJsonException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonController {

    protected String getJson(HttpServletRequest req) {
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

    protected JsonObject getJsonObject(HttpServletRequest req) {
        return GsonUtil.parseObj(getJson(req));
    }

}
