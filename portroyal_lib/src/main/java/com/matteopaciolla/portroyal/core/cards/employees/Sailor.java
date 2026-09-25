package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;

public class Sailor extends PowerCard {
    public Sailor(int id, int points, int cost) {
        super(id, points, cost, 1);
    }

    @Override
    public String getIcon() {
        return Emojis.SAILOR;
    }
}
