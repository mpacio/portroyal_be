package com.matteopaciolla.portroyal.exceptions;

public class IOGameException extends GameException {
    public IOGameException(Throwable cause) {
        super(cause);
    }
    public IOGameException(String message) {
        super(message);
    }
    public IOGameException() {
        super();
    }
}
