package com.matteopaciolla.portroyal.exceptions.userinput;

public class NotEnoughHiringCapacityException extends UserInputException {
    public NotEnoughHiringCapacityException(String message) {
        super(message);
    }

    public NotEnoughHiringCapacityException() {
        super();
    }

    public NotEnoughHiringCapacityException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughHiringCapacityException(Throwable cause) {
        super(cause);
    }
}
