package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Gunner;

public class GunnerEffect {

    public static void activate(Match match) {
        Player runningPlayer = match.getRunningPlayer();
        int gunnerCards = getGunnerCardsNumber(runningPlayer);
        if (gunnerCards > 0) {
            int shipsInHarborNumber = match.getTable().getShipsInHarborNumber();
            if (shipsInHarborNumber > 1) {
                runningPlayer.addMoney(match.getTable().getMoney((shipsInHarborNumber - 1) * gunnerCards));
                match.addNotes(runningPlayer.getName() + " got " + (shipsInHarborNumber - 1) * gunnerCards + " coins from the gunner effect");
            }
        }
    }

    public static int getGunnerCardsNumber(Player player) {
        return player.getEmployeeClassNumber(Gunner.class);
    }
}
