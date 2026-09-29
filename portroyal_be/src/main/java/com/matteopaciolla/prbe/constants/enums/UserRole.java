package com.matteopaciolla.prbe.constants.enums;

public enum UserRole {
    ADMIN,
    USER,
    BOT,
    /**
     * Virtual role used only in API responses (e.g. {@code UserDto.roles}) to mark a match
     * participant as an autonomous AI player. Distinct from {@link #BOT}, which identifies the
     * single technical Telegram proxy account. AI players are never persisted as
     * {@code UserEntity} rows; they live in their own {@code AIPlayerEntity}/{@code ai_players}
     * table and never authenticate.
     */
    AI;

    @Override
    public String toString() {
        return "ROLE_" + super.toString();
    }
}
