package com.matteopaciolla.portroyal.core.phases;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.Event;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.enums.EventType;
import com.matteopaciolla.portroyal.exceptions.userinput.BadPhaseOperationException;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.exceptions.userinput.NotEnoughMoneyForHiringException;

import java.util.List;

public class TradeHireSubPhase extends TradeHireMainPhase {
    public TradeHireSubPhase(Match match) {
        super(match);
        Player runningPlayer = match.getRunningPlayer();
        runningPlayer.setTradingCapacity(1);
    }

    @Override
    public String getIcon() {
        return Emojis.PHASE_SUB_TRADE;
    }

    @Override
    void tradeShip(Ship ship, int pickPlayerIndex, boolean renounce) {
        super.tradeShip(ship, pickPlayerIndex, renounce);
        payActivePlayerFee();
    }

    @Override
    void hireEmployee(EmployeeCard employeeCard) throws NotEnoughMoneyForHiringException {
        if (match.getRunningPlayer().canAffordAnyHiring(match.getTable().getHarbor(), false)) {
            if (match.getRunningPlayer().canAffordHiring(employeeCard, false)) {
                super.hireEmployee(employeeCard);
                payActivePlayerFee();
            } else {
                throw new NotEnoughMoneyForHiringException("You don't have enough money to hire this employee and pay the active player's fee");
            }
        } else {
            throw new NotEnoughMoneyForHiringException("You don't have enough money to hire any employee and pay the active player's fee");
        }
    }

    @Override
    public CommitExpeditionResult commitExpedition(int expeditionIndex, List<ExpeditionEmployee> employeesTypes) throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't commit an expedition during a trade/hire phase in which you are not the active player");
    }

    @Override
    public ContractCard signContract(int contractIndex) throws BadPhaseOperationException {
        throw new BadPhaseOperationException("You can't sign a contract during a trade/hire phase in which you are not the active player");
    }

    private void payActivePlayerFee() {
        match.getActivePlayer().addMoney(match.getRunningPlayer().removeACoin());
        match.addNotes(match.getRunningPlayer().getName() + " paid fee to " + match.getActivePlayer().getName());
        match.getSideEvents().add(new Event(EventType.GOT_AP_FEE, match.getActivePlayer(), 1));
    }
}
