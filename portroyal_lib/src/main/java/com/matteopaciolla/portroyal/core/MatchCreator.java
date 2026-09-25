package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.exceptions.internal.InternalGameException;
import com.matteopaciolla.portroyal.core.cards.Card;

import java.util.List;
import java.util.Random;

public class MatchCreator {

    public static Match createMatch(long seed, List<Player> players, Configuration configuration, List<MoveRecord> movesHistory) throws InternalGameException {
        if (!areAllDifferentPlayers(players)) {
            throw new IllegalArgumentException("Players must be different");
        }
        if (configuration == null) {
            configuration = Configuration.builder().build();
        }
        Random random = new Random(seed);
        Deck<Card> baseDeck = DeckGenerator.getMainDeck(random, players.size(),configuration);
        Match match = new Match(random, baseDeck, players, configuration);
        if (configuration.isJOMC_ExpansionUsed()) {
            Deck<ContractCard> contractCardsDeck = DeckGenerator.getContractDeck(random, configuration);
            match.setContractsDeck(contractCardsDeck);
        }
        match.setup(movesHistory);
        return match;
    }

    public static boolean areAllDifferentPlayers(List<Player> players) {
        return players.stream().distinct().count() == players.size();
    }
}
