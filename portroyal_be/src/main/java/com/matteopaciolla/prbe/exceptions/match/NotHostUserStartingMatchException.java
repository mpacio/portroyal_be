package com.matteopaciolla.prbe.exceptions.match;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
import lombok.Getter;

@Getter
public class NotHostUserStartingMatchException extends BaseClientCausedException {

    private final String keyCode;
    private final String guestUsername;
    private final String hostUsername;

    public NotHostUserStartingMatchException(String keyCode, String unauthorizedUsername, String hostUsername) {
        super("Unauthorized user starting match");
        this.keyCode = keyCode;
        this.guestUsername = unauthorizedUsername;
        this.hostUsername = hostUsername;
    }

    @Override
    public String getDescription() {
        return String.format("Unauthorized user %s tried to start the match with keyCode %s.", guestUsername, keyCode);
    }

    @Override
    public String getSuggestion() {
        return String.format("Only %s can start the match with keyCode %s.", hostUsername, keyCode);
    }
}


