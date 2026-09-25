package com.matteopaciolla.prbe.constants.enums;

public enum UserRole {
    ADMIN,
    USER,
    BOT;

    @Override
    public String toString() {
        return "ROLE_" + super.toString();
    }
}
