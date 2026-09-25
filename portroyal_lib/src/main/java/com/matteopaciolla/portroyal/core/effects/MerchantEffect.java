package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Merchant;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;

import java.util.Map;
import java.util.stream.Collectors;

public class MerchantEffect extends Effect {

    /**
     * Prize the player with as many coins as the number of merchants of the same color of the ship
     * @param match the match
     * @param ship the ship that is traded
     */
    public static void prize(Match match, Ship ship){
        Player player = match.getRunningPlayer();
        Map<ShipColor, Integer> merchantsColorsMap = getMerchantsColorsMap(player);
        int prize = merchantsColorsMap.getOrDefault(ship.getColor(), 0);
        if (prize > 0) {
            player.addMoney(match.getTable().getMoney(prize));
            match.addNotes(player.getName() + " got " + prize + " coins from the merchant effect");
        }
    }

    /**
     * Calculate the number of merchants of each color owned by the player
     * @param player the player
     * @return a map with the number of merchants of each color owned by the player
     */
    private static Map<ShipColor, Integer> getMerchantsColorsMap(Player player){
        return player.getEmployees().stream()
                .filter(card -> card instanceof Merchant)
                .map(card -> (Merchant) card)
                .collect(Collectors.toMap(Merchant::getColor, merchant -> 1, Integer::sum));

    }
}
