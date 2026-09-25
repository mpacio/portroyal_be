package com.matteopaciolla.portroyal.exceptions.userinput;

public class BadPhaseOperationException extends UserInputException {

    public BadPhaseOperationException(String message) {
        super(message);
    }

    public BadPhaseOperationException() {
        super();
    }

    public BadPhaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadPhaseOperationException(Throwable cause) {
        super(cause);
    }
}
