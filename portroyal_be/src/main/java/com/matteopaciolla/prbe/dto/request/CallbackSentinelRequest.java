package com.matteopaciolla.prbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class CallbackSentinelRequest {

    @NotBlank(message = "matchKeyCode must not be blank")
    String matchKeyCode;
    @NotBlank(message = "url must not be blank")
    String url;
    String secret;
}
