package io.kamax.mxhsd;

public class NoJsonException extends MatrixException {

    public static final String CODE = "M_NOT_JSON";

    public NoJsonException(String message) {
        super(CODE, message);
    }

    public NoJsonException(Throwable t) {
        super(CODE, t.getMessage(), t);
    }

}
