package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.AutomaticContractCard;

public class MajorSpeculator extends AutomaticContractCard {

    public MajorSpeculator(int id) {
        super(id);
    }

    @Override
    public boolean requirementsMet(Player player) {
        return player.isMajorSpeculator();
    }

    @Override
    public String getName() {
        return "Major Speculator";
    }

    @Override
    public String getDescription() {
        return "If you are the active player and there are 4 Ships of different colors in the harbor display, " +
                "you may immediately place one of your markers to complete this Contract.";
    }

    @Override
    public int[] getRewards() {
        return new int[]{2, 1, 0, 0, 0};
    }
}
