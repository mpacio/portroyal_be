package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ManualContractCard;

public class Mercenary extends ManualContractCard {

    public Mercenary(int id) {
        super(id);
    }

    @Override
    public boolean requirementsMet(Player player) {
        return player.getPowerValue() >= 3;
    }

    @Override
    public String getName() {
        return "Mercenary";
    }

    @Override
    public String getDescription() {
        return "If you have at least 3 Cutlasses in your personal display, " +
                "you may immediately place one of your markers to complete this Contract.";
    }

    @Override
    public int[] getRewards() {
        return new int[]{3, 2, 1, 0, 0};
    }
}
