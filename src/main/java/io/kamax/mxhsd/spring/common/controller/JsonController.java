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

package io.kamax.mxhsd.spring.common.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.api.exception.AccessTokenNotFoundException;
import io.kamax.mxhsd.api.exception.NoJsonException;
import io.kamax.mxhsd.api.exception.UnknownException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@CrossOrigin
public class JsonController {

    private final static String headerName = "Authorization";
    private final static String headerValuePrefix = "Bearer ";
    private final static String parameterName = "access_token";

    private final Logger log = LoggerFactory.getLogger(JsonController.class);

    private Gson gson = GsonUtil.buildPretty();

    public void log(Logger log, HttpServletRequest req) {
        log.debug("Request {} {} | {}", req.getMethod(), req.getRequestURL(), req.getQueryString());
        Optional.ofNullable(req.getHeader("Authorization")).ifPresent(h -> log.info("Authorization header: {}", h));
    }

    protected String getBody(HttpServletRequest req) {
        try {
            return IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Unable to get body from request {} {}", req.getMethod(), req.getRequestURL(), e);
            throw new UnknownException(e.getMessage());
        }
    }

    protected String getJson(HttpServletRequest req) {
        String data = getBody(req);
        if (StringUtils.isBlank(data)) {
            throw new NoJsonException("Document is empty");
        }
        return data;
    }

    protected JsonObject getJsonObject(HttpServletRequest req) {
        return GsonUtil.parseObj(getJson(req));
    }

    protected String toJson(Logger log, Object o) {
        String json = gson.toJson(o);
        log.debug("To json:\n{}", json);
        return json;
    }

    private <T> Optional<T> findFirst(Supplier<Optional<T>>... suppliers) {
        return Stream.of(suppliers).map(Supplier::get).filter(Optional::isPresent).map(Optional::get).findFirst();
    }

    private Optional<String> findAccessTokenInHeaders(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(headerName))
                .filter(header -> StringUtils.startsWith(header, headerValuePrefix))
                .map(header -> header.substring(headerValuePrefix.length()));
    }

    private Optional<String> findAccessTokenInQuery(HttpServletRequest request) {
        return Optional.ofNullable(request.getParameter(parameterName));
    }

    protected Optional<String> findAccessToken(HttpServletRequest request) {
        return findFirst(() -> findAccessTokenInHeaders(request), () -> findAccessTokenInQuery(request));
    }

    protected String getAccessToken(HttpServletRequest request) {
        return findAccessToken(request).orElseThrow(AccessTokenNotFoundException::new);
    }

}
