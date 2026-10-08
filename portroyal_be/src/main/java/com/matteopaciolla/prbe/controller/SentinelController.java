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
The match key is included in the resource path.""")
    @PostMapping(path = Paths.MATCH_PATH + "/{keyCode}/subscriptions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VoidResponse> addCallbackSentinel(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Callback request body", required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CallbackSentinelRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = "{\"url\":\"http://localhost:8080/callback\"}")))
            @Valid @RequestBody CallbackSentinelRequest request,
            @PathVariable String keyCode) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userService.getUserEntityByUsername(username);
        return ResponseEntity.status(201).body(new VoidResponse(201,
                sentinelService.addCallbackSentinel(user.getId(), keyCode, request.getUrl(), request.getSecret())));
    }

    @Operation(summary = "Subscribe to a long polling notification", description = """
Subscribe to a long polling notification.
The alert will be sent in the response body.
The alert will be sent only once.
The match key is included in the resource path.
The alert will be sent only if the alert is available before the timeout.""")
    @GetMapping(path = Paths.MATCH_PATH + "/{keyCode}/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<AlertDto> addLongPollingSentinel(
            @PathVariable String keyCode,
            @Valid @Min(value = 10, message = "seconds must be greater or equal to 10") @RequestParam(defaultValue = "120") int seconds) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userService.getUserEntityByUsername(username);
        return sentinelService.addLongPollingSentinel(user, keyCode, seconds);
    }

    @Operation(summary = "Subscribe to a Server-Sent Events notification", description = """
Subscribe to a Server-Sent Events notification.
The alert will be sent in the response body.
The alert will be sent every time a new alert is available.
The match key is included in the resource path.
The alert will be sent only if the alert is available before the timeout.""",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE subscribed",
                            content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "text/event-stream",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AlertDto.class))),})
    @GetMapping(path = Paths.MATCH_PATH + "/{keyCode}/alerts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter addSSESentinel(
            @PathVariable String keyCode,
            @Valid @Min(value = 1, message = "seconds must be greater or equal to 1") @RequestParam(value = "seconds", defaultValue = "3600") int seconds) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userService.getUserEntityByUsername(username);
        return sentinelService.addSSESentinel(user, keyCode, seconds * 1000L);
    }

    @Operation(summary = "Remove a subscription", description = """
Remove a subscription.
The subscription will be removed from the user's subscriptions.
The subscription will not be available anymore.""")
    @DeleteMapping(path = Paths.MATCH_PATH + "/{keyCode}/subscriptions/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> removeSentinelSubscription(
            @PathVariable String keyCode) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userService.getUserEntityByUsername(username);
        sentinelService.removeSentinelSubscription(user.getId(), keyCode);
        return ResponseEntity.noContent().build();
    }
}
