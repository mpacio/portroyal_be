package com.matteopaciolla.portroyal.confs;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@Getter
@EqualsAndHashCode
public class Configuration {

    @Builder.Default
    private int initialCoins = 3;
    @Builder.Default
    private boolean firstPlayerRandomlyChosen = false;
    @Builder.Default
    private int taxedMoney = 12;
    @Builder.Default
    private int taxRate = 2;
    @Builder.Default
    private int bigExpeditionMinimumPlayersNumber = 5;

    // expansions configurations
    @Builder.Default
    private boolean JOMC_ExpansionUsed = true;
    @Builder.Default
    private boolean cargoCoinToPoorestPlayer = false;
    @Builder.Default
    private int contractsCardNumber = 4;
    @Builder.Default
    private int maxContractsCompletablePerPlayer = 3;
}
