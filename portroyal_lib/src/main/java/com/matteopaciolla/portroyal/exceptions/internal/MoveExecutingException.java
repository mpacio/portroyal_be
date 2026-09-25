package com.matteopaciolla.portroyal.exceptions.internal;

import com.matteopaciolla.portroyal.core.MoveRecord;

public class MoveExecutingException extends InternalGameException {

    public MoveExecutingException() {
        super();
    }

    public MoveExecutingException(Throwable cause) {
        super(cause);
    }

    public MoveExecutingException(String message) {
        super(message);
    }

    public MoveExecutingException(MoveRecord moveRecord, String message) {
        super(formatMessage(moveRecord, message));
    }

    public MoveExecutingException(MoveRecord moveRecord, String message, Throwable cause) {
        super(formatMessage(moveRecord, message), cause);
    }

    private static String formatMessage(MoveRecord moveRecord, String message) {
        return String.format("%s. Conflicting move = %s", message, moveRecord);
    }
}
