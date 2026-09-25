package com.matteopaciolla.portroyal.core.phases;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.exceptions.userinput.BadPhaseOperationException;
import lombok.Getter;

import java.util.List;

@Getter
public class RepelPhase extends Phase {

    public RepelPhase(Match match) {
        super(match);
    }

    @Override
    public String getIcon() {
        return Emojis.PHASE_REPEL;
    }

    @Override
    public Card discover() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't discover a card during the repel phase");
    }

    @Override
    public void repelShip(){
        Ship ship = match.getRepellingShip();
        if (!match.getRunningPlayer().isAbleToRepelShip(ship)) {
            throw new IllegalStateException("The running player is not allowed to get to this phase because he can't repel this ship");
        }
        this.match.getTable().discardCard(ship);
        this.match.getRunningPlayer().addShipColorRepelled(ship.getColor());
        this.match.setCurrentPhase(new DiscoverPhase(match));
        this.match.setRepellingShip(null);
        this.match.addNotes(ship.getColor().name() + " " + ship.getClass().getSimpleName() + "#" + ship.getId() + " repelled");
    }

    @Override
    public void acceptShip(){
        Ship ship = match.getRepellingShip();
        if (match.getTable().isBustCase(ship)){//bust case
            match.goBust(ship);
        } else {
            match.getTable().addInHarbor(ship);
            match.setCurrentPhase(new DiscoverPhase(match));
        }
        match.setRepellingShip(null);
        match.addNotes(ship.getColor().name() + " " + ship.getClass().getSimpleName() + "#" + ship.getId() + " accepted");
    }

    @Override
    public void finishDiscovering() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't finish discovering in the repel phase");
    }

    @Override
    public Card tradeHire(int cardIndex, int pickPlayerIndex, boolean renounce) throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't trade/hire in the repel phase");
    }

    @Override
    public void endTurn() throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't end your turn in the repel phase");
    }

    @Override
    public CommitExpeditionResult commitExpedition(int expeditionIndex, List<ExpeditionEmployee> employeesTypes) throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't commit an expedition in the repel phase");
    }

    @Override
    public ContractCard signContract(int contractIndex) throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't sign a contract in the repel phase");
    }
}
