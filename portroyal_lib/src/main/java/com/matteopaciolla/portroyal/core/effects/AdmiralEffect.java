package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Admiral;

public class AdmiralEffect extends Effect {

    /**
     * The player takes 2 coins for each admiral card he has
     * if there are at least 5 cards in the harbor
     * @param match the current match
     */
    public static void activate(Match match) {
        Player runningPlayer = match.getRunningPlayer();
        if (match.getTable().getHarbor().size() >= 5) {
            int admiralCards = getAdmiralCardsNumber(runningPlayer);
            if (admiralCards > 0) {
                runningPlayer.addMoney(match.getTable().getMoney(2 * admiralCards));
                match.addNotes(runningPlayer.getName() + " got " + 2 * admiralCards + " coins from the admiral effect");
            }
        }
    }

    public static int getAdmiralCardsNumber(Player player) {
        return player.getEmployeeClassNumber(Admiral.class);
    }
}
