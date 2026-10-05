package com.matteopaciolla.portroyal.core.phases;

import com.matteopaciolla.portroyal.core.Deck;
import com.matteopaciolla.portroyal.core.ContractsBoard;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.cards.employees.Handyman;
import com.matteopaciolla.portroyal.exceptions.userinput.*;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.core.cards.expeditions.Expedition;
import lombok.Value;

import java.util.List;
import java.util.Map;

public abstract class Phase {

    final Match match;

    public Phase(Match match) {
        this.match = match;
    }

    public abstract String getIcon();

    @Override
    public String toString() {
        return this.getIcon() + this.getClass().getSimpleName();
    }

    public abstract Card discover() throws BadPhaseOperationException;

    public abstract void repelShip() throws BadPhaseOperationException;

    public abstract void acceptShip() throws BadPhaseOperationException;

    public abstract void finishDiscovering() throws BadPhaseOperationException;

    public abstract Card tradeHire(int cardIndex, int pickPlayerIndex, boolean renounce) throws UserInputException;

    public abstract void endTurn() throws BadPhaseOperationException;

    public CommitExpeditionResult commitExpedition(int expeditionIndex, List<ExpeditionEmployee> employeesTypes) throws UserInputException {
        // check on preconditions
        if (match.getTable().getExpeditionCards().isEmpty()) {
            throw new UserInputException("No expeditions to commit");
        }
        if (expeditionIndex < 0 || expeditionIndex >= match.getTable().getExpeditionCards().size()) {
            throw new UserInputException("Invalid expedition index");
        }
        if (match.getRunningPlayer().getEmployees().isEmpty()) {
            throw new NoEmpsForCommitException("You don't have any employees");
        }
        Expedition expedition = match.getTable().getExpeditionCards().get(expeditionIndex);
        List<ExpeditionEmployee> definitiveEmployeesTypes;
        //---------------------check the employeesTypes list parameter---------------------
        if (employeesTypes != null && !employeesTypes.isEmpty()) {
            // the user has chosen a list of employees types
            //create a map to associate each employee type with the number of occurrences in the list
            Map<ExpeditionEmployee, Integer> employeesTypeCount = Expedition.getEmpTypesCountFromTypesList(employeesTypes);
            //for every entry in the map, check if the player has the required number of type for each employee
            for (Map.Entry<ExpeditionEmployee, Integer> entry : employeesTypeCount.entrySet()) {
                if (match.getRunningPlayer().getEmployees().stream()
                        .filter(employee -> employee.getClass().equals(entry.getKey().getReferredClass()))
                        .count() < entry.getValue()) {
                    throw new NoEmpsForCommitException("You don't have the employees that you wanna use");
                }
            }
            // check that the given employeesTypes list is enough to commit the expedition
            if (!expedition.isCommittable(employeesTypeCount)) {
                throw new NoEmpsForCommitException("The employees you have chosen are not enough to commit the expedition");
            }
            definitiveEmployeesTypes = employeesTypes;
        } else {
            // the user has not chosen any employee type, the selection will be made by the system
            if (expedition.isPlayerAbleToCommitExpedition(match.getRunningPlayer(), true)) {
                // the player can commit the expedition potentially using handymen
                if (match.getRunningPlayer().getEmployeeClassNumber(Handyman.class) > 0) {
                    // the player has handymen in his display
                    if (expedition.isPlayerAbleToCommitExpedition(match.getRunningPlayer(), false)) {
                        // the player can commit the expedition without using handymen
                        // since the player has handymen, and he has not chosen any employee type, throw an exception
                        // because there are multiple ways to commit the expedition
                        throw new UndefinedCommitEmpsListException("You have to choose the employees to commit the expedition since you have handymen in your display");
                    } else {
                        // the player can commit the expedition only using handymen
                        // lets create the definitiveEmployeesTypes list using the right ExpeditionEmployee enum if present or a handyman otherwise
                        definitiveEmployeesTypes = expedition.getPossibleEmployeesTypesList(match.getRunningPlayer());
                    }
                } else {
                    // the player has not handymen in his display but he can commit the expedition
                    definitiveEmployeesTypes = expedition.getNeededEmployeesTypesList();
                }
            } else {
                // the player can't commit the expedition
                throw new NoEmpsForCommitException("You have not the requirements to commit this expedition");
            }
        }
        Deck<Card> usedCards = match.getRunningPlayer().commitExpedition(expedition, definitiveEmployeesTypes);
        match.getTable().discardCards(usedCards);
        match.getTable().getExpeditionCards().remove(expedition);
        match.getRunningPlayer().addMoney(match.getTable().getMoney(expedition.getMoney()));
        match.addNotes(match.getRunningPlayer().getName() + " got " + expedition.getMoney() + " money from the expedition" + "#" + expedition.getId());
        return new CommitExpeditionResult(definitiveEmployeesTypes, usedCards.toList());
    }

    public ContractCard signContract(int contractIndex) throws UserInputException {
        if (match.getTable().getContractsBoard() == null) {
            throw new UserInputException("No contracts board available in this match");
        }
        int totalContracts = match.getTable().getContractsBoard().size();
        if (contractIndex < 0 || contractIndex >= totalContracts) {
            throw new UserInputException("Invalid contract index");
        }
        ContractsBoard contractsBoard = match.getTable().getContractsBoard();
        if (!contractsBoard.hasAvailableContractSlot(match.getRunningPlayer())) {
            throw new MaxContractsNumberException("You have reached the maximum number of contracts");
        }
        ContractCard contract = contractsBoard.signManualContract(contractIndex, match.getRunningPlayer(), match.getTable());
        match.addNotes(match.getRunningPlayer().getName() + " signed " + contract.getName() + "#" + contract.getId());
        return contract;
    }

    @Value
    public static class CommitExpeditionResult {
        List<ExpeditionEmployee> employeesUsed;
        List<Card> cardsDiscarded;
    }

}
