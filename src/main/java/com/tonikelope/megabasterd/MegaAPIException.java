package com.tonikelope.megabasterd;

import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class MegaAPIException extends APIException {

    private static final Logger LOG = Logger.getLogger(MegaAPIException.class.getName());

    public MegaAPIException(int code) {
        super(code, "MEGA API ERROR: " + String.valueOf(code));
        _code = code;
    }
}
