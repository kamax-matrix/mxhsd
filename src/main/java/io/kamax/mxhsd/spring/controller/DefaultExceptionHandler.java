package io.kamax.mxhsd.spring.controller;

import io.kamax.mxhsd.MatrixException;
import io.kamax.mxhsd.NoJsonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DefaultExceptionHandler {

    private Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    public static String handle(String erroCode, String error) {
        return "{\"errcode\":\"" + erroCode + "\",\"error\":\"" + error + "\"}";
    }

    public String handle(MatrixException e) {
        return handle(e.getErrorCode(), e.getError());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(NoJsonException.class)
    public String handle(NoJsonException e) {
        return handle(e);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    public String handle(Throwable e) {
        log.error("Unknown error", e);
        return handle("M_UNKNOWN", "Internal Server error - Contact the server administrator if this persists");
    }

}
