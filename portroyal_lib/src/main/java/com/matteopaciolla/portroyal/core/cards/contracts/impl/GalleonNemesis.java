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
        return "If you decline all Coins (renounce) when trading with a black Ship (including matching Merchants bonus), " +
                "the first time this happens, you are considered to have started signing this Contract, reducing by 1 the maximum number of Contracts you may sign. " +
                "The second time you renounce Coins with a black Ship, you automatically sign the Contract. " +
                "Once you start signing this Contract, that commitment remains for the rest of the game, even if you do not complete it by renouncing Coins again. " +
                "Multiple players can start signing this Contract at the same time.";
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
