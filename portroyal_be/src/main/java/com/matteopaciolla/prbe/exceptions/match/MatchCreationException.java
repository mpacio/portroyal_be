package com.matteopaciolla.prbe.exceptions.match;

import com.matteopaciolla.prbe.exceptions.BaseInternalException;

public class MatchCreationException extends BaseInternalException {

    private final Throwable throwable;

    public MatchCreationException(Throwable cause) {
        super("Error while creating match");
        this.throwable = cause;
    }

    @Override
    public String getDescription() {
        return "An error occurred while trying to create a match: " + throwable.getMessage();
    }
}
