package com.matteopaciolla.prbe.logging;

import lombok.NonNull;
import org.zalando.logbook.*;

import java.io.IOException;

public class CustomHttpLogFormatter implements HttpLogFormatter {

    @Override
    public String format(@NonNull Precorrelation precorrelation, HttpRequest request) throws IOException {
        return String.format("Request: %s %s", request.getMethod(), request.getPath());
    }

    @Override
    public String format(@NonNull Correlation correlation, HttpResponse response) throws IOException {
        return String.format("Response: %d", response.getStatus());
    }
}
