package com.matteopaciolla.prbe.exceptions.match;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
import lombok.Getter;

@Getter
public class MultipleMatchJoinAttemptException extends BaseClientCausedException {

    private final String keyCode;
    private final String username;

    public MultipleMatchJoinAttemptException(String keyCode, String username) {
        super("User already in a match");
        this.keyCode = keyCode;
        this.username = username;
    }

    @Override
    public String getDescription() {
        return String.format("User %s is already in a match with keyCode %s.", username, keyCode);
    }
}


