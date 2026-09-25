package com.matteopaciolla.portroyal.exceptions.userinput;

public class SelfPickPlayerIndexException extends UserInputException {

    public SelfPickPlayerIndexException(String message) {
        super(message);
    }

    public SelfPickPlayerIndexException() {
        super();
    }

    public SelfPickPlayerIndexException(Throwable cause) {
        super(cause);
    }

    public SelfPickPlayerIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}
