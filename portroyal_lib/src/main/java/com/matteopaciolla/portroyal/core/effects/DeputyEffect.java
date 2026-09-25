package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Deputy;

public class DeputyEffect {

    /**
     * The player takes 1 coin for each deputy card he has if there are 3 or 4 cards in the harbor
     * @param match the current match
     */
    public static void activate(Match match) {
        Player runningPlayer = match.getRunningPlayer();
        if (match.getTable().getHarbor().size() == 3 || match.getTable().getHarbor().size() == 4) {
            int deputyCards = getDeputyCardsNumber(runningPlayer);
            if (deputyCards > 0) {
                runningPlayer.addMoney(match.getTable().getMoney(deputyCards));
                match.addNotes(runningPlayer.getName() + " got " + deputyCards + " coins from the deputy effect");
            }
        }
    }

    public static int getDeputyCardsNumber(Player player) {
        return player.getEmployeeClassNumber(Deputy.class);
    }
}
