package com.matteopaciolla.prbe.exceptions.common;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;
import lombok.Getter;

@Getter
public class UniqueConstraintViolatedException extends BaseClientCausedException {

    private String description = null;

    public UniqueConstraintViolatedException() {
        super("Unique constraint violated on a field");
    }

    public UniqueConstraintViolatedException(String description) {
        this();
        this.description = description;
    }

    @Override
    public String getDescription() {
        if (this.description != null) {
            return this.description;
        } else {
            return super.getDescription();
        }
    }

}


