package com.matteopaciolla.prbe.exceptions.match;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

/**
 * Thrown when trying to change a match's player composition (e.g. add/remove a bot player) after
 * the match has already started.
 */
public class MatchCompositionLockedException extends BaseClientCausedException {

    private final String keyCode;

    public MatchCompositionLockedException(String keyCode) {
        super("Match composition can no longer be changed");
        this.keyCode = keyCode;
    }

    @Override
    public String getDescription() {
        return String.format("Match with keyCode %s has already started, its players can no longer be changed", keyCode);
    }
}
