package com.matteopaciolla.portroyal.core.phases;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.Event;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.ships.CargoShip;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.core.effects.*;
import com.matteopaciolla.portroyal.core.enums.EventType;
import com.matteopaciolla.portroyal.exceptions.userinput.*;

public class TradeHireMainPhase extends Phase {

    public TradeHireMainPhase(Match match) {
        super(match);
    }

    @Override
    public String getIcon() {
        return Emojis.PHASE_TRADE;
    }

    @Override
    public Card discover() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't discover a card during the trade/hire phase");
    }

    @Override
    public void repelShip() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't repel a ship in the trade/hire phase");
    }

    @Override
    public void acceptShip() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't accept a ship in this phase");
    }

    @Override
    public void finishDiscovering() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't finish discovering in the trade/hire phase");
    }

    @Override
    public Card tradeHire(int cardIndex, int pickPlayerIndex, boolean renounce) throws NotEnoughMoneyForHiringException,
            NotEnoughHiringCapacityException, EmptyHarborDemandException,
            UndefinedPickPlayerIndexException, SelfPickPlayerIndexException, HarborCardIndexNotValidException {
        tradeHireChecks(cardIndex, pickPlayerIndex);
        Card card = match.getTable().getHarbor().get(cardIndex);
        if (card instanceof CargoShip && !pickPlayerChecksPassed(pickPlayerIndex)) {
            throw new UndefinedPickPlayerIndexException("Invalid player index");
        }
        if (card instanceof Ship ship) {
            tradeShip(ship, pickPlayerIndex, renounce);
        } else if (card instanceof EmployeeCard employeeCard) {
            hireEmployee(employeeCard);
            match.addNotes(match.getRunningPlayer().getName() + " hired " + employeeCard.getClass().getSimpleName() + "#" + employeeCard.getId());
        } else {
            throw new IllegalStateException("Invalid card type in harbor");
        }
        AdmiralEffect.activate(match);
        DeputyEffect.activate(match);
        GunnerEffect.activate(match);
        match.getTable().getHarbor().remove(card);
        match.getRunningPlayer().decreaseTradingCapacity();
        if (match.getRunningPlayer().getTradingCapacity() == 0 && match.comparePhase(TradeHireSubPhase.class)) {
            endTurn();
        }
        return card;
    }

    @Override
    public void endTurn() {
        checkAndSetFinalTurn();
        match.getRunningPlayer().resetTradingCapacity();
        int nextPlayerIndex = getNextSubTraderPlayerIndex();
        if (nextPlayerIndex == -1) {
            match.addNotes("EoT removed " + match.getTable().getHarbor().size() + " cards");
            match.getTable().discardHarbor();
            match.startNewTurn();// also sets the phase to DiscoverPhase
        } else {
            match.setCurrentPhase(new TradeHireSubPhase(match));
        }
    }

    private boolean pickPlayerChecksPassed(int pickPlayerIndex) {
        return pickPlayerIndex >= 0 && pickPlayerIndex < match.getPlayers().size() && pickPlayerIndex != match.getRunningPlayerIndex();
    }

    void tradeShip(Ship ship, int pickPlayerIndex, boolean renounce) {
        match.getTable().discardCard(ship);
        if (renounce) {
            match.getRunningPlayer().renounceShip(ship);
            match.updateAutomaticContractProgress(match.getRunningPlayer());
            match.addNotes(match.getRunningPlayer().getName()+ " renounced the "
                    + ship.getColor().name() + " " + ship.getClass().getSimpleName() + "#" + ship.getId()
                    + " no money gained");
        } else {
            match.getRunningPlayer().addMoney(match.getTable().getMoney(ship.getGain()));
            match.addNotes(match.getRunningPlayer().getName() + " traded with "
                    + ship.getColor().name() + " " + ship.getClass().getSimpleName() + "#" + ship.getId());
            MerchantEffect.prize(match, ship);
        }
        if (ship instanceof CargoShip) {
            String suffix = "";
            int benefitingPlayerIndex = pickPlayerIndex;
            if (match.getConfiguration().isCargoCoinToPoorestPlayer()) {
                benefitingPlayerIndex = match.getPlayers().stream()
                        .reduce((p1, p2) -> p1.getMoneyValue() < p2.getMoneyValue() ? p1 : p2)
                        .map(match.getPlayers()::indexOf)
                        .orElseThrow(() -> new IllegalStateException("No player found"));
                suffix = " (poorest)";
            } else {
                match.getPlayers().get(benefitingPlayerIndex).addMoney(match.getTable().getMoney(1));
            }
            match.addNotes(match.getPlayers().get(benefitingPlayerIndex).getName() + suffix
                    + " benefited from " + ship.getClass().getSimpleName());
            match.getSideEvents().add(new Event(EventType.GOT_CARGO_SHIP_MONEY, match.getPlayers().get(benefitingPlayerIndex), 1, ship));
        }
        ClerkEffect.activate(match, ship);
    }

    void hireEmployee(EmployeeCard  employeeCard) throws NotEnoughMoneyForHiringException {
        Player runningPlayer = match.getRunningPlayer();
        if (runningPlayer.canAffordAnyHiring(match.getTable().getHarbor(), true)){
            if (runningPlayer.canAffordHiring(employeeCard, true)) {
                if (employeeCard.getCost() > runningPlayer.getActualCost(employeeCard)) {
                    match.addNotes(runningPlayer.getName() + " got "
                            + (employeeCard.getCost() - runningPlayer.getActualCost(employeeCard))
                            + " discount from the mademoiselle effect");
                }
                match.getTable().discardCards(runningPlayer.removeMoney(runningPlayer.getActualCost(employeeCard)));
                runningPlayer.hireEmployee(employeeCard);
            } else {
                throw new NotEnoughMoneyForHiringException("You don't have enough money to hire this employee");
            }
        } else {
            throw new NotEnoughMoneyForHiringException("You don't have enough money to hire any employee");
        }
    }

    int getNextSubTraderPlayerIndex() {
        int nextPlayerIndex = -1;
        match.changeRunningPlayer();
        while (!match.isRunningTheActivePlayer()) {
            if (match.getTable().isThereAnyShipInHarbor() || match.getRunningPlayer().canAffordAnyHiring(match.getTable().getHarbor(), false)) {
                nextPlayerIndex = match.getRunningPlayerIndex();
                break;
            }
            match.changeRunningPlayer();
        }
        return nextPlayerIndex;
    }

    private void checkHarborCardIndex(int cardIndex) throws HarborCardIndexNotValidException {
        if (cardIndex < 0 || cardIndex >= match.getTable().getHarbor().size()) {
            throw new HarborCardIndexNotValidException("Invalid harbor card index: " + cardIndex);
        }
    }

    private void tradeHireChecks(int cardIndex, int pickPlayerIndex) throws NotEnoughHiringCapacityException, EmptyHarborDemandException, SelfPickPlayerIndexException, HarborCardIndexNotValidException {
        checkHarborCardIndex(cardIndex);
        if (match.getRunningPlayer().getTradingCapacity() == 0) {
            throw new NotEnoughHiringCapacityException("You don't have enough trading capacity to trade/hire.");
        }
        if (match.getTable().getHarbor().isEmpty()) {
            throw new EmptyHarborDemandException("The harbor is empty");
        }
        if (match.getRunningPlayerIndex() == pickPlayerIndex) {
            throw new SelfPickPlayerIndexException("You can't pick yourself");
        }
    }

    private void checkAndSetFinalTurn() {
        if (match.getRunningPlayer().isFinalTurnCondition() && !match.isFinalTurn()) {
            match.setFinalTurn(true);
            match.addNotes(match.getRunningPlayer().getName() + " triggered the final turn condition");
            match.getSideEvents().add(new Event(EventType.TRIGGERED_FINAL_TURN, match.getRunningPlayer()));
        }
    }
}
