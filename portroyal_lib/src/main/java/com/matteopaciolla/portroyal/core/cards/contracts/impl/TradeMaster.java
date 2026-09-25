package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.TwoProfessionsContractCard;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.employees.Merchant;

public class TradeMaster extends TwoProfessionsContractCard {

    public static final Class<? extends EmployeeCard> PROFESSION_1_AND_2_CLASS = Merchant.class;

    public TradeMaster(int id) {
        super(id);
    }

    @Override
    public Class<? extends EmployeeCard> getProfession1Class() {
        return PROFESSION_1_AND_2_CLASS;
    }

    @Override
    public Class<? extends EmployeeCard> getProfession2Class() {
        return PROFESSION_1_AND_2_CLASS;
    }

    @Override
    public String getProfession1Name() {
        return PROFESSION_1_AND_2_CLASS.getSimpleName();
    }

    @Override
    public String getProfession2Name() {
        return PROFESSION_1_AND_2_CLASS.getSimpleName();
    }

    @Override
    public String getName() {
        return "Trade Master";
    }

    @Override
    public boolean requirementsMet(Player player) {
        // return true if the player has at least 2 Merchants
        return player.getEmployees().stream().filter(e -> e.getClass().equals(PROFESSION_1_AND_2_CLASS)).count() >= 2;
    }

    @Override
    public int[] getRewards() {
        return new int[]{3, 2, 1, 0, 0};
    }
}
