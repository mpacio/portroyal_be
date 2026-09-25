package com.matteopaciolla.prbe.exceptions.game;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class EndedMatchException extends BaseClientCausedException {

    public EndedMatchException() {
        super("The match has already ended.");
    }

    @Override
    public String getDescription() {
        return "You can't perform any action on a match that has already ended.";
    }

    @Override
    public String getSuggestion() {
        return "You can check the match results and host a new match.";
    }
}
