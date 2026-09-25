package com.matteopaciolla.portroyal.exceptions.userinput;

import com.matteopaciolla.portroyal.exceptions.GameException;

public class UserInputException extends GameException {
    public UserInputException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserInputException(Throwable cause) {
        super(cause);
    }

    public UserInputException(String message) {
        super(message);
    }

    public UserInputException() {
        super();
    }
}
