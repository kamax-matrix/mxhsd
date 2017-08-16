package io.kamax.mxhsd.spring.controller;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class EmptyJsonResponse {

    private final static EmptyJsonResponse obj = new EmptyJsonResponse();

    public static EmptyJsonResponse get() {
        return obj;
    }

    public static String stringify() {
        return obj.toString();
    }

    @Override
    public String toString() {
        return "{}";
    }

}
