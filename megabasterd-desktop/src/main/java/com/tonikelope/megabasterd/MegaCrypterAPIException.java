/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
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
