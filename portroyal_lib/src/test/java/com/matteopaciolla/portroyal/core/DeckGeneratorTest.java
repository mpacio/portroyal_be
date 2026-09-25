package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.portroyal.core.cards.Card;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeckGeneratorTest {

    @Test
    public void testGenerateDeck() {
        Deck<Card> deck1 = DeckGenerator.getMainDeck(new Random(101), 3, Configuration.builder().JOMC_ExpansionUsed(false).build());
        assertEquals(119, deck1.size());
        Deck<Card> deck2 = DeckGenerator.getMainDeck(new Random(101), 5, Configuration.builder().JOMC_ExpansionUsed(false).build());
        assertEquals(120, deck2.size());
        Deck<Card> deck3 = DeckGenerator.getMainDeck(new Random(101), 3, Configuration.builder().build());
        assertEquals(143, deck3.size());
        Deck<Card> deck4 = DeckGenerator.getMainDeck(new Random(101), 5, Configuration.builder().build());
        assertEquals(144, deck4.size());
    }
}
