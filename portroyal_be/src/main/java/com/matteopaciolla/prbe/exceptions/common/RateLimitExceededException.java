package com.matteopaciolla.prbe.exceptions.common;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds) {
        super("Request rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
