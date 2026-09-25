package com.matteopaciolla.prbe.exceptions.user;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class EmailAlreadyConfirmedException extends BaseClientCausedException {

    public EmailAlreadyConfirmedException() {
        super("Email has already been confirmed");
    }
}
