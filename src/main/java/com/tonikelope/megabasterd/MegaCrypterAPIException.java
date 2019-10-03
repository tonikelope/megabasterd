package com.tonikelope.megabasterd;

import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class MegaCrypterAPIException extends APIException {

    private static final Logger LOG = Logger.getLogger(MegaCrypterAPIException.class.getName());

    public MegaCrypterAPIException(int code) {
        super(code, "MEGACRYPTER API ERROR: " + String.valueOf(code));
        _code = code;
    }
}
