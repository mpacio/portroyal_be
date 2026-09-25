package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;

public class Captain extends EmployeeCard {
    public Captain(int id, int points, int cost) {
        super(id, points, cost);
    }

    @Override
    public String getIcon() {
        return Emojis.CAPTAIN;
    }
}
