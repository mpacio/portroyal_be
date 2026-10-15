package com.matteopaciolla.prbe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.prbe.constants.enums.SentinelAlertMessage;
import com.matteopaciolla.prbe.exceptions.callback.InvalidCallbackUrlException;
import com.matteopaciolla.prbe.model.entity.CallbackEntity;
import com.matteopaciolla.prbe.model.entity.CallbackEntityId;
import com.matteopaciolla.prbe.model.entity.CallbackSecretEntity;
import com.matteopaciolla.prbe.repository.CallbackRepository;
import com.matteopaciolla.prbe.repository.CallbackSecretRepository;
import com.matteopaciolla.prbe.repository.MatchRepository;
import com.matteopaciolla.prbe.security.callback.CallbackUrlValidator;
import com.matteopaciolla.prbe.security.callback.CallbackSecretCrypto;
import com.matteopaciolla.prbe.service.SentinelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SentinelCallbackSecurityTest {

    private static final String CALLBACK_URL = "https://hooks.example.com/port-royal";
    private static final String SHARED_SECRET = "callback-test-secret-with-at-least-32-bytes";
    private static final Long USER_ID = 12L;
    private static final String MATCH_KEY = "ABC123";

    @Mock
    private CallbackRepository callbackRepository;
    @Mock
    private CallbackSecretRepository callbackSecretRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private CallbackUrlValidator callbackUrlValidator;
    private final CallbackSecretCrypto callbackSecretCrypto = new CallbackSecretCrypto(
            Base64.getEncoder().encodeToString(
                    "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII)),
            "test-key",
            "");

    private RestTemplate restTemplate;
    private SentinelService sentinelService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        sentinelService = new SentinelService(
                callbackRepository,
                callbackSecretRepository,
                matchRepository,
                restTemplate,
                new ObjectMapper(),
                callbackUrlValidator,
                callbackSecretCrypto);
    }

    @Test
    void registrationValidatesUrlBeforePersistingSubscription() {
        doThrow(new InvalidCallbackUrlException()).when(callbackUrlValidator).validate("http://localhost/callback");

        assertThatThrownBy(() -> sentinelService.addCallbackSentinel(
                USER_ID, MATCH_KEY, "http://localhost/callback", SHARED_SECRET))
                .isInstanceOf(InvalidCallbackUrlException.class);

        verifyNoInteractions(matchRepository, callbackRepository, callbackSecretRepository);
    }

    @Test
    void dispatchSignsExactJsonBodyAndDoesNotIncludeSecretInPayload() throws Exception {
        CallbackEntity callback = new CallbackEntity(MATCH_KEY, USER_ID, CALLBACK_URL, null);
        CallbackEntityId callbackId = new CallbackEntityId(MATCH_KEY, USER_ID);
        when(callbackRepository.findByMatchKeyCode(MATCH_KEY)).thenReturn(List.of(callback));
        when(callbackSecretRepository.findById(callbackId)).thenReturn(Optional.of(
                new CallbackSecretEntity(MATCH_KEY, USER_ID, callbackSecretCrypto.encrypt(SHARED_SECRET))));

        AtomicReference<byte[]> sentBody = new AtomicReference<>();
        AtomicReference<String> sentTimestamp = new AtomicReference<>();
        AtomicReference<String> sentEventId = new AtomicReference<>();
        AtomicReference<String> sentSignature = new AtomicReference<>();

        when(restTemplate.postForEntity(eq(CALLBACK_URL), any(HttpEntity.class), eq(Void.class)))
                .thenAnswer(invocation -> {
                    HttpEntity<byte[]> request = invocation.getArgument(1);
                    assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
                    sentTimestamp.set(request.getHeaders().getFirst("X-Callback-Timestamp"));
                    sentEventId.set(request.getHeaders().getFirst("X-Callback-Event-Id"));
                    sentSignature.set(request.getHeaders().getFirst("X-Callback-Signature"));
                    sentBody.set(request.getBody());
                    return org.springframework.http.ResponseEntity.ok().build();
                });

        sentinelService.sendUpdate(MATCH_KEY, "alice", SentinelAlertMessage.MATCH_STARTED);
        verify(restTemplate).postForEntity(eq(CALLBACK_URL), any(HttpEntity.class), eq(Void.class));

        byte[] body = sentBody.get();
        String timestamp = sentTimestamp.get();
        String eventId = sentEventId.get();
        String signature = sentSignature.get();
        assertThat(timestamp).matches("\\d+");
        assertThat(Long.parseLong(timestamp)).isBetween(
                Instant.now().getEpochSecond() - 10, Instant.now().getEpochSecond() + 10);
        assertThat(eventId).isNotBlank();
        assertThat(signature).matches("v1=[0-9a-f]{64}");
        assertThat(signature).isEqualTo(independentlyCalculateSignature(
                timestamp, eventId, body, SHARED_SECRET));
        assertThat(new String(body, StandardCharsets.UTF_8)).doesNotContain(SHARED_SECRET);

        JsonNode payload = new ObjectMapper().readTree(body);
        assertThat(payload.has("secret")).isFalse();
        verify(callbackUrlValidator).validate(CALLBACK_URL);
    }

    @Test
    void dispatchRevalidatesUrlAndSkipsUnsafeDestination() {
        CallbackEntity callback = new CallbackEntity(MATCH_KEY, USER_ID, CALLBACK_URL, null);
        when(callbackRepository.findByMatchKeyCode(MATCH_KEY)).thenReturn(List.of(callback));
        doThrow(new InvalidCallbackUrlException()).when(callbackUrlValidator).validate(CALLBACK_URL);

        sentinelService.sendUpdate(MATCH_KEY, "alice", SentinelAlertMessage.MATCH_STARTED);

        verifyNoInteractions(restTemplate, callbackSecretRepository);
        verify(callbackUrlValidator).validate(CALLBACK_URL);
    }

    private static String independentlyCalculateSignature(
            String timestamp, String eventId, byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update(timestamp.getBytes(StandardCharsets.US_ASCII));
        mac.update((byte) '.');
        mac.update(eventId.getBytes(StandardCharsets.US_ASCII));
        mac.update((byte) '.');
        mac.update(body);
        return "v1=" + HexFormat.of().formatHex(mac.doFinal());
    }
}
