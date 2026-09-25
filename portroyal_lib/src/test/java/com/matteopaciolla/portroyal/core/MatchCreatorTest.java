package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.exceptions.internal.InternalGameException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchCreatorTest {

    @Test
    void areAllDifferentPlayersReturnsTrueWhenAllPlayersAreDifferent() {
        List<Player> players = List.of(mock(Player.class), mock(Player.class));
        Assertions.assertTrue(MatchCreator.areAllDifferentPlayers(players));
    }

    @Test
    void areAllDifferentPlayersReturnsFalseWhenPlayersAreNotDifferent() {
        Player player = mock(Player.class);
        List<Player> players = List.of(player, player);
        assertFalse(MatchCreator.areAllDifferentPlayers(players));
    }
}
