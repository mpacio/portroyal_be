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
                return "Whenever you repel a ship as active player using your Sailors and Pirates, " +
                        "you may take a repelled Ship of a color you don’t have to place in your display, " +
                        "otherwise discard it as usual. Once you have all 5 colors of Ships, " +
                        "immediately discard them and place your marker to complete this Contract.";
            }

            @Override
            public int[] getRewards() {
                return new int[]{3, 2, 1, 0, 0};
            }
}
