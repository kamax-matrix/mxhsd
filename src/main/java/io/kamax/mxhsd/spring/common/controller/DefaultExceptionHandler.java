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

import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxhsd.api.exception.*;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
@ResponseBody
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DefaultExceptionHandler {

    private static Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    public static String handle(String erroCode, String error) {
        log.debug("Reply: {} - {}", erroCode, error);

        JsonObject body = new JsonObject();
        body.addProperty("errcode", erroCode);
        body.addProperty("error", error);
        return GsonUtil.get().toJson(body);
    }

    public static String handle(String error) {
        return handle("M_UNKNOWN", error);
    }

    public String handleException(MatrixException e) {
        return handle(e.getErrorCode(), e.getError());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NoJsonException.class)
    public String handle(NoJsonException e) {
        return handleException(e);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidJsonException.class)
    public String handle(InvalidJsonException e) {
        return handleException(e);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException.class)
    public String handle(InvalidRequestException e) {
        return handleException(e);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handle(IllegalArgumentException e) {
        return handle(e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalStateException.class)
    public String handle(IllegalStateException e) {
        return handle(e.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public String handle(ForbiddenException e) {
        return handleException(e);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidTokenException.class)
    public String handle(InvalidTokenException e) {
        return handleException(e);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public String handle(NotFoundException e) {
        return handleException(e);
    }

    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    @ExceptionHandler(NotImplementedException.class)
    public String handle(NotImplementedException e) {
        JsonObject body = new JsonObject();
        body.addProperty("errcode", "M_NOT_IMPLEMENTED");
        body.addProperty("error", e.getMessage());
        return GsonUtil.get().toJson(body);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    public String handle(Throwable e) {
        log.error("Unknown error", e);
        return handle("M_UNKNOWN", "Internal Server error - Contact the server administrator if this persists");
    }

}
