package com.matteopaciolla.prbe.constants.enums;

public enum UserRole {
    ADMIN,
    USER,
    BOT,
    /**
     * Autonomous, per-match player operated by the backend (not a real account, never
     * authenticates). Distinct from {@link #BOT}, which identifies the single technical Telegram
     * proxy account. AI players carry a {@code botDifficulty} on their {@code UserEntity}.
     */
    AI;

    @Override
    public String toString() {
        return "ROLE_" + super.toString();
    }
}
