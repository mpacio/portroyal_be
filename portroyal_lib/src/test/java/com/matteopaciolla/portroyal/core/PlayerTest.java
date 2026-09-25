package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.employees.Captain;
import com.matteopaciolla.portroyal.core.cards.employees.Priest;
import com.matteopaciolla.portroyal.core.cards.employees.Settler;
import com.matteopaciolla.portroyal.core.cards.expeditions.Expedition;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void compareTo_whenOtherPlayerHasNoExpeditionsAndThisPlayerHasExpeditions_returnsPositive() {
        Player player1 = new Player("Player 1");
        Player player2 = new Player("Player 2");
        player1.addExpeditionCard(new Expedition(1, 1, 1, 1, 1, 1));

        int result = player1.compareTo(player2);

        assertTrue(result > 0);
    }

    @Test
    void compareTo_whenThisPlayerHasNoExpeditionsAndOtherPlayerHasExpeditions_returnsNegative() {
        Player player1 = new Player("Player 1");
        Player player2 = new Player("Player 2");
        player2.addExpeditionCard(new Expedition(1,1,1,1,1,1));

        int result = player1.compareTo(player2);

        assertTrue(result < 0);
    }

    @Test
    void compareTo_whenBothPlayersHaveSamePoints_returnsComparisonBasedOnMoney() {
        Player player1 = new Player("Player 1");
        Player player2 = new Player("Player 2");
        player1.setup(new LinkedDeck<>(new Random()));
        player2.setup(new LinkedDeck<>(new Random()));
        player1.addMoney(new Captain(1, 1, 4));
        player2.addMoney(new Priest(2,1,4));
        player2.addMoney(new Settler(3,1,4));

        int result = player1.compareTo(player2);

        assertTrue(result < 0);
    }

    @Test
    void compareTo_whenBothPlayersHaveDifferentPoints_returnsComparisonBasedOnPoints() {
        Player player1 = new Player("Player 1");
        Player player2 = new Player("Player 2");
        player1.addExpeditionCard(new Expedition(1,1,1,1,1,1));
        player2.addExpeditionCard(new Expedition(2,1,1,1,1,1));
        player2.addExpeditionCard(new Expedition(3,1,1,1,1,1));

        int result = player1.compareTo(player2);

        assertTrue(result < 0);
    }
}