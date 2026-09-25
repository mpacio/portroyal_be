package com.matteopaciolla.portroyal;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.ContractsBoard;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ManualContractCard;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.core.cards.ships.CargoShip;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.core.phases.RepelPhase;
import com.matteopaciolla.portroyal.core.phases.TradeHireMainPhase;
import com.matteopaciolla.portroyal.exceptions.IOGameException;
import com.matteopaciolla.portroyal.exceptions.userinput.UndefinedCommitEmpsListException;
import com.matteopaciolla.portroyal.exceptions.userinput.UserInputException;
import com.matteopaciolla.portroyal.facades.IOFacade;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GamerUI {

    static final Scanner scanner = new Scanner(System.in);
    private final Match match;
    List<MoveRecord> movesBackup = new LinkedList<>();
    private final String movesBackupFileName;

    public GamerUI(Match match, String movesBackupFileName) {
        this.match = match;
        this.movesBackupFileName = movesBackupFileName;
    }

    public void playMatch() {
        boolean gameExited = false;
        while (!gameExited) {
            System.out.println(match);
            if (match.isMatchEnded()){
                System.out.println("The match is ended, no more moves can be done.");
                break;
            }
            int runningPlayerIndex = match.getRunningPlayerIndex();
            int activePlayerIndex = match.getActivePlayerIndex();
            System.out.println("-------------------------------------------------------");
            try {
                System.out.println("Which move you want to do?");
                System.out.println("1. discover");
                System.out.println("2. finish discover");
                System.out.println("3. trade or hire");
                System.out.println("4. end your turn");
                System.out.println("5. commit an expedition");
                System.out.println("6. sign a contract");
                System.out.println("7. renounce a ship");
                System.out.println("8. exit the game and save the moves on file.");
                //read the console input
                int choice = getSinglePositiveIntegerInput(match.getRunningPlayer().getName() + "'s choice: ", 1, 11);
                switch (choice) {
                    case 1: {
                        discover();
                        break;
                    }
                    case 2: {
                        finishDiscovering();
                        break;
                    }
                    case 3: {
                        tradeHire(false);
                        break;
                    }
                    case 4: {
                        endTurn();
                        break;
                    }
                    case 5: {
                        commitExpedition();
                        break;
                    }
                    case 6: {
                        signContract();
                        break;
                    }
                    case 7: {
                        tradeHire(true);
                        break;
                    }
                    case 8: {
                        System.out.println("Exiting the game...");
                        gameExited = true;
                        break;
                    }
                    default:
                        throw new UserInputException("Invalid choice");
                }
                if (choice != 8) {
                    backupMove();
                }
            } catch (UserInputException e) {
                System.err.println("Error: " + e.getMessage());
            } catch (Exception e) {
                // faulty
                // saveMovesOnFile(true);
                e.printStackTrace();
            }
            if (match.getRunningPlayerIndex() != runningPlayerIndex) {
                System.out.println("-->>-->>-->>-->>--The running player changed, new player is " + match.getRunningPlayer().getName() + "-->>-->>-->>-->>--");
            }
            if (match.getActivePlayerIndex() != activePlayerIndex) {
                System.out.println("--<<--<<--<<--<<--The active player changed, new player is " + match.getActivePlayer().getName() + "--<<--<<--<<--<<--");
            }
            System.out.println("=======================================================");
        }
        saveMovesOnFile(false);
    }

    private int getSinglePositiveIntegerInput(String message) throws UserInputException {
        while (true) {
            System.out.print(message);
            String userInput = scanner.nextLine();
            if (!userInput.matches("\\d+")) {
                System.err.println("Invalid input: it must be a single integer");
            } else {
                return Integer.parseInt(userInput);
            }
        }
    }

    private int getSinglePositiveIntegerInput(String message, int bound) throws UserInputException {
        return getSinglePositiveIntegerInput(message, 0, bound);
    }

    private int getSinglePositiveIntegerInput(String message, int downBound, int upBound) throws UserInputException {
        while (true) {
            System.out.print(message);
            String userInput = scanner.nextLine();
            if (!userInput.matches("\\d+")) {
                System.err.println("Invalid input: it must be a single integer");
            } else {
                int choice = Integer.parseInt(userInput);
                if (choice < downBound || choice >= upBound) {
                    System.err.println("Invalid input: it must be an integer between " + downBound + " and " + (upBound - 1));
                } else {
                    return choice;
                }
            }
        }
    }

    private void saveMovesOnFile(boolean backup) {
        List<MoveRecord> movesToBeSaved = backup ? movesBackup : new LinkedList<>(match.getMovesHistory());
        try {
            if (movesBackupFileName != null) {
                IOFacade.saveMovesToCSV(movesToBeSaved, movesBackupFileName);
            } else {
                //the filename is generated by the system with the current datetime in the format moves_yyyyMMdd_HHmmss
                String datetimeFormatted = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now());
                String fileName = "moves_" + datetimeFormatted;
                IOFacade.saveMovesToCSV(movesToBeSaved, fileName);
            }
        } catch (IOGameException e) {
            throw new RuntimeException(e);
        }
    }

    private void backupMove() {
        movesBackup.add(match.getLastMove());
    }

    private void discover() throws UserInputException {
        Card card = match.discover();
        System.out.println("Discovered card " + card);
        if (match.getCurrentPhase() instanceof RepelPhase) {
            System.out.println("-------------------------------------------------------");
            System.out.println(match.getRepelExtraInfoString((Ship) card));
            System.out.println("1. Repel the ship");
            System.out.println("2. Accept the ship");
            int choice = getSinglePositiveIntegerInput("Your choice: ",1,3);
            if (choice == 1) {
                repelShip();
            } else if (choice == 2) {
                acceptShip();
            } else {
                throw new UserInputException("Invalid choice");
            }
        }
    }

    private void repelShip() throws UserInputException {
        match.repelShip();
        System.out.println("Ship repelled");
    }

    private void acceptShip() throws UserInputException {
        match.acceptShip();
        System.out.println("Ship accepted");
    }

    private void finishDiscovering() throws UserInputException {
        match.finishDiscovering();
        System.out.println("Finish discovering");
        if (match.getTable().getHarbor().isEmpty()){
            endTurn();
        }
    }

    private void tradeHire(boolean renounce) throws UserInputException {
        // I check only the TradeHireMainPhase since the TradeHireSubPhase is a child class of TradeHireMainPhase
        if (!(match.getCurrentPhase() instanceof TradeHireMainPhase))
            throw new UserInputException("You can't trade/hire in this phase");
        int index = 0;
        int playerIndex = -1;
        if (match.getTable().getHarbor().size() > 1) {
            index = selectCardToTradeHire(renounce);
            if (index == match.getTable().getHarbor().size()) {
                endTurn();
                return;
            }
        } else {
            System.out.println("You are trading/hiring the card: " + match.getTable().getHarbor().getFirst());
        }
        Card card = match.getTable().getHarbor().get(index);
        if (card instanceof CargoShip) {
            if (match.getPlayers().size() > 2) {
                playerIndex = selectPickPlayerIndex();
            } else {
                // pick the only other player
                playerIndex = match.getRunningPlayerIndex() == 0 ? 1 : 0;
            }
        }
        match.tradeHire(index, playerIndex, renounce);
        System.out.println("Traded/hired card " + card);
        if (card instanceof CargoShip) {
            System.out.println("Extra coin given to player " + match.getPlayers().get(playerIndex).getName());
        }
    }

    private int selectCardToTradeHire(boolean renouncing) throws UserInputException {
        int index = 0;
        Set<Integer> skipIndexes = null;
        System.out.println("Harbor cards:");
        if (renouncing) {
            // create the set of indexes of card instance of employees cards that are to be skipped
            skipIndexes = match.getTable().getHarbor().stream()
                    .filter(card -> card instanceof EmployeeCard)
                    .map(match.getTable().getHarbor()::indexOf)
                    .collect(Collectors.toSet());
            printListSkipping(match.getTable().getHarbor(), skipIndexes, "Employee NOT ALLOWED");
        } else {
            printList(match.getTable().getHarbor());
        }
        System.out.println(match.getTable().getHarbor().size() + ". End trade/hire phase");
        if (renouncing) {
            index = getSinglePositiveIntegerInput("The ship you want to renounce: ", match.getTable().getHarbor().size() + 1); // +1 for the end trade/hire phase
        } else {
            index = getSinglePositiveIntegerInput("The card you want to trade/hire: ", match.getTable().getHarbor().size() + 1); // +1 for the end trade/hire phase
        }
        if (renouncing && skipIndexes.contains(index)) {
            throw new UserInputException("You can't renounce an employee");
        }
        return index;
    }

    private int selectPickPlayerIndex() throws UserInputException {
        System.out.println("Which player you want to prize with an extra coin? ");
        printPlayersNamesListSkipping(match.getPlayers(), match.getRunningPlayerIndex(), " NOT ALLOWED");
        int playerIndex = getSinglePositiveIntegerInput("The player you want to pick: ", match.getPlayers().size());
        if (playerIndex == match.getRunningPlayerIndex()) {
            throw new UserInputException("You can't prize yourself");
        }
        return playerIndex;
    }

    private void commitExpedition() throws UserInputException {
        System.out.println("Expedition cards:");
        printList(match.getTable().getExpeditionCards());
        int index = getSinglePositiveIntegerInput("Which expedition you want to commit? ", match.getTable().getExpeditionCards().size());
        System.out.println("Commit expedition card = " + match.getTable().getExpeditionCards().get(index));
        try {
            match.commitExpedition(index, null);
            System.out.println("Expedition committed");
        } catch (UndefinedCommitEmpsListException e) {
            List<String> empEnumStringList = Stream.of(ExpeditionEmployee.values()).map(ExpeditionEmployee::name).toList();
            List<String> empEnumStringListWithCount = new LinkedList<>();
            for (String s : empEnumStringList) {
                empEnumStringListWithCount.add(s
                        + " (you have: " + match.getRunningPlayer().getEmployees().stream()
                        .filter(emp -> emp.getClass().equals(ExpeditionEmployee.valueOf(s).getReferredClass())).count() + ")");
            }
            printList(empEnumStringListWithCount);
            System.out.print("Which employees you want to use? ");
            String empListResponse = scanner.nextLine();
            if (empListResponse.matches("^(\\d+\\s+){1,2}\\d$"))  { //check with a regex that the empListResponse is a list of integers separated by space
                String[] employees = empListResponse.split(" ");
                List<ExpeditionEmployee> expeditionEmployees = new LinkedList<>();
                for (String employee : employees) {
                    expeditionEmployees.add(ExpeditionEmployee.valueOf(empEnumStringList.get(Integer.parseInt(employee))));
                }
                match.commitExpedition(index, expeditionEmployees);
            } else {
                throw new UserInputException("Invalid employees list: it must be a list of integers (min 2, max 3) separated by space");
            }
        }
    }

    private void signContract() throws UserInputException {
        if (match.getTable().getContractsBoard() == null) {
            throw new UserInputException("Signing contracts is not allowed in this match");
        }
        System.out.println("Contract cards:");
        printManualContractList(match.getTable().getContractsBoard());
        int contractIndex = getSinglePositiveIntegerInput("Which contract you want to sign? ", match.getTable().getContractsBoard().size());
        match.signContract(contractIndex);
        System.out.println("Contract signed");
    }

    private void endTurn() throws UserInputException {
        match.endTurn();
        System.out.println("End of the turn");
    }

    private static void printList(List<?> list) {
        for (int i = 0; i < list.size(); i++) {
            System.out.println(i + ". " + list.get(i));
        }
    }

    private static void printListSkipping(List<?> list, int skipIndex, String skipMessage) {
        for (int i = 0; i < list.size(); i++) {
            if (i != skipIndex) {
                System.out.println(i + ". " + list.get(i));
            } else if (skipMessage != null) {
                System.out.println(i + ". " + skipMessage);
            } else {
                System.out.println(i + ". -");
            }
        }
    }

    private static void printListSkipping(List<?> list, Set<Integer> skipIndexes, String skipMessage) {
        for (int i = 0; i < list.size(); i++) {
            if (!skipIndexes.contains(i)) {
                System.out.println(i + ". " + list.get(i));
            } else if (skipMessage != null) {
                System.out.println(i + ". " + skipMessage);
            } else {
                System.out.println(i + ". -");
            }
        }
    }

    private static void printPlayersNamesListSkipping(List<Player> players, int skipIndex, String skipMessage) {
        for (int i = 0; i < players.size(); i++) {
            if (i != skipIndex) {
                System.out.println(i + ". " + String.format("%s %d" + Emojis.MONEY, players.get(i).getName(), players.get(i).getMoneyValue()));
            } else if (skipMessage != null) {
                System.out.println(i + ". (you)" + skipMessage);
            }
        }
    }

    private static void printManualContractList(ContractsBoard contractsBoard) {
        List<ContractCard> contractCards = contractsBoard.getContracts();
        for (int i = 0; i < contractCards.size(); i++) {
            if (contractCards.get(i) instanceof ManualContractCard) {
                System.out.println(i + ". " + contractCards.get(i).toString());
            } else {
                System.out.println(i + ". " + contractCards.get(i).toString() + " (automatic) NOT ALLOWED");
            }
        }
    }
}