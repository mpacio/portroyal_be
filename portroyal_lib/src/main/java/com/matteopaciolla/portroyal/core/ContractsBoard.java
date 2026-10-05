package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.core.cards.contracts.abst.AutomaticContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ManualContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ProgressiveContract;
import com.matteopaciolla.portroyal.core.enums.EventType;
import com.matteopaciolla.portroyal.exceptions.userinput.MaxContractsNumberException;
import com.matteopaciolla.portroyal.exceptions.userinput.UserInputException;

import java.util.*;

public class ContractsBoard {

    private final ContractCard[] contracts;

    private final Map<ContractCard, Queue<Player>> contractSpotsMap = new HashMap<>();

    private final Map<ContractCard, Set<Player>> contractsInProgressMap = new HashMap<>();

    private final int maxContractsPerPlayer;

    private final int playersCount;

    public ContractsBoard(List<ContractCard> contracts, int maxContractsPerPlayer, int playersCount) {
        this.contracts = contracts.toArray(new ContractCard[0]);
        this.maxContractsPerPlayer = maxContractsPerPlayer;
        this.playersCount = playersCount;
        for (ContractCard contract : contracts) {
            contractSpotsMap.put(contract, new LinkedList<>());
            contractsInProgressMap.put(contract, new HashSet<>());
        }
    }

    public int size() {
        return contracts.length;
    }

    public boolean isFull(int contractIndex) {
        ContractCard contract = contracts[contractIndex];
        return contractSpotsMap.get(contract).size() == playersCount;
    }

    public List<Player> getSignedPlayers(int contractIndex) {
        ContractCard contract = contracts[contractIndex];
        return getSignedPlayers(contract);
    }

    public List<Player> getSignedPlayers(ContractCard contract) {
        return contractSpotsMap.get(contract).stream().toList();
    }

    /**
     * Get the number of contracts in progress for a player
     * @param player the player to check
     * @return the number of contracts in progress for the player
     */
    public int getContractsInProgressCount(Player player) {
        return contractsInProgressMap.values().stream()
                .mapToInt(players -> players.contains(player) ? 1 : 0)
                .sum();
    }

    /**
     * Update the contracts in progress for a player
     * If the player has started signing a progressive contract, and the contract is not full, and the player has an available contract slot, add the player to the contracts in progress for that contract
     * @param player the player to update
     */
    public void updateAutomaticContractProgress(Player player) {
        for (int i = 0; i < contracts.length; i++) {
            ContractCard contract = contracts[i];
            if (contract instanceof ProgressiveContract progressiveContract
                    && progressiveContract.hasStartedSigning(player)
                    && !contractSpotsMap.get(contract).contains(player)
                    && !contractsInProgressMap.get(contract).contains(player)
                    && !isFull(i)
                    && hasAvailableContractSlot(player)) {
                contractsInProgressMap.get(contract).add(player);
            }
        }
    }

    /**
     * Check if the player has an available contract slot
     * @param player the player to check
     * @return true if the player has an available contract slot, false otherwise
     */
    public boolean hasAvailableContractSlot(Player player) {
        return player.getContractsCompleted() + getContractsInProgressCount(player) < maxContractsPerPlayer;
    }

    /**
     * Sign a contract for a player
     * Conditions for signing a contract:
     * - the contractIndex is a valid index
     * - the player has not reached the maximum number of contracts
     * - the contract is manual
     * - the player is not already signed in the contract
     * - the contract is not full
     * - the player meets the requirements for the contract
     * @param contractIndex the index of the contract to sign (must be a valid index)
     * @param player the player that is signing the contract
     * @param table the table where the game is being played
     */
    public ContractCard signManualContract(int contractIndex, Player player, Table table) throws UserInputException {
        assert contractIndex >= 0 && contractIndex < contracts.length;
        ContractCard contract = contracts[contractIndex];
        if (contract instanceof AutomaticContractCard) {
            throw new UserInputException("You can't manually sign an automatic contract");
        }
        ManualContractCard manualContract = (ManualContractCard) contract;
        if (!hasAvailableContractSlot(player)) {
            throw new MaxContractsNumberException("You have reached the maximum number of contracts");
        }
        if (isFull(contractIndex)) {
            throw new UserInputException("This contract is already full");
        }
        if (contractSpotsMap.get(manualContract).contains(player)) {
            throw new UserInputException("You have already signed this contract");
        }
        if (!manualContract.requirementsMet(player)) {
            throw new UserInputException("You don't meet the requirements for this contract");
        }
        signContract(contractIndex, player, table);
        return manualContract;
    }

    /**
     * Sign all the automatic contracts that the player meets the requirements for
     * Uses an available slot or a slot already reserved by progress on that contract.
     *
     * @param player the player that is signing the contracts
     * @param match  the match that is being played
     */
    public void signAutomaticContracts(Player player, Match match) {
        for (int i = 0; i < contracts.length; i++) {
            ContractCard contractCard = contracts[i];
            if (contractCard instanceof AutomaticContractCard // the contract is automatic
                    && !isFull(i)// the contract is not full
                    && !contractSpotsMap.get(contractCard).contains(player)// the player is not already signed in the contract
                    && contractCard.requirementsMet(player)// the player meets the requirements for the contract
                    && (contractsInProgressMap.get(contractCard).contains(player) || hasAvailableContractSlot(player))
            ) {
                ContractSigned contractSigned = signContract(i, player, match.getTable());
                if (contractSigned.contractCard instanceof AutomaticContractCard) {
                    match.addNotes(player.getName() + " auto signed " + contractSigned.contractCard.getName() + "#" + contractSigned.contractCard.getId());
                    match.getSideEvents().add(new Event(EventType.AUTO_SIGNED_CONTRACT, player, contractSigned.reward, contractSigned.contractCard));
                }
            }
        }
    }

    private ContractSigned signContract(int contractIndex, Player player, Table table) {
        ContractCard contract = contracts[contractIndex];
        Queue<Player> contractQueue = contractSpotsMap.get(contract);
        contractQueue.offer(player);
        contractsInProgressMap.get(contract).remove(player);
        int index = contractQueue.size() - 1; // the index of the player in the contract's queue
        int reward = contracts[contractIndex].getRewards()[index];
        player.addMoney(table.getMoney(reward));
        player.increaseContractsCompleted();
        return new ContractSigned(contract, reward);
    }

    /**
     * Get a String representation of the contracts board
     * the String will look like this:<br>
     * C.0: (ContName) R=[1, 2, 3, 3, 4] Players=[michael, ross, eve, _, _]<br>
     * C.1: (ContName) R=[1, 2, 3, 0, 0] Players=[ross, eve, _, _, _]<br>
     * C.2: (ContName) R=[3, 3, 3, 3, 3] Players=[michael, ross, _, _, _]<br>
     * C.3: (ContName) R=[1, 0, 0, 0, 0] Players=[eve, ross, michael, _, _]<br>
     * ...
     * where C.i is the i-th contract,
     * ContractName is the name of the contract,
     * R is the rewards array each number represents the reward for the player in that slot,
     * Players is the array of players, each player is represented by his name,
     * the '_' character represents an empty slot
     * @return a String representation of the contracts board
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contracts.length; i++) {
            ContractCard contract = contracts[i];
            sb.append("C.").append(i).append(": (").append(contract.getName()).append(") R=").append(Arrays.toString(contract.getRewards()))
                    .append(" Players=").append(getSignedPlayers(i).stream().map(Player::getName).toList()).append("\n");
        }
        return sb.toString();
    }

    public List<ContractCard> getContracts() {
        return Arrays.asList(contracts);
    }

    private record ContractSigned(ContractCard contractCard, int reward) {}
}
