package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import lombok.Getter;

@Getter
public class Merchant extends ColoredEmployeeCard {

    public Merchant(int id, int points, int cost, ShipColor color) {
        super(id, points, cost, color);
    }

    @Override
    public String getClassIcon() {
        return Emojis.MERCHANT;
    }
}