package com.matteopaciolla.portroyal.core;

import java.util.*;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.core.cards.employees.*;

import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import com.matteopaciolla.portroyal.core.cards.expeditions.Expedition;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.core.effects.MademoiselleEffect;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class Player implements Comparable<Player> {
    @Getter
    private final String name;
    @Getter
    private final List<Expedition> expeditionCards = new LinkedList<>();
    @Getter
    private final List<EmployeeCard> employees = new LinkedList<>();
    @Getter
    private int contractsCompleted;
    @Getter
    @Setter
    private int tradingCapacity;
    private Deck<Card> wallet;

    // Contracts conditions
    @Getter
    private int redShipRenounced = 0;
    @Getter
    private int blackShipRenounced = 0;
    @Getter
    private final Set<ShipColor> shipColorsRepelled = new HashSet<>();
    @Getter
    @Setter
    private boolean taxed = false;
    @Getter
    private int minorSpeculator = 0;
    @Getter
    @Setter
    private boolean majorSpeculator = false;
    @Getter
    @Setter
    private boolean wentBust = false;


    public Player(@NonNull String name){
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        Player other = (Player) obj;
        return this.name.equals(other.name);
    }

    @Override
    public String toString() {
        return String.format("%s %d" + Emojis.MONEY + "%d" + Emojis.POWER + "%d" + Emojis.POINTS + "%d" + Emojis.EXPEDITION + " tc=%d e=%s",
                name, getMoneyValue(),
                getPowerValue(),
                getPointsValue(),
                expeditionCards.size(),
                tradingCapacity,
                employees);
    }

    public void setup(Deck<Card> deck) {
        this.wallet = deck;
    }

    public int getMoneyValue(){
        return wallet.size();
    }

    public int getPowerValue(){
        return employees.stream()
                .filter(card -> card instanceof PowerCard)
                .mapToInt(card -> ((PowerCard) card).getPower()).sum();
    }
    public int getPointsValue(){
        return employees.stream().mapToInt(EmployeeCard::getPoints).sum()
                + expeditionCards.stream().mapToInt(Expedition::getPoints).sum()
                + contractsCompleted;
    }

    public int getEmployeeClassNumber(Class<? extends EmployeeCard> employeeClass) {
        return (int) employees.stream().filter(card -> card.getClass().equals(employeeClass)).count();
    }

    /**
     * This method returns the number of employees of a specific class and color owned by the player.
     * @param employeeClass the class of the employee
     * @param shipColor the color of the employee
     */
    public int getColoredEmployeeClassNumber(Class<? extends ColoredEmployeeCard> employeeClass, ShipColor shipColor) {
        return (int) employees.stream()
                .filter(card -> card.getClass().equals(employeeClass))
                .filter(card -> ((ColoredEmployeeCard) card).getColor().equals(shipColor))
                .count();
    }

    public Deck<Card> removeMoney(int number){
        return wallet.getFirst(number);
    }

    public Card removeACoin(){
        return wallet.getFirst();
    }

    public void addMoney(Card card){
        this.wallet.insertTop(card);
    }

    public void addMoney(Deck<Card> cards){
        this.wallet.insertTop(cards);
    }

    public void hireEmployee(EmployeeCard employeeCard) {
        employees.add(employeeCard);
    }

    public void addExpeditionCard(Expedition expedition){
        this.expeditionCards.add(expedition);
    }

    public void decreaseTradingCapacity() {
        this.tradingCapacity--;
    }

    public void resetTradingCapacity() {
        this.tradingCapacity = 0;
    }

    public Deck<Card> commitExpedition(Expedition expedition, List<ExpeditionEmployee> employeesTypeList) {
        if (!expedition.isPlayerAbleToCommitExpedition(this, true)) {
            throw new IllegalStateException("Player is not able to commit the expedition");
        }
        expeditionCards.add(expedition);
        Deck<Card> employeeCardsToBeRemoved = ((LinkedDeck<Card>) wallet).getNewDeck();
        //add to employeeCardsToBeRemoved a card for each employee type in employeesTypeList
        for (ExpeditionEmployee employee : employeesTypeList) {
            EmployeeCard employeeCard = employees.stream()
                    .filter(card -> card.getClass().equals(employee.getReferredClass()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Player does not have the required employee"));
            employeeCardsToBeRemoved.insertTop(employeeCard);
            employees.remove(employeeCard);
        }
        return employeeCardsToBeRemoved;
    }

    public boolean canAffordHiring(EmployeeCard employeeCard, boolean isActivePlayer) {
        return getMoneyValue() >= getActualCost(employeeCard) + (isActivePlayer ? 0 : 1);
    }

    public int getActualCost(EmployeeCard employeeCard) {
        return employeeCard.getCost() - MademoiselleEffect.getMademoiselleDiscount(this);
    }

    public boolean canAffordAnyHiring(List<Card> harbor, boolean isActivePlayer) {
        return harbor.stream().anyMatch(card -> card instanceof EmployeeCard && canAffordHiring((EmployeeCard) card, isActivePlayer));
    }

    public boolean isFinalTurnCondition() {
        return getPointsValue() >= 12 && !getExpeditionCards().isEmpty();
    }

    public boolean isAbleToRepelShip(Ship ship) {
        return getPowerValue() >= ship.getPower();
    }

    public void renounceShip(Ship ship) {
        if (ship.getColor().equals(ShipColor.RED)) {
            redShipRenounced++;
        } else if (ship.getColor().equals(ShipColor.BLACK)) {
            blackShipRenounced++;
        }
    }

    public void addShipColorRepelled(ShipColor shipColor) {
        shipColorsRepelled.add(shipColor);
    }

    public void increaseMinorSpeculator() {
        minorSpeculator++;
    }

    public void increaseContractsCompleted() {
        this.contractsCompleted++;
    }

    /**
     * This method compares two players based on their points value, in case of tie the player with the most money wins.
     * A player with no expedition cards is considered to have 0 points.
     * @param otherPlayer the expedition to be checked
     * @return the comparison result.
     */
    @Override
    public int compareTo(Player otherPlayer) {
        if (otherPlayer.expeditionCards.isEmpty() && !this.expeditionCards.isEmpty()) {
            return 1;
        }
        if (this.expeditionCards.isEmpty() && !otherPlayer.expeditionCards.isEmpty()) {
            return -1;
        }
        int pointsComparison = Integer.compare(this.getPointsValue(), otherPlayer.getPointsValue());
        if (pointsComparison != 0) {
            return pointsComparison;
        } else {
            return Integer.compare(this.getMoneyValue(), otherPlayer.getMoneyValue());
        }
    }
}
