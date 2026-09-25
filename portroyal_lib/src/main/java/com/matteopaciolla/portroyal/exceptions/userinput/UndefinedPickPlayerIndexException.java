package com.matteopaciolla.portroyal.exceptions.userinput;

public class UndefinedPickPlayerIndexException extends UserInputException {

    public UndefinedPickPlayerIndexException(String message) {
        super(message);
    }

    public UndefinedPickPlayerIndexException() {
        super();
    }

    public UndefinedPickPlayerIndexException(Throwable cause) {
        super(cause);
    }

    public UndefinedPickPlayerIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}
