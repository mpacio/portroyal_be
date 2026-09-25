package com.matteopaciolla.portroyal.exceptions.userinput;

public class EmptyExpeditionsDemandException extends UserInputException {

    public EmptyExpeditionsDemandException(String message) {
        super(message);
    }

    public EmptyExpeditionsDemandException() {
        super();
    }

    public EmptyExpeditionsDemandException(Throwable cause) {
        super(cause);
    }

    public EmptyExpeditionsDemandException(String message, Throwable cause) {
        super(message, cause);
    }
}
