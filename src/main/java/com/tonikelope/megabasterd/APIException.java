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
