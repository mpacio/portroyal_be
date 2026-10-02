package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ManualContractCard;

public class Explorer extends ManualContractCard {

    public Explorer(int id) {
        super(id);
    }

    @Override
    public boolean requirementsMet(Player player) {
        return !player.getExpeditionCards().isEmpty();
    }

    @Override
    public String getName() {
        return "Explorer";
    }

    @Override
    public String getDescription() {
        return "If you have at least 1 Expedition in your personal display, " +
                "you may immediately sign this Contract.";
    }

    @Override
    public int[] getRewards() {
        return new int[]{2, 1, 0, 0, 0};
    }
}
