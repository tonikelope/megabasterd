package com.tonikelope.megabasterd;

/**
 *
 * @author tonikelope
 */
public class MegaCrypterAPIException extends APIException {

    public MegaCrypterAPIException(int code) {
        super(code, "MEGACRYPTER API ERROR: " + String.valueOf(code));
        _code = code;
    }
}
