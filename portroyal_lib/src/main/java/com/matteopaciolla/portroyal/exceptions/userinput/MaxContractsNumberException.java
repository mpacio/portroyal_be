package com.matteopaciolla.portroyal.exceptions.userinput;

public class MaxContractsNumberException extends UserInputException {

    public MaxContractsNumberException(String message) {
        super(message);
    }

    public MaxContractsNumberException() {
        super();
    }

    public MaxContractsNumberException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxContractsNumberException(Throwable cause) {
        super(cause);
    }
}
