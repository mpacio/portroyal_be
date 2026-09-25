package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;

public class Pirate extends PowerCard {
    public Pirate(int id, int points, int cost) {
        super(id, points, cost, 2);
    }

    @Override
    public String getIcon() {
        return Emojis.PIRATE;
    }
}
