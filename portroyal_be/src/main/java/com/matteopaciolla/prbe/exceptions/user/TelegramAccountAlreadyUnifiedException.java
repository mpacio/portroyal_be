package com.matteopaciolla.prbe.exceptions.user;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class TelegramAccountAlreadyUnifiedException extends BaseClientCausedException {

    private String description;

    public TelegramAccountAlreadyUnifiedException() {
        super("Telegram account is already unified");
    }

    public TelegramAccountAlreadyUnifiedException(String description) {
        this();
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description != null ? description : "The Telegram account is already unified with a user account.";
    }
}
