package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Event;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Jester;
import com.matteopaciolla.portroyal.core.enums.EventType;

public class JesterEffect extends Effect {

    /**
     * Prize the players with jesters
     * @param match the match
     */
    public static void prizePlayers(Match match) {
        match.getPlayers().forEach(player -> {
            int jestersNumber = getJestersNumber(player);
            if (jestersNumber > 0) {
                player.addMoney(match.getTable().getMoney(jestersNumber));
                match.addNotes(player.getName() + " got " + jestersNumber + " coins from the jester effect");
                match.getSideEvents().add(new Event(EventType.GOT_JESTER_MONEY_EFF, player, jestersNumber));
            }
        });
    }

    /**
     * Get the number of jesters owned by the player
     * @param player the player
     * @return the number of jesters owned by the player
     */
    private static int getJestersNumber(Player player) {
        return (int) player.getEmployees().stream().filter(card -> card instanceof Jester).count();
    }
}
