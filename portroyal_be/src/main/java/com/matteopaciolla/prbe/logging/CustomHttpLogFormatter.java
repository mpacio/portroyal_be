package com.matteopaciolla.prbe.logging;

import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zalando.logbook.*;

import java.io.IOException;
import java.util.List;

public class CustomHttpLogFormatter implements HttpLogFormatter {

    private static final String requestPattern = "Request: %s %s by %s";
    private static final String anonymousUser = "anonymous";

    @Override
    public String format(@NonNull Precorrelation precorrelation, HttpRequest request) throws IOException {
        String res = String.format(requestPattern,
                request.getMethod(),
                request.getPath(),
                getAuthenticatedUser());
        return res;
    }

    @Override
    public String format(@NonNull Correlation correlation, HttpResponse response) throws IOException {
        return String.format("Response: %d to %s",
                response.getStatus(),
                getAuthenticatedUser());
    }

    private String getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null) ? authentication.getName() : anonymousUser;
    }
}
