package com.tonikelope.megabasterd;

/**
 *
 * @author tonikelope
 */
public class MegaAPIException extends APIException {

    public MegaAPIException(int code) {
        super(code, "MEGA API ERROR: " + String.valueOf(code));
        _code = code;
    }
}
