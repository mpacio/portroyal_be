package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Schema(name = "CallbackSentinelRequest", description = "Payload used to register an HTTP callback for match notifications.")
@Value
public class CallbackSentinelRequest {

    @Schema(description = "Webhook URL that will receive the alert payload.", example = "https://example.com/callback", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "url must not be blank")
    String url;

    @Schema(description = "Optional secret used to validate or sign callback requests.", example = "my-secret")
    String secret;
}
