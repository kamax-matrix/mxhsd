package io.kamax.mxhsd.core;

public enum UserLoginType {

    Password("m.login.password");

    private String id;

    private UserLoginType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
