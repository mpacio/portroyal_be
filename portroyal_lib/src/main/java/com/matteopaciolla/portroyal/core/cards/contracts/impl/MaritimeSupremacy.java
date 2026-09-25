package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.cards.contracts.abst.TwoProfessionsContractCard;
import com.matteopaciolla.portroyal.core.cards.employees.Captain;
import com.matteopaciolla.portroyal.core.cards.employees.Deputy;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;

public class MaritimeSupremacy extends TwoProfessionsContractCard {

    public static final Class<? extends EmployeeCard> PROFESSION_1_CLASS = Captain.class;
    public static final Class<? extends EmployeeCard> PROFESSION_2_CLASS = Deputy.class;

    public MaritimeSupremacy(int id) {
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
        return "Maritime Supremacy";
    }

    @Override
    public int[] getRewards() {
        return new int[]{4, 3, 2, 1, 0};
    }
}
