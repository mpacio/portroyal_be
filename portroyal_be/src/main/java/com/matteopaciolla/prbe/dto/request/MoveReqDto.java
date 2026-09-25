package com.matteopaciolla.prbe.dto.request;

import com.matteopaciolla.portroyal.core.enums.MoveAction;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.prbe.annotation.EnumStringList;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import com.matteopaciolla.prbe.annotation.EnumString;
import lombok.Value;

import java.util.List;

@Schema(title = "Move insert body", description = "Request to make a move in the game")
@Value
public class MoveReqDto {

    @Schema(example = "DISCOVER", description = "The move to make")
    @NotNull
    @EnumString(enumClass=MoveAction.class, ignoreCase=true)
    String move;
    @Schema(example = "0", description = "The index of the card on which the move is made")
    Integer parameterIndex;
    @Schema(example = "-1", description = "The index of the player on which the move is made")
    Integer pickPlayerIndex;
    @Schema(example = "[\"CAPTAIN\", \"CAPTAIN\", \"SETTLER\"]", description = "The list of employees needed for the expedition (only for COMMIT_EXPEDITION)")
    @EnumStringList(enumClass= ExpeditionEmployee.class)
    List<String> expeditionEmployeesList;
}
