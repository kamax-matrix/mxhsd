package io.kamax.mxhsd.api.exception;

public class NotFoundException extends MatrixException {

    public NotFoundException(String error) {
        super("M_NOT_FOUND", error);
    }

}
