package com.matteopaciolla.prbe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "MatchConfigRequest", description = "Optional game configuration used when creating or hosting a match.")
@Data
public class MatchConfigReqDto {

    @Schema(description = "Configuration identifier to load from the available preset list.", example = "1")
    private Integer id;

    @Schema(description = "Human-readable configuration name.", example = "default")
    private String name;

    @Schema(description = "Initial coin budget assigned to each player.", example = "6")
    private Integer initialCoins;

    @Schema(description = "Whether the first player should be selected randomly.", example = "true")
    private Boolean firstPlayerRandomlyChosen;

    @Schema(description = "Money amount collected via tax cards.", example = "3")
    private Integer taxedMoney;

    @Schema(description = "Tax rate applied by the game configuration.", example = "1")
    private Integer taxRate;

    @Schema(description = "Minimum number of players needed for the big expedition to be available in the deck.", example = "5")
    private Integer bigExpeditionMinimumPlayersNumber;

    @Schema(description = "Whether \"Just One More Contract\" expansion deck is enabled.", example = "false")
    private Boolean JOMC_ExpansionUsed;

    @Schema(description = "Whether extra cargo coins are automatically redistributed to the poorest player.", example = "true")
    private Boolean cargoCoinToPoorestPlayer;

    @Schema(description = "Number of contract cards available in the table configuration.", example = "5")
    private Integer contractsCardNumber;

    @Schema(description = "Maximum number of contracts a single player can complete.", example = "3")
    private Integer maxContractsCompletablePerPlayer;
}
