package com.matteopaciolla.prbe.dto.request;

import com.matteopaciolla.portroyal.core.enums.BotDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(name = "AddAiPlayerRequest", description = "Request payload used by the host to add an autonomous AI player to a not-yet-started match.")
@Data
public class AddAiPlayerReqDto {

    @NotNull
    @Schema(description = "Difficulty level the backend uses to compute this AI player's moves.", example = "MEDIUM", requiredMode = Schema.RequiredMode.REQUIRED)
    private BotDifficulty difficulty;

    @Schema(description = "Optional display name for the AI player. When omitted, a default name is generated.", example = "Rocco the Pirate")
    private String name;
}
