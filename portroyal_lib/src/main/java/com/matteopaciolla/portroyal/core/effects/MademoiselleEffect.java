package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Mademoiselle;

public class MademoiselleEffect extends Effect {

    public static int getMademoiselleDiscount(Player player) {
        return player.getEmployeeClassNumber(Mademoiselle.class);
    }
}