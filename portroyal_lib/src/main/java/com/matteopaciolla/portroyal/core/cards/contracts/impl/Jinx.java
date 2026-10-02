package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.AutomaticContractCard;

public class Jinx extends AutomaticContractCard {

    public Jinx(int id) {
        super(id);
    }

    @Override
    public boolean requirementsMet(Player player) {
        return player.isWentBust();
    }

    @Override
    public String getName() {
        return "Jinx";
    }

    @Override
    public String getDescription() {
        return "While this Contract is active in the match, if you must end your turn as the active player due to drawing a Ship " +
                "of the same color as another already in the harbor display (going bust), you automatically sign it.";
    }

    @Override
    public int[] getRewards() {
        return new int[]{1, 1, 1, 1, 1};
    }
}
