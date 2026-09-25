package com.matteopaciolla.prbe.exceptions.match;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class JoiningAlreadyStartedMatchException extends BaseClientCausedException {

    private final String keyCode;

    public JoiningAlreadyStartedMatchException(String keyCode) {
        super("Trying to join a match that has already started");
        this.keyCode = keyCode;
    }

    @Override
    public String getDescription() {
        return String.format("Match with keyCode %s has already started", keyCode);
    }
}
