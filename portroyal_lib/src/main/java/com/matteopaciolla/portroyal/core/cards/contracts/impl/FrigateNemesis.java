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
        return "While this Contract is active in the match, if you decline all Coins (renounce) when trading with a red ship (including matching Merchants bonus), " +
                "the first time you do so, you are considered to have started signing this Contract, reducing by 1 the maximum number of Contracts you may sign. " +
                "Each further time you renounce Coins with a red ship, you advance your progress toward signing it; the third time, you automatically sign the Contract. " +
                "Once you start signing this Contract, that commitment remains for the rest of the game, even if you do not complete it renouncing Coins enough times. " +
                "Multiple players can start signing this Contract at the same time.";
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
