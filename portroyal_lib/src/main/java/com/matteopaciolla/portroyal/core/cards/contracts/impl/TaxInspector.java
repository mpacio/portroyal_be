package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.AutomaticContractCard;

public class TaxInspector extends AutomaticContractCard {

        public TaxInspector(int id) {
            super(id);
        }

    @Override
    public boolean requirementsMet(Player player) {
        return player.isTaxed();
    }

    @Override
        public String getName() {
            return "Tax Inspector";
        }

    @Override
    public String getDescription() {
        return "When paying taxes, you may immediately place a marker to complete this Contract.";
    }

    @Override
    public int[] getRewards() {
        return new int[]{7, 7, 7, 7, 7};
    }
}
