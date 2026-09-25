package com.matteopaciolla.prbe.exceptions;

import java.util.Map;

public class BaseClientCausedException extends RuntimeException {

        public BaseClientCausedException(String message) {
            super(message);
        }

        public BaseClientCausedException(String message, Throwable cause) {
            super(message, cause);
        }

        public BaseClientCausedException(Throwable cause) {
            super(cause);
        }

        public BaseClientCausedException() {
            super();
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
