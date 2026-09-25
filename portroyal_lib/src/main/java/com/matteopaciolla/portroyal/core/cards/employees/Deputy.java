package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;

public class Deputy extends EmployeeCard {
    public Deputy(int id, int points, int cost) {
        super(id, points, cost);
    }

    @Override
    public String getIcon() {
        return Emojis.DEPUTY;
    }
}
