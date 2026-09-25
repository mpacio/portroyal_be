package com.matteopaciolla.prbe.exceptions.match;

import com.matteopaciolla.prbe.exceptions.common.UniqueConstraintViolatedException;
import lombok.Getter;

@Getter
public class MultipleHostingDemandException extends UniqueConstraintViolatedException {

    private final String keyCode;
    private final String username;

    public MultipleHostingDemandException(String keyCode, String username) {
        super("Multiple hosting demand for the user " + username);
        this.keyCode = keyCode;
        this.username = username;
    }

    @Override
    public String getSuggestion() {
        return String.format("User %s should close the hosted match with keyCode \"%s\" before trying to host a new one", username, keyCode);
    }
}


