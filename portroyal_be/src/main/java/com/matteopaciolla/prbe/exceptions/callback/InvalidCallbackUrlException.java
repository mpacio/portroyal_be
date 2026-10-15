package com.matteopaciolla.prbe.exceptions.callback;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class InvalidCallbackUrlException extends BaseClientCausedException {

    public InvalidCallbackUrlException() {
        super("The callback URL is invalid or resolves to a disallowed network address.");
    }
}
