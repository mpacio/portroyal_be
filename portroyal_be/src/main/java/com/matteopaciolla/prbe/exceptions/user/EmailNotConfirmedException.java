package com.matteopaciolla.prbe.exceptions.user;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class EmailNotConfirmedException extends BaseClientCausedException {

    public EmailNotConfirmedException() {
        super("Email not confirmed");
    }

    @Override
    public String getDescription() {
        return "The email address must be confirmed before performing this action.";
    }

    @Override
    public String getSuggestion() {
        return "Please confirm your email address.";
    }
}
