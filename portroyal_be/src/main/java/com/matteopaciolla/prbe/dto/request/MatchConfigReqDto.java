package com.matteopaciolla.prbe.dto.request;

import lombok.Data;

@Data
public class MatchConfigReqDto {

    private Integer id;
    private String name;
    private Integer initialCoins;
    private Boolean firstPlayerRandomlyChosen;
    private Integer taxedMoney;
    private Integer taxRate;
    private Integer bigExpeditionMinimumPlayersNumber;
    private Boolean JOMC_ExpansionUsed;
    private Boolean cargoCoinToPoorestPlayer;
    private Integer contractsCardNumber;
    private Integer maxContractsCompletablePerPlayer;
}
