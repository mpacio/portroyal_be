package com.matteopaciolla.prbe.logging;

import org.junit.jupiter.api.Test;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomHttpLogFormatterTest {

    private final CustomHttpLogFormatter formatter = new CustomHttpLogFormatter();

    @Test
    void requestLogContainsOnlyMethodAndPath() throws Exception {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getPath()).thenReturn("/api/v1/public/register");

        String formatted = formatter.format(mock(Precorrelation.class), request);

        assertThat(formatted).isEqualTo("Request: POST /api/v1/public/register");
    }

    @Test
    void responseLogContainsOnlyStatus() throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getStatus()).thenReturn(200);

        String formatted = formatter.format(mock(Correlation.class), response);

        assertThat(formatted).isEqualTo("Response: 200");
    }
}
