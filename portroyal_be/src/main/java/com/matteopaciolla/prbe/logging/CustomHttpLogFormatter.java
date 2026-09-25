package com.matteopaciolla.prbe.logging;

import com.matteopaciolla.prbe.constants.CommonConstants;
import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zalando.logbook.*;

import java.io.IOException;
import java.util.List;

public class CustomHttpLogFormatter implements HttpLogFormatter {

    private static final String requestPattern = "Request: %s %s by %s -> Params=[%s] Headers: " + CommonConstants.BOT_MANDATORY_HEADER + "=%s, cookie=%s";
    private static final String anonymousUser = "anonymous";

    @Override
    public String format(@NonNull Precorrelation precorrelation, HttpRequest request) throws IOException {
        String res = String.format(requestPattern,
                request.getMethod(),
                request.getPath(),
                getAuthenticatedUser(),
                request.getQuery(),
                request.getHeaders().get(CommonConstants.BOT_MANDATORY_HEADER),
                request.getHeaders().get("cookie"));
        if (request.getBodyAsString() != null && !request.getBodyAsString().isEmpty()) {
            res += String.format(" Body: %s", request.getBodyAsString());}
        return res;
    }

    @Override
    public String format(@NonNull Correlation correlation, HttpResponse response) throws IOException {
        String res = String.format("Response: %d to %s -> Headers: Set-Cookie=%s",
                response.getStatus(),
                getAuthenticatedUser(),
                response.getHeaders().get("Set-Cookie"));
        if (response.getBodyAsString() != null && !response.getBodyAsString().isEmpty()) {
            if (response.getBodyAsString().startsWith("<!DOCTYPE html>")) {
                // avoid logging the body of html responses
                res += " [HTML response]";
            } else {
                res += String.format(" Body: %s", response.getBodyAsString());
            }
        }
        return res;
    }

    private String getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null) ? authentication.getName() : anonymousUser;
    }
}
