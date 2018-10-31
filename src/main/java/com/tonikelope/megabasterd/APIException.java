package com.tonikelope.megabasterd;

/**
 *
 * @author tonikelope
 */
public abstract class APIException extends Exception {

    protected Integer _code;

    public APIException(int code) {
        super("API EXCEPTION: " + String.valueOf(code));
        _code = code;
    }

    public APIException(int code, String message) {
        super(message);
        _code = code;
    }

    public int getCode() {
        return _code;
    }
}
