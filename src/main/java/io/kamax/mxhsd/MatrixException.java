package io.kamax.mxhsd;

public class MatrixException extends RuntimeException {

    private String errorCode;
    private String error;

    public MatrixException(String errorCode, String error) {
        this.errorCode = errorCode;
        this.error = error;
    }

    public MatrixException(String errorCode, String error, Throwable t) {
        super(errorCode + ": " + error, t);
        this.errorCode = errorCode;
        this.error = error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getError() {
        return error;
    }

}
