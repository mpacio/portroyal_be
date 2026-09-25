package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.RenouncingContractCard;

import java.util.List;

public class FrigateNemesis extends RenouncingContractCard {

    public FrigateNemesis(int id) {
        super(id);
    }

    @Override
    public boolean requirementsMet(Player player) {
        return player.getRedShipRenounced() > 2;
    }

    @Override
    public String getName() {
        return "Frigate Nemesis";
    }

    @Override
    public String getDescription() {
        return "If you decline all Coins when trading with a Frigate (including matching Traders), " +
                "you may immediately place a marker on space [1]. The next time you decline Coins, " +
                "move your marker to space [2]. The third time you decline Coins, you immediately " +
                "complete the Contract. Once you place a marker on this Contract, it remains there " +
                "for the rest of the game, even if it’s not completed. Multiple players can have " +
                "markers on these spaces.";
    }

    public static List<String> getPark1PlayersNames(Match match) {
        // return the list of the players who have a single red ship renounced
        return match.getPlayers().stream()
                .filter(player -> player.getRedShipRenounced() == 1)
                .map(Player::getName)
                .toList();
    }

    public static List<String> getPark2PlayersNames(Match match) {
        // return the list of the players who have two red ships renounced
        return match.getPlayers().stream()
                .filter(player -> player.getRedShipRenounced() == 2)
                .map(Player::getName)
                .toList();
    }

    @Override
    public int[] getRewards() {
        return new int[]{8, 6, 5, 4, 3};
    }
}
