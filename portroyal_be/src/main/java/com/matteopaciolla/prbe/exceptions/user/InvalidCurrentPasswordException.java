package com.matteopaciolla.prbe.exceptions.user;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class InvalidCurrentPasswordException extends BaseClientCausedException {

    public InvalidCurrentPasswordException() {
        super("The current password is incorrect");
    }
}
