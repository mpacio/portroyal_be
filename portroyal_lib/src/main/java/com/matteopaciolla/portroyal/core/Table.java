package com.matteopaciolla.portroyal.core;

import java.util.*;

import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.expeditions.Expedition;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;

import lombok.Getter;
import lombok.Setter;

public class Table {

    private Deck<Card> drawPile;
    private Deck<Card> discardPile;
    @Getter
    private final List<Card> harbor = new LinkedList<>();
    @Getter
    private final List<Expedition> expeditionCards = new LinkedList<>();
    @Getter
    @Setter
    private ContractsBoard contractsBoard = null;

    public Table(Deck<Card> deck) {
        this.drawPile = deck;
        this.discardPile = ((LinkedDeck<Card>) drawPile).getNewDeck();
    }

    public int getDrawPileSize() {
        return drawPile.size();
    }

    public int getDiscardPileSize() {
        return discardPile.size();
    }

    @Override
    public String toString() {
        return "Table draw=" + drawPile
                + ", discard=" + discardPile
                + "\nharbor=" + harbor
                + "\nexpeditions=" + expeditionCards
                + "\ncontracts=" + contractsBoard;
    }

    public void shuffleDrawPile(){
        this.drawPile.shuffle();
    }

    public void refreshDrawPile(){
        Deck<Card> tempDeck = drawPile;
        drawPile = discardPile;
        shuffleDrawPile();
        drawPile.insertTop(tempDeck);
        discardPile = ((LinkedDeck<Card>) drawPile).getNewDeck();
    }

    public Card drawCard() {
        if (drawPile.isEmpty()) {
            if (!discardPile.isEmpty()) {
                refreshDrawPile();
            } else {
                return null;
            }
        }
        return drawPile.getFirst();
    }

    public Deck<Card> getMoney(int number){
        if (drawPile.size() < number) {
            refreshDrawPile();
        }
        return drawPile.getFirst(number);
    }

    public void discardCard(Card card){
        discardPile.insertTop(card);
    }

    public void discardCards(Deck<Card> cards){
        discardPile.insertTop(cards);
    }

    public void discardHarbor() {
        discardPile.insertTop(((LinkedDeck<Card>) drawPile).getNewDeck(harbor));
        harbor.clear();
    }

    public Set<ShipColor> getHarborColorSet(){
        Set<ShipColor> set = new HashSet<>();
        harbor.stream().filter(Ship.class::isInstance).forEach(card -> set.add(((Ship) card).getColor()));
        return set;
    }

    public List<Card> getEmployees(){
        return harbor.stream().filter(EmployeeCard.class::isInstance).toList();
    }

    public void addInHarbor(Card card){
        harbor.add(card);
    }

    public void addExpeditionCard(Expedition card) {
        expeditionCards.add(card);
    }

    public boolean isColorInHarbor(ShipColor color){
        return harbor.stream().anyMatch(card -> card instanceof Ship && ((Ship) card).getColor() == color);
    }

    public boolean isBustCase(Ship ship) {
        return isColorInHarbor(ship.getColor());
    }

    /**
     * Checks if there is at least one ship in the harbor.
     * @return true if there is at least one ship in the harbor, false otherwise.
     */
    public boolean isThereAnyShipInHarbor(){
        return harbor.stream().anyMatch(card -> card instanceof Ship);
    }

    public int getShipsInHarborNumber() {
        return (int) harbor.stream().filter(Ship.class::isInstance).count();
    }
}
