package com.matteopaciolla.portroyal.core.cards.employees;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import lombok.Getter;

@Getter
public abstract class ColoredEmployeeCard extends EmployeeCard {
    private final ShipColor color;
    public ColoredEmployeeCard(int id, int points, int cost, ShipColor color) {
        super(id, points, cost);
        this.color = color;
    }

    public abstract String getClassIcon();

    @Override
    public String getIcon() {
        return getClassIcon() + Emojis.SHIP_COLORS_MAP.get(this.color);
    }
}
