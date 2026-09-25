package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Governor;

public class GovernorEffect extends Effect {

    /**
     * Activate the governor effect.
     * The effect is to increase the player's trading capacity
     * by the number of governor cards in the player's employee cards list.
     * @param match the current match
     */
    public static void activate(Match match){
        Player player = match.getRunningPlayer();
        //count the number of governor cards in the player's employee cards list
        int governorCards = getGovernorCardsNumber(player);
        if (governorCards > 0) {
            //add the number of governor cards to the player's trading capacity
            player.setTradingCapacity(player.getTradingCapacity() + governorCards);
            match.addNotes(player.getName() + " got " + governorCards + " more trading capacity from the governor effect");
        }
    }

    public static int getGovernorCardsNumber(Player player) {
        return (int) player.getEmployees().stream().filter(card -> card instanceof Governor).count();
    }
}
