package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.AutomaticContractCard;

public class PiratesNest extends AutomaticContractCard {

            public PiratesNest(int id) {
                super(id);
            }

            @Override
            public boolean requirementsMet(Player player) {
                return player.getShipColorsRepelled().size() == 5;
            }

            @Override
            public String getName() {
                return "Pirate's Nest";
            }

            @Override
            public String getDescription() {
                return "While this Contract is active in the match, if you have repelled 5 Ships of different colors, you automatically sign it.";
            }

            @Override
            public int[] getRewards() {
                return new int[]{3, 2, 1, 0, 0};
            }
}
