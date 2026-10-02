package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ManualContractCard;

public class ThriftyStaff extends ManualContractCard {

    public ThriftyStaff(int id) {
        super(id);
    }

    @Override
    public boolean requirementsMet(Player player) {
        // check if the player has 4 professions in his personal display costing 3 coins or less
        int count = 0;
        for (int i = 0; i < player.getEmployees().size(); i++) {
            if (player.getEmployees().get(i).getCost() <= 3) {
                count++;
            }
        }
        return count >= 4;
    }

    @Override
    public String getName() {
        return "Thrifty Staff";
    }

    @Override
    public String getDescription() {
        return "If you have 4 Employees in your personal display costing 3 Coins " +
                "each (or less in the future…), you may sign this Contract. " +
                "(Discounts from Mademoiselle don’t count.)";
    }

    @Override
    public int[] getRewards() {
        return new int[]{5, 4, 3, 2, 1};
    }
}
