package com.matteopaciolla.portroyal.core.cards.ships;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;

public class CargoShip extends Ship {

    public CargoShip(int id, int power, ShipColor color) {
        super(id, 3, power, color);
    }

    @Override
    public String getMoneyPowerString() {
        return this.gain + "+1" + Emojis.MONEY + getPowerString();
    }
}
