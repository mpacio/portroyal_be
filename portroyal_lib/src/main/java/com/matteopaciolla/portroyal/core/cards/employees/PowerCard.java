package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;
import lombok.Getter;

@Getter
public abstract class PowerCard extends EmployeeCard {
    private final int power;
    public PowerCard(int id, int points, int cost, int power) {
        super(id, points, cost);
        this.power = power;
    }

    @Override
    public String toString() {
        return super.toString() + Emojis.POWER.repeat(this.power);
    }
}
