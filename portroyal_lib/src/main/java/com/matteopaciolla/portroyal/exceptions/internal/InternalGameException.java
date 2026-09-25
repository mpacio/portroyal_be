package com.matteopaciolla.portroyal.exceptions.internal;

import com.matteopaciolla.portroyal.exceptions.GameException;

public class InternalGameException extends GameException {
    public InternalGameException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalGameException(Throwable cause) {
        super(cause);
    }

    public InternalGameException(String message) {
        super(message);
    }

    public InternalGameException() {
        super();
    }
}
