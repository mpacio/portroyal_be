package com.matteopaciolla.prbe.exceptions.common;

import com.matteopaciolla.prbe.exceptions.BaseClientCausedException;

public class ResourceNotFoundException extends BaseClientCausedException {

    private final String description;

    public ResourceNotFoundException(String description) {
        super("Resource not found");
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description != null ? description : "The requested resource could not be found.";
    }
}
