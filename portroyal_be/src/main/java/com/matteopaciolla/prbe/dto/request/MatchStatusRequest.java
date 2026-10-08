package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
@Schema(description = "Lifecycle status to apply to the authenticated user's current match.")
public class MatchStatusRequest {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "STARTED")
    Status status;

    public enum Status {
        STARTED,
        CLOSED
    }
}
