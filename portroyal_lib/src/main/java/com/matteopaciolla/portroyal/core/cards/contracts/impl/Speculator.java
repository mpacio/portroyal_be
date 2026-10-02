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
        return "When you are the active player and there are 3 Ships of different colors in the harbor display, " +
                "you are considered to have started signing this Contract, reducing by 1 the maximum number of Contracts you may sign. " +
                "The next time this condition is met while you are the active player, you automatically sign the Contract. " +
                "Once you start signing this Contract, that commitment remains for the rest of the game, even if you do not complete it by meeting this condition again. " +
                "Multiple players can start signing this Contract at the same time.";
    }

    @Override
    public int[] getRewards() {
        return new int[]{2, 1, 0, 0, 0};
    }
}
