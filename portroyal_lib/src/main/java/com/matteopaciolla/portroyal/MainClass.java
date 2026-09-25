package com.matteopaciolla.portroyal;

import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.MatchCreator;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.enums.BotDifficulty;
import com.matteopaciolla.portroyal.exceptions.IOGameException;
import com.matteopaciolla.portroyal.exceptions.internal.InternalGameException;
import com.matteopaciolla.portroyal.facades.IOFacade;

import java.util.List;
import java.util.Map;

public class MainClass {

    public static void main(String[] args) throws InternalGameException {
        System.out.println("Playing...");
        playMatchVsBots();
    }

    /**
     * Starts a match where "Matteo" is played interactively while "EasBot", "MedBot" and "HarBot"
     * are driven automatically by {@link Match#calculateNextMoveRecord(BotDifficulty)}, one at each difficulty.
     */
    private static void playMatchVsBots() throws InternalGameException {
        String filename = "match_vs_bots";
        List<Player> players = List.of(
                new Player("Matteo"),
                new Player("EasBot"),
                new Player("MedBot"),
                new Player("HarBot")
        );
        Map<String, BotDifficulty> botPlayers = Map.of(
                "EasBot", BotDifficulty.EASY,
                "MedBot", BotDifficulty.MEDIUM,
                "HarBot", BotDifficulty.HARD
        );
        List<MoveRecord> moveRecords = null;
        try {
            moveRecords = IOFacade.loadMovesFromExternalCSV(filename);
        } catch (IOGameException e) {
            System.err.println("Error loading moves from external CSV " + e.getMessage());
        }
        Configuration configuration = Configuration.builder().JOMC_ExpansionUsed(true).build();
        Match match = MatchCreator.createMatch(0, players, configuration, moveRecords);
        GamerUI gamerUI = new GamerUI(match, filename, botPlayers);
        gamerUI.playMatch();
    }

    private static void playMatch0() throws InternalGameException {
        String filename = "match_0_jomc_3p";
        //create list of players
        List<Player> players = List.of(
                new Player("Alfio"),
                new Player("Bruno"),
                new Player("Carlo")
        );
        List<MoveRecord> moveRecords = null;
        try {
            moveRecords = IOFacade.loadMovesFromExternalCSV(filename);
        } catch (IOGameException e) {
            System.err.println("Error loading moves from external CSV " + e.getMessage());
        }
        //create match
        Configuration configuration = Configuration.builder().JOMC_ExpansionUsed(true).build();
        Match match = MatchCreator.createMatch(0, players, configuration, moveRecords);
        //play match
        GamerUI gamerUI = new GamerUI(match, filename);
        gamerUI.playMatch();
    }

    private static void playMatch1() throws InternalGameException {
        String filename = "match_1_base_3p";
        //create list of players
        List<Player> players = List.of(
                new Player("Alfio"),
                new Player("Bruno"),
                new Player("Carlo")
        );
        List<MoveRecord> moveRecords = null;
        try {
            moveRecords = IOFacade.loadMovesFromExternalCSV(filename);
        } catch (IOGameException e) {
            System.err.println("Error loading moves from external CSV " + e.getMessage());
        }
        //create match
        Configuration configuration = Configuration.builder().JOMC_ExpansionUsed(false).build();
        Match match = MatchCreator.createMatch(1, players, configuration, moveRecords);
        //play match
        GamerUI gamerUI = new GamerUI(match, filename);
        gamerUI.playMatch();
    }

    private static void playMatch2() throws InternalGameException {
        String filename = "moves";
        //create list of players
        List<Player> players = List.of(
                new Player("Matteo"),
                new Player("Giulia")
        );
        List<MoveRecord> moveRecords = null;
        try {
            moveRecords = IOFacade.loadMovesFromExternalCSV(filename);
        } catch (IOGameException e) {
            System.err.println("Error loading moves from external CSV " + e.getMessage());
        }
        //create match
        Configuration configuration = Configuration.builder().JOMC_ExpansionUsed(true).build();
        Match match = MatchCreator.createMatch(2, players, configuration, moveRecords);
        //play match
        GamerUI gamerUI = new GamerUI(match, filename);
        gamerUI.playMatch();
    }

    private static void playMatch3() throws InternalGameException {
        String filename = "moves3";
        //create list of players
        List<Player> players = List.of(
                new Player("Matteo"),
                new Player("Thomas")
        );
        List<MoveRecord> moveRecords = null;
        try {
            moveRecords = IOFacade.loadMovesFromExternalCSV(filename);
        } catch (IOGameException e) {
            System.err.println("Error loading moves from external CSV " + e.getMessage());
        }
        //create match
        Configuration configuration = Configuration.builder().JOMC_ExpansionUsed(true).build();
        Match match = MatchCreator.createMatch(3, players, configuration, moveRecords);
        //play match
        GamerUI gamerUI = new GamerUI(match, filename);
        gamerUI.playMatch();
    }
}
