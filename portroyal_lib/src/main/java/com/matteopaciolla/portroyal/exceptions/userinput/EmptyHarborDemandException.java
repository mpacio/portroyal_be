package com.matteopaciolla.portroyal.exceptions.userinput;

public class EmptyHarborDemandException extends UserInputException {
    public EmptyHarborDemandException(String message) {
        super(message);
    }

    public EmptyHarborDemandException() {
        super();
    }

    public EmptyHarborDemandException(Throwable cause) {
        super(cause);
    }

    public EmptyHarborDemandException(String message, Throwable cause) {
        super(message, cause);
    }
}
