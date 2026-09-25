package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;

public class Mademoiselle  extends EmployeeCard {
    public Mademoiselle(int id, int points, int cost) {
        super(id, points, cost);
    }

    @Override
    public String getIcon() {
        return Emojis.MADEMOISELLE;
    }
}
