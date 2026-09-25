package com.matteopaciolla.prbe.exceptions.match;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.Map;

@Getter
public class MultipleMatchStartAttemptException extends BaseClientCausedException {

    String keyCode;

    public MultipleMatchStartAttemptException() {
        super("Match has already started");
    }

    public MultipleMatchStartAttemptException(@NotNull String keyCode) {
        this();
        this.keyCode = keyCode;
    }

    @Override
    public String getDescription() {
        return "Time to insert moves";
    }

    @Override
    public Map<String, String> getDetails() {
        return Map.of("keyCode", keyCode);
    }
}


