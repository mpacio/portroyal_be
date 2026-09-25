package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.AutomaticContractCard;

public class Speculator extends AutomaticContractCard {

    public Speculator(int id) {
        super(id);
    }

    @Override
    public boolean requirementsMet(Player player) {
        return player.getMinorSpeculator() > 1;
    }

    @Override
    public String getName() {
        return "Speculator";
    }

    @Override
    public String getDescription() {
        return "If you are the active player and there are 3 Ships of different colors in the harbor display, " +
                "you may immediately decide whether or not to place one of your markers on space [1]. " +
                "Once you place a marker on this Contract it remains there for the rest of the game, " +
                "even if you don’t complete it. Multiple players can have markers on these spaces. " +
                "The next time you are the active player and have 3 Ships of different colors " +
                "in the harbor display, you immediately complete the Contract.";
    }

    @Override
    public int[] getRewards() {
        return new int[]{2, 1, 0, 0, 0};
    }
}
