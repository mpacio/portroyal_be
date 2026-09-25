package com.matteopaciolla.portroyal.core.cards.ships;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.cards.Card;

import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import lombok.Getter;

@Getter
public class Ship extends Card {
    final ShipColor color;
    final int gain;
    final int power;

    public Ship(int id, int gain, int power, ShipColor color) {
        super(id);
        this.gain = gain;
        this.power = power;
        this.color = color;
    }

    @Override
    public String toString() {
        return super.toString() + " " + getMoneyPowerString();
    }

    public String getMoneyPowerString() {
        return this.gain + Emojis.MONEY + getPowerString();
    }

    @Override
    public String getIcon() {
        return Emojis.SHIP + getColorIcon();
    }

    public String getColorIcon() {
        return Emojis.SHIP_COLORS_MAP.get(this.color);
    }

    public String getPowerString() {
        return power < 100 ? this.power + Emojis.POWER : Emojis.INFINITE_POWER;
    }
}
