package com.matteopaciolla.prbe.exceptions.game;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class NotSinglePlayerMatchException extends BaseClientCausedException {

    private String keyCode;

    public NotSinglePlayerMatchException() {
        super("This match is not a single player match.");
    }

    public NotSinglePlayerMatchException(@NotNull String keyCode) {
        this();
        this.keyCode = keyCode;
    }

    @Override
    public String getDescription() {
        return "You cannot start this match until at least one other player joins.";
    }

    @Override
    public String getSuggestion() {
        return "Wait for the other player to join the match before starting it.";
    }

    @Override
    public Map<String, String> getDetails() {
        return Map.of("keyCode", keyCode);
    }
}
