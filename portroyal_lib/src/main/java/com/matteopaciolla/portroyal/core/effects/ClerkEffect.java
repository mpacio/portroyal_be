package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Clerk;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;

public class ClerkEffect {

    public static void activate(Match match, Ship ship) {
        Player player = match.getRunningPlayer();
        int clerkCards = getClerkCardsNumber(player, ship.getColor());
        if (clerkCards > 0) {
            player.setTradingCapacity(player.getTradingCapacity() + clerkCards);
            match.addNotes(player.getName() + " got " + clerkCards + " more trading capacity from the clerk effect");
        }
    }

    public static int getClerkCardsNumber(Player player, ShipColor color) {
        return player.getColoredEmployeeClassNumber(Clerk.class, color);
    }
}
