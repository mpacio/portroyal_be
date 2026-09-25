package com.matteopaciolla.portroyal.exceptions;

public abstract class GameException extends Exception {
    public GameException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameException(Throwable cause) {
        super(cause);
    }

    public GameException(String message) {
        super(message);
    }

    public GameException() {
        super();
    }
}
