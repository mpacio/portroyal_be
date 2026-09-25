package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;

public class Handyman extends EmployeeCard {
    public Handyman(int id, int points, int cost) {
        super(id, points, cost);
    }

    @Override
    public String getIcon() {
        return Emojis.HANDYMAN;
    }
}
