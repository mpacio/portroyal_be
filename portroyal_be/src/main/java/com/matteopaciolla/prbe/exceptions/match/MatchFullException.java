package com.matteopaciolla.prbe.exceptions.match;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class MatchFullException extends BaseClientCausedException {

    private final String keyCode;

    public MatchFullException(String keyCode) {
        super("Match is full");
        this.keyCode = keyCode;
    }

    @Override
    public String getDescription() {
        return String.format("Match with keyCode %s is full", keyCode);
    }
}
