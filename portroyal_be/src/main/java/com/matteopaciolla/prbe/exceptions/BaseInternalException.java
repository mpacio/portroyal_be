package com.matteopaciolla.prbe.exceptions;

import java.util.Map;

public class BaseInternalException extends RuntimeException {

        public BaseInternalException(String message) {
            super(message);
        }

        public String getDescription() {
            return null;
        }

        public String getSuggestion() {
            return null;
        }

        public Map<String, String> getDetails() {
            return null;
        }
}
