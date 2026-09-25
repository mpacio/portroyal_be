package com.matteopaciolla.portroyal.exceptions.userinput;

public class MoveEndedMatchException extends UserInputException {

    public MoveEndedMatchException() {
        super("Match ended, no more moves allowed");
    }
}
