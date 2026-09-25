package com.matteopaciolla.prbe.controller;

import com.matteopaciolla.prbe.constants.Paths;
import com.matteopaciolla.prbe.dto.request.CallbackSentinelRequest;
import com.matteopaciolla.prbe.dto.AlertDto;
import com.matteopaciolla.prbe.dto.response.VoidResponse;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.service.SentinelService;
import com.matteopaciolla.prbe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Sentinel", description = "Notification subscription APIs")
@SecurityRequirements({@SecurityRequirement(name = "basicAuth")})
@Slf4j
@RestController
@RequestMapping(Paths.SENTINEL_PATH)
public class SentinelController {

    @Autowired
    private SentinelService sentinelService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Subscribe to a callback notification", description = """
Subscribe to a callback notification.
The callback will be sent to the URL specified in the request.
The callback will be sent when a new alert is available.
The alert will be sent in the request body.
The alert will be sent only once.
The alerts refer to any match, that's why the matchKeyCode is required.""")
    @PostMapping(path = "/callback/subscribe", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VoidResponse> addCallbackSentinel(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Callback request body", required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CallbackSentinelRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"matchKeyCode\":\"xxxxxx\",\"url\":\"http://localhost:8080/callback\"}")))
            @Valid @RequestBody CallbackSentinelRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userService.getUserEntityByUsername(username);
        return ResponseEntity.ok(new VoidResponse(sentinelService.addCallbackSentinel(user.getId(), request.getMatchKeyCode(), request.getUrl(), request.getSecret())));
    }

    @Operation(summary = "Subscribe to a long polling notification", description = """
Subscribe to a long polling notification.
The alert will be sent in the response body.
The alert will be sent only once.
The alerts refer to any match, that's why the matchKeyCode is required.
The alert will be sent only if the alert is available before the timeout.""")
    @GetMapping(path = "/long-polling/subscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<AlertDto> addLongPollingSentinel(
            @Valid @NotBlank(message = "keyCode must not be blank") @RequestParam String keyCode,
            @Valid @Min(value = 10, message = "seconds must be greater or equal to 10") @RequestParam(defaultValue = "120") int seconds) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userService.getUserEntityByUsername(username);
        return sentinelService.addLongPollingSentinel(user, keyCode, seconds);
    }

    @Operation(summary = "Subscribe to a Server-Sent Events notification", description = """
Subscribe to a Server-Sent Events notification.
The alert will be sent in the response body.
The alert will be sent every time a new alert is available.
The alerts refer to any match, that's why the matchKeyCode is required.
The alert will be sent only if the alert is available before the timeout.""",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE subscribed",
                            content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "text/event-stream",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AlertDto.class))),})
    @GetMapping(path = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter addSSESentinel(
            @Valid @NotBlank(message = "keyCode must not be blank") @RequestParam(value = "keyCode") String keyCode,
            @Valid @Min(value = 1, message = "seconds must be greater or equal to 1") @RequestParam(value = "seconds", defaultValue = "3600") int seconds) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userService.getUserEntityByUsername(username);
        return sentinelService.addSSESentinel(user, keyCode, seconds * 1000L);
    }

    @Operation(summary = "Remove a subscription", description = """
Remove a subscription.
The subscription will be removed from the user's subscriptions.
The subscription will not be available anymore.""")
    @DeleteMapping(path = "/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VoidResponse> removeSentinelSubscription(
            @Valid @NotBlank(message = "keyCode must not be blank") @RequestParam(value = "keyCode") String keyCode) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userService.getUserEntityByUsername(username);
        sentinelService.removeSentinelSubscription(user.getId(), keyCode);
        return ResponseEntity.ok(new VoidResponse("Subscription removed"));
    }
}
