package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.confs.BaseDeckDictionary;
import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.portroyal.confs.JOMC_ExpansionDeckDictionary;
import com.matteopaciolla.portroyal.core.cards.*;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;

import java.util.Random;

public class DeckGenerator {
    
    
    public static Deck<Card> getMainDeck(Random random, int numPlayers, Configuration configuration) {
        Deck<Card> deck = new LinkedDeck<>(random, BaseDeckDictionary.DECK_LIST);
        if (numPlayers < configuration.getBigExpeditionMinimumPlayersNumber()) {
            deck.getLast();
        }
        if (configuration.isJOMC_ExpansionUsed()){
            deck.insertTop(new LinkedDeck<>(random, JOMC_ExpansionDeckDictionary.DECK_LIST));
        }
        deck.reverse();
        return deck;
    }

    public static Deck<ContractCard> getContractDeck(Random random, Configuration configuration) {
        return new LinkedDeck<>(random, JOMC_ExpansionDeckDictionary.CONTRACTS_DECK_LIST);
    }
}
