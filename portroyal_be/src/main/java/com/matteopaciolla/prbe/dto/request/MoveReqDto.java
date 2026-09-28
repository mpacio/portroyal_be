package com.matteopaciolla.prbe.dto.request;

import com.matteopaciolla.portroyal.core.enums.MoveAction;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.prbe.annotation.EnumStringList;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import com.matteopaciolla.prbe.annotation.EnumString;
import lombok.Value;

import java.util.List;

@Schema(title = "MoveRequest", description = "Payload sent to execute a game move. The move type determines which optional fields are meaningful.")
@Value
public class MoveReqDto {

    @Schema(example = "DISCOVER", description = "Name of the move to execute. Examples include DISCOVER, HIRE, TRADE, or COMMIT_EXPEDITION.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    @EnumString(enumClass=MoveAction.class, ignoreCase=true)
    String move;

    @Schema(example = "0", description = "Index of the target card or element involved in the move, when the move requires one.")
    Integer parameterIndex;

    @Schema(example = "-1", description = "Index of the target player involved in the move; use -1 when not applicable.")
    Integer pickPlayerIndex;

    @Schema(example = "[\"CAPTAIN\", \"CAPTAIN\", \"SETTLER\"]", description = "Employee list used only for COMMIT_EXPEDITION or other expedition-related moves.")
    @EnumStringList(enumClass= ExpeditionEmployee.class)
    List<String> expeditionEmployeesList;
}
