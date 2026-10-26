package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Schema(name = "CallbackSentinelRequest", description = "Payload used to register an HTTPS callback. Production callbacks must resolve to publicly routable addresses; localhost and private addresses are available only in dev/test.")
@Value
public class CallbackSentinelRequest {

    @Schema(description = "Match key code that the callback is tied to.", example = "ABC123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "matchKeyCode must not be blank")
    @Size(max = 255, message = "matchKeyCode must not exceed 255 characters")
    String matchKeyCode;

    @Schema(description = "Webhook URL that will receive the alert payload.", example = "https://example.com/callback", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "url must not be blank")
    @Size(max = 255, message = "url must not exceed 255 characters")
    String url;

    @Schema(description = "Optional secret used to validate or sign callback requests.", example = "my-secret")
    @Size(max = 255, message = "secret must not exceed 255 characters")
    String secret;
}
