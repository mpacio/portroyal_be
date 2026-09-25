package com.matteopaciolla.portroyal;

import com.matteopaciolla.portroyal.confs.Configuration;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.MatchCreator;
import com.matteopaciolla.portroyal.core.MoveRecord;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.exceptions.IOGameException;
import com.matteopaciolla.portroyal.exceptions.internal.InternalGameException;
import com.matteopaciolla.portroyal.exceptions.userinput.UserInputException;
import com.matteopaciolla.portroyal.facades.IOFacade;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GamerUITest {

    @BeforeAll
    public static void setUp() {
    }

    @Disabled
    @Test
    void testPlayMatchWithBaseDeckAnd3Players() throws UserInputException, IOGameException, InternalGameException {
        //create list of players
        List<Player> players = List.of(
                new Player("Alfio"),
                new Player("Bruno"),
                new Player("Carlo")
        );
        //create match
        Configuration configuration = Configuration.builder().JOMC_ExpansionUsed(false).build();
        List<MoveRecord> moves = IOFacade.loadMovesFromResourceCSV("match_1_base_3p");
        Match match = MatchCreator.createMatch(1, players, configuration, moves);
        //play match
        GamerUI gamerUI = new GamerUI(match ,null);
        gamerUI.playMatch();
        assertTrue(match.isMatchEnded());
        assertEquals(match.getWinner(), players.get(1));
    }

    @Disabled
    @Test
    void testPlayMatchWithJOMCExpAnd2Players() throws UserInputException, IOGameException, InternalGameException {
        //create list of players
        List<Player> players = List.of(
                new Player("Matteo"),
                new Player("Giulia")
        );
        //create match
        Configuration configuration = Configuration.builder().JOMC_ExpansionUsed(true).build();
        List<MoveRecord> moves = IOFacade.loadMovesFromResourceCSV("match_2_JOMC_2p");
        Match match = MatchCreator.createMatch(2, players, configuration, moves);
        //play match
        GamerUI gamerUI = new GamerUI(match ,null);
        gamerUI.playMatch();
        assertTrue(match.isMatchEnded());
        assertEquals(match.getWinner(), players.get(1));
    }

}