package com.matteopaciolla.portroyal.core.phases;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.Table;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import com.matteopaciolla.portroyal.core.cards.expeditions.Expedition;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.core.cards.taxes.TaxCard;
import com.matteopaciolla.portroyal.core.effects.GovernorEffect;
import com.matteopaciolla.portroyal.core.effects.TaxEffect;
import com.matteopaciolla.portroyal.exceptions.userinput.BadPhaseOperationException;

import java.util.Set;

public class DiscoverPhase extends Phase{

    public DiscoverPhase(Match match) {
        super(match);
    }

    @Override
    public String getIcon() {
        return Emojis.PHASE_DISCOVER;
    }

    @Override
    public Card discover(){
        Card card = match.getTable().drawCard();
        //effects here
        switch (card) {
            case Ship ship-> {
                match.addNotes(ship.getColor().name() + " " + ship.getClass().getSimpleName() + "#" + ship.getId() + " discovered");
                if (match.getRunningPlayer().isAbleToRepelShip(ship)) {
                    match.setRepellingShip(ship);
                    match.setCurrentPhase(new RepelPhase(match));
                    break;
                }
                if (match.getTable().isBustCase(ship)) {//bust case
                    match.goBust(ship);
                } else {
                    match.getTable().addInHarbor(ship);
                }
//                break;// isn't necessary because of the return statement at the end of the method
            }
            case EmployeeCard employeeCard -> {
                match.getTable().addInHarbor(employeeCard);
                match.addNotes(employeeCard.getClass().getSimpleName() + "#" + employeeCard.getId() + " discovered");
//                break;// isn't necessary because of the return statement at the end of the method
            }
            case TaxCard taxCard -> {
                match.addNotes(taxCard.getClass().getSimpleName() + "#" + taxCard.getId() + " discovered");
                TaxEffect.activate(match, taxCard);
                match.getTable().discardCard(taxCard);
//                break;// isn't necessary because of the return statement at the end of the method
            }
            case Expedition expeditionCard -> {
                match.getTable().addExpeditionCard(expeditionCard);
                match.addNotes(expeditionCard.getClass().getSimpleName() + "#" + expeditionCard.getId() + " discovered");
//                break;// isn't necessary because of the return statement at the end of the method
            }
            case null, default -> throw new IllegalStateException("Impossible discovering cards");
        }
        return card;
    }

    @Override
    public void repelShip() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't repel a ship in this phase");
    }

    @Override
    public void acceptShip() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't accept a ship in this phase");
    }

    @Override
    public void finishDiscovering() {
        match.addNotes("Ships in harbor: " + match.getTable().getShipsInHarborNumber() + "/" + match.getTable().getHarbor().size());
        match.setCurrentPhase(new TradeHireMainPhase(match));
        setActualTradingCapacity();
        updateSpeculatorData(match.getRunningPlayer(), match.getTable());
        match.updateAutomaticContractProgress(match.getRunningPlayer());
    }

    @Override
    public Card tradeHire(int cardIndex, int pickPlayerIndex, boolean renounce) throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't trade/hire in this phase");
    }

    @Override
    public void endTurn() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't end your turn before finishing discover phase. Finish discovering first");
    }

    private void updateSpeculatorData(Player runningPlayer, Table table) {
        int harborColorSetSize = table.getShipsInHarborNumber();
        if (harborColorSetSize >= 3) {
            runningPlayer.increaseMinorSpeculator();
        }
        if (harborColorSetSize >= 4) {
            runningPlayer.setMajorSpeculator(true);
        }
    }

    private int getBaseTradingCapacity(){
        Set<ShipColor> colors = match.getTable().getHarborColorSet();
        return colors.size() - 2 > 0 ? colors.size() - 2 : 1;
    }

    public void setActualTradingCapacity() {
        match.getRunningPlayer().setTradingCapacity(getBaseTradingCapacity());
        GovernorEffect.activate(match);
    }
}
