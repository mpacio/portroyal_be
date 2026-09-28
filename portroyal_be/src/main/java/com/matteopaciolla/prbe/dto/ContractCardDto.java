package com.matteopaciolla.prbe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(name = "ContractCard", description = "Contract card definition exposed by the game catalog.")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractCardDto {

    @Schema(description = "Unique id of the contract card in the game catalog.", example = "101")
    private Integer id;

    @Schema(description = "Category of the contract card", example = "AUTOMATIC")
    private ContractCardType type;

    @Schema(description = "Graphic icon associated with the card.", example = "📜")
    private String icon;

    @Schema(description = "Display name of the contract card.", example = "Trade Master")
    private String name;

    @Schema(description = "Rule description for the contract.")
    private String description;

    @Schema(description = "First profession associated with the contract; applicable when type is MANUAL_TWO_PROFESSIONS.", example = "Captain")
    private String profession1;

    @Schema(description = "Second profession associated with the contract; applicable when type is MANUAL_TWO_PROFESSIONS.", example = "Mademoiselle")
    private String profession2;

    @Schema(description = "First profession park. Every element in the list is the name of the player that has fulfilled the requirement of the contract card. Applicable when the contract card requires multiple iterations of the requirement to be fulfilled.")
    private List<String> park1;

    @Schema(description = "Second profession park. Every element in the list is the name of the player that has fulfilled the requirement of the contract card. Applicable when the contract card requires multiple iterations of the requirement to be fulfilled.")
    private List<String> park2;

    @Schema(description = "Available spaces or slots exposed by the contract card. Every element in the list is the name of the player that has fulfilled the requirement of the contract card.")
    private List<String> spots;

    @Schema(description = "Reward values granted by the contract card, one for each possible player that can fulfill the requirement of the contract card. The length of the array will always be equal to the maximum number of players in the game (5).", example = "[3, 2, 1, 0, 0]")
    private int[] rewards;

    public enum ContractCardType {
        AUTOMATIC,
        MANUAL,
        AUTOMATIC_RENOUNCING,
        MANUAL_TWO_PROFESSIONS;

        public static ContractCardType fromClass(ContractCard classType) {
            return switch (classType) {
                case RenouncingContractCard ignored -> AUTOMATIC_RENOUNCING;
                case TwoProfessionsContractCard ignored -> MANUAL_TWO_PROFESSIONS;
                case AutomaticContractCard ignored -> AUTOMATIC;
                case ManualContractCard ignored -> MANUAL;
                default -> throw new IllegalArgumentException("Invalid contract card type: " + classType.getClass().getSimpleName());
            };
        }
    }
}
