package com.matteopaciolla.prbe.exceptions.game;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

/**
 * Exception thrown when a player which is not running tries to perform an action.
 */
public class NotRunningPlayerException extends BaseClientCausedException {

    private final String runningPlayerUsername;

    public NotRunningPlayerException(String runningPlayerUsername) {
        super("This is not your turn.");
        this.runningPlayerUsername = runningPlayerUsername;
    }

    @Override
    public String getDescription() {
        return "Wait for your turn to add a move.";
    }

    @Override
    public String getSuggestion() {
        return "The running player is " + runningPlayerUsername + ". He is the only allowed to perform any action.";
    }
}
