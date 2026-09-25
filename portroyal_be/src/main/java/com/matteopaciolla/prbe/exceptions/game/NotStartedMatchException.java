package com.matteopaciolla.prbe.exceptions.game;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class NotStartedMatchException extends BaseClientCausedException {

    public NotStartedMatchException() {
        super("The match has not started yet.");
    }

    @Override
    public String getDescription() {
        return "You cannot perform any action until the match has started.";
    }

    @Override
    public String getSuggestion() {
        return "Wait for the host to start the match.";
    }
}
