package com.tonikelope.megabasterd;

import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class ChunkInvalidException extends Exception {

    private static final Logger LOG = Logger.getLogger(ChunkInvalidException.class.getName());

    public ChunkInvalidException(String message) {
        super(message);
    }

}
