package com.matteopaciolla.portroyal.core;

import java.util.*;

import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ManualContractCard;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;
import com.matteopaciolla.portroyal.core.cards.expeditions.Expedition;
import com.matteopaciolla.portroyal.core.cards.ships.CargoShip;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.core.effects.JesterEffect;
import com.matteopaciolla.portroyal.core.enums.BotDifficulty;
import com.matteopaciolla.portroyal.core.enums.EventType;
import com.matteopaciolla.portroyal.core.enums.MoveAction;
import com.matteopaciolla.portroyal.core.phases.*;
import com.matteopaciolla.portroyal.exceptions.internal.*;
import com.matteopaciolla.portroyal.exceptions.userinput.*;
import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Match {

    private final List<Player> players;
    //the index of the player that owns the turn, he is the only one that can end the turn
    private int activePlayerIndex = 0;
    //the index of the player that is currently playing and that is executing the actions
    private int runningPlayerIndex = 0;
    private final Table table;
    @Setter
    private Phase currentPhase;
    private final Random random;
    private final Configuration configuration;
    private final Deque<MoveRecord> movesHistory = new LinkedList<>();
    private boolean setupDone;
    @Setter
    private boolean finalTurn = false;
    private boolean matchEnded = false;
    private int firstActivePlayerIndex;
    private int lastActivePlayerIndex;
    @Setter
    private Ship repellingShip;
    @Getter(AccessLevel.NONE)
    private StringBuilder lastMoveNotes = new StringBuilder();
    @Setter
    private Deck<ContractCard> contractsDeck;
    private int lastDrawPileSize = -1;
    private final List<Event> sideEvents = new LinkedList<>();
    @Setter
    @Getter
    private Event mainEvent = null;


    public Match(Random random, Deck<Card> deck, List<Player> players, Configuration configuration) {
        this.random = random;
        this.players = players;
        this.table = new Table(deck);
        this.configuration = configuration;
    }

    public int getMovesCount() {
        return movesHistory.size();
    }

    public MoveRecord getLastMove() {
        return movesHistory.peekLast();
    }

    @Override
    public String toString() {
        if (!setupDone) {
            return "Match not set up yet";
        }
        if (matchEnded) {
            try {
                return String.format("Match: moves = %d, The winner is %s", getMovesCount(), getWinner().getName()) + Emojis.TROPHY;
            } catch (UserInputException ignored) {}
        }
        return String.format("Match: moves = %d, lastMove = ", getMovesCount()) + getLastMove() + " {"
                + "\n" + table.getContractsBoard()
                + "\nexpeditions(" + table.getExpeditionCards().size() + ")=" + table.getExpeditionCards()
                + "\nharbor(" + table.getHarbor().size() + ")(" + table.getShipsInHarborNumber() + Emojis.SHIP + ")=" + table.getHarbor()
                + getAllPlayersString()
                + "\ncurrentPhase = " + currentPhase + getPhaseExtraInfoString()
                + "\n       draw|disc|harb|exps|aplc|r|total"
                + String.format("\ncards: %4s+%4s+%4s+%4s+%4s+%d=%d",
                        table.getDrawPileSize(),
                        table.getDiscardPileSize(),
                        table.getHarbor().size(),
                        table.getExpeditionCards().size(),
                        getAllPlayersCardsNumber(),
                        repellingShip == null ? 0 : 1,
                        getAllTableCardsNumber()
                ) + " }";
    }

    public void changeActivePlayer(){
        this.activePlayerIndex = (this.activePlayerIndex + 1) % this.players.size();
    }

    public void changeRunningPlayer(){
        this.runningPlayerIndex = (this.runningPlayerIndex + 1) % this.players.size();
    }

    public void synchronizeRunningPlayer(){
        this.runningPlayerIndex = this.activePlayerIndex;
    }

    public Player getActivePlayer(){
        return this.players.get(activePlayerIndex);
    }

    public Player getRunningPlayer(){
        return this.players.get(runningPlayerIndex);
    }

    public boolean isRunningTheActivePlayer(){
        return this.activePlayerIndex == this.runningPlayerIndex;
    }

    public boolean isActivePlayer(String playerName){
        return playerName.equals(this.getActivePlayer().getName());
    }

    public boolean isRunningPlayer(String playerName) {
        return playerName.equals(this.players.get(runningPlayerIndex).getName());
    }

    public Player getWinner() throws UserInputException {
        if (!matchEnded) {
            throw new UserInputException("Match not ended yet");
        }
        return this.players.stream().filter(Player::isFinalTurnCondition).max(Player::compareTo).orElse(null);
    }

    public int getAllPlayersCardsNumber() {
        return getPlayers().stream().mapToInt(player ->
                player.getEmployees().size()
                + player.getMoneyValue()
                + player.getExpeditionCards().size()
        ).sum();
    }

    public int getAllTableCardsNumber() {
        return getTable().getDrawPileSize()
                + getTable().getDiscardPileSize()
                + getTable().getHarbor().size()
                + getTable().getExpeditionCards().size()
                + (repellingShip == null ? 0 : 1)
                + getAllPlayersCardsNumber();
    }

    public String getAllPlayersString() {
        StringBuilder playersString = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            playersString.append("\n").append(i);
            if (i == activePlayerIndex && i == runningPlayerIndex) {
                playersString.append("a/r");
            } else if (i == activePlayerIndex) {
                playersString.append("act");
            } else if (i == runningPlayerIndex) {
                playersString.append("run");
            } else {
                playersString.append("   ");
            }
            playersString.append("=").append(players.get(i));
        }
        return playersString.toString();
    }

    public String getPhaseExtraInfoString() {
        switch (currentPhase) {
            case RepelPhase repelPhase -> {
                return getRepelExtraInfoString(repellingShip);
            }
            case TradeHireSubPhase tradeHireSubPhase -> {
                return getSubTradeExtraInfoString();
            }
            case TradeHireMainPhase tradeHireMainPhase -> {
                return getRunningAndActivePlayerString();
            }
            case DiscoverPhase discoverPhase -> {
                return getRunningAndActivePlayerString();
            }
            default -> {
                return "";
            }
        }
    }

    public String getSubTradeExtraInfoString() {
        return "    " + Emojis.JOGGING + getRunningPlayer().getName() +
                " >> " + Emojis.CROWN + getActivePlayer().getName();
    }

    public String getRunningAndActivePlayerString() {
        if (runningPlayerIndex != activePlayerIndex) throw new IllegalStateException("The running player is not the active player");
        return "    " + Emojis.JOGGING + getRunningPlayer().getName() + Emojis.CROWN;
    }

    public String getRepelExtraInfoString(Ship ship) {
        return "  ship (" + ship + ") can be repelled";
    }

    public void setup(List<MoveRecord> movesHistory) throws InternalGameException {
        if (setupDone) {
            throw new InternalGameException("Match already set up");
        }
        this.table.shuffleDrawPile();
        if (configuration.isFirstPlayerRandomlyChosen()){
            this.activePlayerIndex = random.nextInt(this.players.size());
        }
        if (configuration.isJOMC_ExpansionUsed()) {
            contractsDeck.shuffle();
            this.table.setContractsBoard(new ContractsBoard(
                    contractsDeck.getFirst(configuration.getContractsCardNumber()).toList(),
                    configuration.getMaxContractsCompletablePerPlayer(),
                    this.players.size()));
        }
        this.firstActivePlayerIndex = this.activePlayerIndex;
        this.lastActivePlayerIndex = (firstActivePlayerIndex + players.size() - 1) % players.size();
        synchronizeRunningPlayer();
        for (int i = 0; i < this.players.size(); i++) {
            this.changeActivePlayer();
            this.getPlayers().get(this.activePlayerIndex).setup(this.getTable().getMoney(configuration.getInitialCoins()));
        }
        this.currentPhase = new DiscoverPhase(this);
        if (movesHistory != null) {
            try {
                setMovesHistory(new LinkedList<>(movesHistory));
            } catch (MoveExecutingException e) {
                throw new InternalGameException("Error in executing moves history", e);
            }
        }
        setupDone = true;
    }

    public void goBust(Ship ship) {
        if (this.getTable().isBustCase(ship)) {
            addNotes("Bust case");
            getRunningPlayer().setWentBust(true);
            JesterEffect.prizePlayers(this);
            this.getTable().discardCard(ship);
            this.getTable().discardHarbor();
            evaluateContracts();
            this.startNewTurn();
        } else {
            throw new IllegalStateException("This is not a bust case");
        }
    }

    public void endMatch() {
        matchEnded = true;
    }

    private void setMovesHistory(Deque<MoveRecord> movesHistory) throws MoveExecutingException {
        int timeIndex = 0;
        for (MoveRecord moveRecord : movesHistory) {
            if (moveRecord.getRunningPlayerIndex() != runningPlayerIndex)
                throw new MoveExecutingException(moveRecord, "The player that tried executing the move is wrong. " +
                        "The one that should have been executing it is the #" + runningPlayerIndex + " "
                        + getRunningPlayer().getName());
            if (moveRecord.getTimeIndex() != timeIndex)
                throw new MoveExecutingException(moveRecord, "The time index is wrong. " +
                        "The expected time index is " + timeIndex);
            try {
                executeMove(moveRecord);
            } catch (UserInputException e) {
                throw new MoveExecutingException(moveRecord, e.getMessage(), e);
            }
            timeIndex++;
        }
    }

    public void executeMove(MoveRecord moveRecord) throws UserInputException {
        if (moveRecord.getRunningPlayerIndex() != runningPlayerIndex) {
            throw new WrongRunningPlayerException("The player that executed the move is not the same as the one that should execute it");
        }
        switch (moveRecord.getMove()) {
            case DISCOVER -> discover();
            case REPEL -> repelShip();
            case ACCEPT -> acceptShip();
            case FINISH_DISCOVER -> finishDiscovering();
            case TRADE_HIRE, TRADE, HIRE -> tradeHire(moveRecord.getChoiceIndex(), moveRecord.getPickPlayerIndex(), false);
            case TRADE_RENOUNCE -> tradeHire(moveRecord.getChoiceIndex(), moveRecord.getPickPlayerIndex(), true);
            case COMMIT_EXPEDITION -> commitExpedition(moveRecord.getChoiceIndex(), moveRecord.getExpeditionEmployees());
            case SIGN_CONTRACT -> signContract(moveRecord.getChoiceIndex());
            case END_TURN -> endTurn();
        }
    }

    public void evaluateContracts() {
        if (!configuration.isJOMC_ExpansionUsed() || table.getContractsBoard() == null) {
            return; // no contracts to evaluate
        }
        // check every player starting from the running player
        for (int i = runningPlayerIndex; i < runningPlayerIndex + players.size(); i++) {
            Player player = players.get(i % players.size());
            table.getContractsBoard().signAutomaticContracts(player, this);
        }
    }

    public void updateAutomaticContractProgress(Player player) {
        if (configuration.isJOMC_ExpansionUsed() && table.getContractsBoard() != null) {
            table.getContractsBoard().updateAutomaticContractProgress(player);
        }
    }

    public void startNewTurn() {
        changeActivePlayer();
        synchronizeRunningPlayer();
        if (isFinalTurn() && activePlayerIndex == firstActivePlayerIndex) {
            endMatch();
            setCurrentPhase(null);
        } else {
            setCurrentPhase(new DiscoverPhase(this));
        }
    }

    public void addNotes(String notes) {
        if (!lastMoveNotes.isEmpty()) {
            lastMoveNotes.append(", ");
        }
        lastMoveNotes.append(notes);
    }

    private String popNotes() {
        String notes = null;
        if (!lastMoveNotes.isEmpty()) {
            notes = lastMoveNotes.toString();
            lastMoveNotes = new StringBuilder();
        }
        return notes;
    }

    private List<Event> popSideEvents() {
        List<Event> res = new ArrayList<>(sideEvents);
        sideEvents.clear();
        return res;
    }

    private Event popMainEvent() {
        Event event = mainEvent;
        mainEvent = null;
        return event;
    }

    //|-----------------MOVES-----------------|
    //V---------------------------------------V

    public Card discover() throws UserInputException {
        if (matchEnded) throw new MoveEndedMatchException();
        // store the index of the player that is making the move before changing anything
        // the running player index does not change in this move but for consistency we store it here
        int movingPlayer = this.runningPlayerIndex;
        if (lastDrawPileSize > 0 && getTable().getDrawPileSize() > lastDrawPileSize) {
            addNotes("Draw pile refreshed");
        }
        Card card = this.currentPhase.discover();
        mainEvent = new Event(EventType.DISCOVERED_CARD, players.get(movingPlayer), card);
        MoveRecord move = new MoveRecord(movesHistory.size(), movingPlayer, MoveAction.DISCOVER, popNotes(), popMainEvent(), popSideEvents());
        movesHistory.add(move);
        lastDrawPileSize = getTable().getDrawPileSize();
        return card;
    }

    public void repelShip() throws UserInputException {
        if (matchEnded) throw new MoveEndedMatchException();
        // store the index of the player that is making the move before changing anything
        // the running player index does not change in this move but for consistency we store it here
        int movingPlayer = this.runningPlayerIndex;
        this.currentPhase.repelShip();
        mainEvent = new Event(EventType.REPELLED_SHIP, players.get(movingPlayer), repellingShip);
        MoveRecord move = new MoveRecord(movesHistory.size(), movingPlayer, MoveAction.REPEL, popNotes(), popMainEvent(), popSideEvents());
        movesHistory.add(move);
    }

    public void acceptShip() throws UserInputException {
        if (matchEnded) throw new MoveEndedMatchException();
        // store the index of the player that is making the move before changing anything
        // the running player index does not change in this move but for consistency we store it here
        int movingPlayer = this.runningPlayerIndex;
        this.currentPhase.acceptShip();
        mainEvent = new Event(EventType.ACCEPTED_SHIP, players.get(movingPlayer), repellingShip);
        MoveRecord move = new MoveRecord(movesHistory.size(), movingPlayer, MoveAction.ACCEPT, popNotes(), popMainEvent(), popSideEvents());
        movesHistory.add(move);
    }

    public void finishDiscovering() throws UserInputException {
        if (matchEnded) throw new MoveEndedMatchException();
        // store the index of the player that is making the move before changing anything
        // the running player index does not change in this move but for consistency we store it here
        int movingPlayer = this.runningPlayerIndex;
        this.currentPhase.finishDiscovering();
        MoveRecord move = new MoveRecord(movesHistory.size(), movingPlayer, MoveAction.FINISH_DISCOVER, popNotes(), popMainEvent(), popSideEvents());
        movesHistory.add(move);
    }

    public void tradeHire(int cardIndex, int pickPlayerIndex, boolean renounce) throws UserInputException {
        if (matchEnded) throw new MoveEndedMatchException();
        // store the index of the player that is making the move before changing anything
        int movingPlayer = this.runningPlayerIndex;
        MoveAction actualMove = MoveAction.TRADE_HIRE;
        EventType eventType = null;
        Card card = this.currentPhase.tradeHire(cardIndex, pickPlayerIndex, renounce);
        if (card instanceof Ship ship) {
            actualMove = renounce ? MoveAction.TRADE_RENOUNCE : MoveAction.TRADE;
            eventType = renounce ? EventType.RENOUNCED_SHIP : EventType.TRADED_SHIP;
        } else if (card instanceof EmployeeCard employeeCard) {
            actualMove = MoveAction.HIRE;
            eventType = EventType.HIRED_EMPLOYEE;
        }
        // the current running player may have changed during the tradeHire phase
        mainEvent = new Event(eventType, players.get(movingPlayer), card);
        MoveRecord move = new MoveRecord(movesHistory.size(), movingPlayer, actualMove, cardIndex, pickPlayerIndex, popNotes(), popMainEvent(), popSideEvents());
        movesHistory.add(move);
    }

    public void commitExpedition(int expeditionIndex, List<ExpeditionEmployee> employees) throws UserInputException {
        if (matchEnded) throw new MoveEndedMatchException();
        // store the index of the player that is making the move before changing anything
        // the running player index does not change in this move but for consistency we store it here
        int movingPlayer = this.runningPlayerIndex;
        Card expeditionCard = this.getTable().getExpeditionCards().get(expeditionIndex);
        Phase.CommitExpeditionResult result = this.currentPhase.commitExpedition(expeditionIndex, employees);
        List<ExpeditionEmployee> actualUsedEmpTypes = result.getEmployeesUsed();
        List<Card> cards = new ArrayList<>();
        cards.add(expeditionCard);
        cards.addAll(result.getCardsDiscarded());
        mainEvent = new Event(EventType.COMMITTED_EXPEDITION, players.get(movingPlayer), cards);
        MoveRecord move = new MoveRecord(movesHistory.size(), movingPlayer, MoveAction.COMMIT_EXPEDITION, expeditionIndex, actualUsedEmpTypes, popNotes(), popMainEvent(), popSideEvents());
        movesHistory.add(move);
    }

    public void signContract(int contractIndex) throws UserInputException {
        if (matchEnded) throw new MoveEndedMatchException();
        // store the index of the player that is making the move before changing anything
        // the running player index does not change in this move but for consistency we store it here
        int movingPlayer = this.runningPlayerIndex;
        if (!configuration.isJOMC_ExpansionUsed()) throw new UserInputException("Contracts not available in this match");
        ContractCard signedContract = this.currentPhase.signContract(contractIndex);
        mainEvent = new Event(EventType.SIGNED_CONTRACT, players.get(movingPlayer), signedContract);
        MoveRecord move = new MoveRecord(movesHistory.size(), movingPlayer, MoveAction.SIGN_CONTRACT, contractIndex, popNotes(), popMainEvent(), popSideEvents());
        movesHistory.add(move);
    }

    public void endTurn() throws UserInputException {
        if (matchEnded) throw new MoveEndedMatchException();
        // store the index of the player that is ending the turn before changing it
        int movingPlayer = this.runningPlayerIndex;
        this.currentPhase.endTurn();
        // now the running player index has changed
        evaluateContracts();
        MoveRecord move = new MoveRecord(movesHistory.size(), movingPlayer, MoveAction.END_TURN, popNotes(), popMainEvent(), popSideEvents());
        movesHistory.add(move);
    }

    //^-----------------MOVES-----------------^
    //|---------------------------------------|

    public boolean comparePhase(Class<? extends Phase> phaseClass) {
        return currentPhase.getClass().equals(phaseClass);
    }

    /**
     * Calculates the next move record for the bot player based on the current game state and the specified difficulty level.
     * The method decides and executes exactly one atomic move (the same way a human player would trigger one of the
     * public move methods), appending the resulting {@link MoveRecord} to the moves history and returning it.
     * Callers are expected to invoke this method repeatedly until the bot's turn ends.
     *
     * @param botDifficulty The difficulty level of the bot (EASY, MEDIUM, HARD).
     * @return The calculated MoveRecord representing the bot's next move, or null if the match already ended.
     */
    public MoveRecord calculateNextMoveRecord(BotDifficulty botDifficulty) {
        if (matchEnded || currentPhase == null) {
            return null;
        }
        try {
            return switch (currentPhase) {
                case RepelPhase repelPhase -> calculateRepelMove(botDifficulty);
                // TradeHireSubPhase must be checked before TradeHireMainPhase since it extends it
                case TradeHireSubPhase tradeHireSubPhase -> calculateTradeHireMove(botDifficulty, false);
                case TradeHireMainPhase tradeHireMainPhase -> calculateTradeHireMove(botDifficulty, true);
                case DiscoverPhase discoverPhase -> calculateDiscoverMove(botDifficulty);
                default -> throw new IllegalStateException("Unknown phase, can't calculate a bot move: " + currentPhase);
            };
        } catch (UserInputException e) {
            throw new IllegalStateException("Bot was not able to compute a valid move for difficulty " + botDifficulty, e);
        }
    }

    private MoveRecord calculateRepelMove(BotDifficulty botDifficulty) throws UserInputException {
        Ship ship = repellingShip;
        boolean shouldRepel = table.isBustCase(ship);
        if (!shouldRepel && botDifficulty == BotDifficulty.HARD) {
            // Repelling a not-yet-repelled color works toward "ship colors repelled" contracts;
            // only worth giving up the harbor opportunity once it already holds some value and the ship is a real threat.
            shouldRepel = configuration.isJOMC_ExpansionUsed()
                    && !getRunningPlayer().getShipColorsRepelled().contains(ship.getColor())
                    && table.getShipsInHarborNumber() >= 2
                    && ship.getPower() >= 3;
        }
        if (shouldRepel) {
            repelShip();
        } else {
            acceptShip();
        }
        return getLastMove();
    }

    private MoveRecord calculateDiscoverMove(BotDifficulty botDifficulty) throws UserInputException {
        Player runningPlayer = getRunningPlayer();
        OptionalInt expeditionIndex = pickBestCommittableExpeditionIndex(runningPlayer, botDifficulty);
        if (expeditionIndex.isPresent()) {
            return performCommitExpedition(expeditionIndex.getAsInt(), runningPlayer);
        }
        OptionalInt contractIndex = pickBestSignableContractIndex(runningPlayer);
        if (contractIndex.isPresent()) {
            signContract(contractIndex.getAsInt());
            return getLastMove();
        }
        if (shouldKeepDiscovering(runningPlayer, botDifficulty)) {
            discover();
        } else {
            finishDiscovering();
        }
        return getLastMove();
    }

    private MoveRecord calculateTradeHireMove(BotDifficulty botDifficulty, boolean isActivePlayerTurn) throws UserInputException {
        Player runningPlayer = getRunningPlayer();
        if (isActivePlayerTurn) {
            OptionalInt expeditionIndex = pickBestCommittableExpeditionIndex(runningPlayer, botDifficulty);
            if (expeditionIndex.isPresent()) {
                return performCommitExpedition(expeditionIndex.getAsInt(), runningPlayer);
            }
            OptionalInt contractIndex = pickBestSignableContractIndex(runningPlayer);
            if (contractIndex.isPresent()) {
                signContract(contractIndex.getAsInt());
                return getLastMove();
            }
        }
        OptionalInt harborIndex = runningPlayer.getTradingCapacity() > 0
                ? pickBestHarborCardIndex(runningPlayer, isActivePlayerTurn, botDifficulty)
                : OptionalInt.empty();
        if (harborIndex.isPresent()) {
            Card card = table.getHarbor().get(harborIndex.getAsInt());
            int pickPlayerIndex = card instanceof CargoShip ? pickCargoBeneficiaryIndex() : -1;
            tradeHire(harborIndex.getAsInt(), pickPlayerIndex, false);
        } else {
            endTurn();
        }
        return getLastMove();
    }

    /**
     * Decides whether the bot should keep drawing cards during the discover phase or stop and move on to trading.
     * The risk of a bust grows with the number of distinct ship colors already sitting in the harbor, so the
     * tolerated harbor size grows with the bot's difficulty (and with the running player's power, since a
     * powerful player can often repel the next dangerous ship instead of being forced to accept it).
     */
    private boolean shouldKeepDiscovering(Player runningPlayer, BotDifficulty botDifficulty) {
        int shipsInHarbor = table.getShipsInHarborNumber();
        if (shipsInHarbor >= ShipColor.values().length - 1) {
            // one more distinct-colored ship would guarantee a repeated color somewhere: too risky regardless of difficulty
            return false;
        }
        int riskTolerance = switch (botDifficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
        };
        if (runningPlayer.getPowerValue() >= 3) {
            riskTolerance++;
        }
        return shipsInHarbor < riskTolerance;
    }

    /**
     * Picks the harbor card index that is the most worth trading or hiring.
     * Hiring an affordable employee is preferred over trading a ship, since employees provide lasting
     * points/power while a ship only provides a one-off coin gain.
     */
    private OptionalInt pickBestHarborCardIndex(Player runningPlayer, boolean isActivePlayer, BotDifficulty botDifficulty) {
        List<Card> harbor = table.getHarbor();
        int bestEmployeeIndex = -1;
        int bestEmployeeScore = Integer.MIN_VALUE;
        int bestShipIndex = -1;
        int bestShipGain = -1;
        for (int i = 0; i < harbor.size(); i++) {
            Card card = harbor.get(i);
            if (card instanceof EmployeeCard employeeCard) {
                if (!runningPlayer.canAffordHiring(employeeCard, isActivePlayer)) {
                    continue;
                }
                int score = botDifficulty == BotDifficulty.EASY
                        ? employeeCard.getPoints()
                        : employeeCard.getPoints() * 3 - runningPlayer.getActualCost(employeeCard);
                if (score > bestEmployeeScore) {
                    bestEmployeeScore = score;
                    bestEmployeeIndex = i;
                }
            } else if (card instanceof Ship ship && ship.getGain() > bestShipGain) {
                bestShipGain = ship.getGain();
                bestShipIndex = i;
            }
        }
        if (bestEmployeeIndex != -1) {
            return OptionalInt.of(bestEmployeeIndex);
        }
        return bestShipIndex != -1 ? OptionalInt.of(bestShipIndex) : OptionalInt.empty();
    }

    /**
     * Picks the poorest other player to benefit from a cargo ship's extra coin, helping balance the game.
     */
    private int pickCargoBeneficiaryIndex() {
        int beneficiaryIndex = -1;
        int lowestMoney = Integer.MAX_VALUE;
        for (int i = 0; i < players.size(); i++) {
            if (i == runningPlayerIndex) {
                continue;
            }
            int money = players.get(i).getMoneyValue();
            if (money < lowestMoney) {
                lowestMoney = money;
                beneficiaryIndex = i;
            }
        }
        return beneficiaryIndex;
    }

    /**
     * Picks the best expedition the running player is able to commit to (considering handymen as substitutes),
     * favoring the one with the highest points/money return. An EASY bot settles for the first committable one.
     */
    private OptionalInt pickBestCommittableExpeditionIndex(Player player, BotDifficulty botDifficulty) {
        List<Expedition> expeditions = table.getExpeditionCards();
        int bestIndex = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < expeditions.size(); i++) {
            Expedition expedition = expeditions.get(i);
            if (!expedition.isPlayerAbleToCommitExpedition(player, true)) {
                continue;
            }
            if (botDifficulty == BotDifficulty.EASY) {
                return OptionalInt.of(i);
            }
            int score = expedition.getPoints() * 2 + expedition.getMoney();
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex == -1 ? OptionalInt.empty() : OptionalInt.of(bestIndex);
    }

    private MoveRecord performCommitExpedition(int expeditionIndex, Player player) throws UserInputException {
        Expedition expedition = table.getExpeditionCards().get(expeditionIndex);
        List<ExpeditionEmployee> employeesTypes = expedition.getPossibleEmployeesTypesList(player);
        commitExpedition(expeditionIndex, employeesTypes);
        return getLastMove();
    }

    /**
     * Picks the manual contract with the highest immediate reward that the running player currently meets the
     * requirements for. Automatic contracts are signed automatically by {@link #evaluateContracts()} and are
     * therefore skipped here.
     */
    private OptionalInt pickBestSignableContractIndex(Player player) {
        ContractsBoard contractsBoard = table.getContractsBoard();
        if (!configuration.isJOMC_ExpansionUsed() || contractsBoard == null
                || player.getContractsCompleted() >= configuration.getMaxContractsCompletablePerPlayer()) {
            return OptionalInt.empty();
        }
        List<ContractCard> contracts = contractsBoard.getContracts();
        int bestIndex = -1;
        int bestReward = -1;
        for (int i = 0; i < contracts.size(); i++) {
            ContractCard contract = contracts.get(i);
            if (!(contract instanceof ManualContractCard)
                    || contractsBoard.isFull(i)
                    || contractsBoard.getSignedPlayers(i).contains(player)
                    || !contract.requirementsMet(player)) {
                continue;
            }
            int[] rewards = contract.getRewards();
            int slotIndex = contractsBoard.getSignedPlayers(i).size();
            int reward = slotIndex < rewards.length ? rewards[slotIndex] : 0;
            if (reward > bestReward) {
                bestReward = reward;
                bestIndex = i;
            }
        }
        return bestIndex == -1 ? OptionalInt.empty() : OptionalInt.of(bestIndex);
    }
}
