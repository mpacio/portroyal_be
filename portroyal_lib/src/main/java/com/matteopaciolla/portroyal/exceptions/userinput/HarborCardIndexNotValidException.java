package com.matteopaciolla.portroyal.exceptions.userinput;

public class HarborCardIndexNotValidException extends UserInputException {
    public HarborCardIndexNotValidException(String message) {
        super(message);
    }
    public HarborCardIndexNotValidException() {
        super("The index of the card in the harbor is not valid");
    }
}
