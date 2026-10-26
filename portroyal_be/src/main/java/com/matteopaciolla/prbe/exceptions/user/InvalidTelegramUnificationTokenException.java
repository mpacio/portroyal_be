package com.matteopaciolla.prbe.exceptions.user;

import com.matteopaciolla.prbe.exceptions.common.ResourceNotFoundException;

public class InvalidTelegramUnificationTokenException extends ResourceNotFoundException {

    public InvalidTelegramUnificationTokenException() {
        super("Telegram account unification could not be completed");
    }
}
