package com.matteopaciolla.prbe.exceptions.game;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class MoveExecutionException extends BaseClientCausedException {

    private String description;

    public MoveExecutionException() {
        super("Move execution failed");
    }

    public MoveExecutionException(Throwable cause) {
        this();
        this.description = cause.getMessage();
    }

    @Override
    public String getDescription() {
        return description != null ? description : "An error occurred while executing the move.";
    }
}
