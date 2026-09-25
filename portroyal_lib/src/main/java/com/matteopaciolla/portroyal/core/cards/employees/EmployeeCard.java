package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.cards.Card;

import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class EmployeeCard extends Card {
    private final int points;
    private final int cost;

    public EmployeeCard(int id, int points, int cost) {
        super(id);
        this.points = points;
        this.cost = cost;
    }

    @Override
    public String toString() {
        String template = "%s %d" + Emojis.MONEY + "%d" + Emojis.POINTS;
        return String.format(template, super.toString(), cost, points);
    }
}
