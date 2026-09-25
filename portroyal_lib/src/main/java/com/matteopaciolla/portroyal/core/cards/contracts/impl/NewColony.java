package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.cards.contracts.abst.TwoProfessionsContractCard;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.employees.Gunner;
import com.matteopaciolla.portroyal.core.cards.employees.Settler;

public class NewColony extends TwoProfessionsContractCard {

    public static final Class<? extends EmployeeCard> PROFESSION_1_CLASS = Settler.class;
    public static final Class<? extends EmployeeCard> PROFESSION_2_CLASS = Gunner.class;

    public NewColony(int id) {
        super(id);
    }

    @Override
    public Class<? extends EmployeeCard> getProfession1Class() {
        return PROFESSION_1_CLASS;
    }

    @Override
    public Class<? extends EmployeeCard> getProfession2Class() {
        return PROFESSION_2_CLASS;
    }

    @Override
    public String getProfession1Name() {
        return PROFESSION_1_CLASS.getSimpleName();
    }

    @Override
    public String getProfession2Name() {
        return PROFESSION_2_CLASS.getSimpleName();
    }

    @Override
    public String getName() {
        return "New Colony";
    }

    @Override
    public int[] getRewards() {
        return new int[]{3, 2, 1, 0, 0};
    }
}
