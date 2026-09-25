package com.matteopaciolla.portroyal.core.cards.contracts.impl;

import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.RenouncingContractCard;

import java.util.List;

public class GalleonNemesis extends RenouncingContractCard {

    public GalleonNemesis(int id) {
        super(id);
    }

    @Override
    public boolean requirementsMet(Player player) {
        return player.getBlackShipRenounced() > 1;
    }

    @Override
    public String getName() {
        return "Galleon Nemesis";
    }

    @Override
    public String getDescription() {
        return "If you decline all Coins when trading with a Galleon (including matching Traders), " +
                "you may immediately place a marker on space [1]. The second time you decline Coins, " +
                "you immediately complete the Contract. Once you place a marker on this Contract, " +
                "it remains there for the rest of the game, even if it’s not completed. " +
                "Multiple players can have markers on these spaces.";
    }

    public static List<String> getPark1PlayersNames(Match match) {
        // return the list of the players who have a single black ship renounced
        return match.getPlayers().stream()
                .filter(player -> player.getBlackShipRenounced() == 1)
                .map(Player::getName)
                .toList();
    }

    @Override
    public int[] getRewards() {
        return new int[]{6, 5, 4, 3, 2};
    }
}
