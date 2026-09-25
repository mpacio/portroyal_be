package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.employees.Captain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    @Test
    void testDrawCardWithEmptyDrawPile() {
        Table table = new Table(new LinkedDeck<>(new Random()));
        Card uniqueCard = new Captain(1,1,4);
        table.discardCard(uniqueCard);
        Card drawnCard = table.drawCard();
        assertThat(drawnCard).isNotNull();
        assertThat(drawnCard).isInstanceOf(Captain.class);
        assertThat(table.getDrawPileSize()).isEqualTo(0);
        assertThat(table.getDiscardPileSize()).isEqualTo(0);
        assertThat(drawnCard).isEqualTo(uniqueCard);
    }

    @Test
    void testGetMoneyWithEmptyDrawPile() {
        Table table = new Table(new LinkedDeck<>(new Random()));
        table.discardCard(new Captain(1,1,4));
        table.discardCard(new Captain(2,1,4));
        table.discardCard(new Captain(3,1,4));
        table.discardCard(new Captain(4,1,4));
        Deck<Card> money = table.getMoney(2);
        assertThat(money.size()).isEqualTo(2);
        assertThat(table.getDrawPileSize()).isEqualTo(2);
    }

    @Test
    void testGetMoneyWithLessCardsInDrawPileThanRequested() {
        Card uniqueDrawnableCard = new Captain(4,1,4);
        Deck<Card> deck = new LinkedDeck<>(new Random(), List.of(uniqueDrawnableCard));
        Table table = new Table(deck);
        table.discardCard(new Captain(1,1,4));
        table.discardCard(new Captain(2,1,4));
        table.discardCard(new Captain(3,1,4));
        Deck<Card> money =table.getMoney(2);
        Card firstCard = money.getFirst();
        assertThat(firstCard).isEqualTo(uniqueDrawnableCard);
        assertThat(table.getDrawPileSize()).isEqualTo(2);
        assertThat(table.getDiscardPileSize()).isEqualTo(0);
    }
}