package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.cards.contracts.abst.TwoProfessionsContractCard;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.employees.Jester;
import com.matteopaciolla.portroyal.core.cards.employees.Priest;

public class Luminary extends TwoProfessionsContractCard {

    public static final Class<? extends EmployeeCard> PROFESSION_1_CLASS = Priest.class;
    public static final Class<? extends EmployeeCard> PROFESSION_2_CLASS = Jester.class;

    public Luminary(int id) {
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
        return "Luminary";
    }

    @Override
    public int[] getRewards() {
        return new int[]{4, 3, 2, 1, 0};
    }
}
