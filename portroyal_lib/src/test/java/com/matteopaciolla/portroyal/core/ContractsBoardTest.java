package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.impl.FrigateNemesis;
import com.matteopaciolla.portroyal.core.cards.contracts.impl.GalleonNemesis;
import com.matteopaciolla.portroyal.core.cards.contracts.impl.Mercenary;
import com.matteopaciolla.portroyal.core.cards.contracts.impl.Speculator;
import com.matteopaciolla.portroyal.core.cards.contracts.impl.TaxInspector;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.exceptions.userinput.MaxContractsNumberException;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContractsBoardTest {

    @Test
    void progressOnAutomaticContractReservesSlotUntilItIsSigned() {
        Player player = new Player("Player");
        player.setup(new LinkedDeck<>(new Random()));
        ContractsBoard board = new ContractsBoard(
                List.of(new FrigateNemesis(1), new TaxInspector(2), new Mercenary(3)),
                1,
                2);
        player.setTaxed(true);

        Match match = mock(Match.class);
        Table table = mock(Table.class);
        when(match.getTable()).thenReturn(table);
        when(match.getSideEvents()).thenReturn(new LinkedList<>());
        when(table.getMoney(anyInt())).thenReturn(new LinkedDeck<>(new Random()));

        player.renounceShip(new Ship(3, 1, 1, ShipColor.RED));
        board.updateAutomaticContractProgress(player);

        assertEquals(1, board.getContractsInProgressCount(player));
        board.signAutomaticContracts(player, match);
        assertEquals(0, player.getContractsCompleted());
        assertThrows(MaxContractsNumberException.class,
                () -> board.signManualContract(2, player, null));

        player.renounceShip(new Ship(4, 1, 1, ShipColor.RED));
        player.renounceShip(new Ship(5, 1, 1, ShipColor.RED));
        board.updateAutomaticContractProgress(player);

        board.signAutomaticContracts(player, match);

        assertEquals(1, player.getContractsCompleted());
        assertEquals(0, board.getContractsInProgressCount(player));
        assertEquals(0, board.getSignedPlayers(1).size());
    }

    @Test
    void progressDoesNotReserveSlotWhenAutomaticContractIsNotOnBoard() {
        Player player = new Player("Player");
        ContractsBoard board = new ContractsBoard(List.of(new Mercenary(1)), 1, 2);

        player.renounceShip(new Ship(2, 1, 1, ShipColor.RED));
        board.updateAutomaticContractProgress(player);

        assertEquals(0, board.getContractsInProgressCount(player));
    }

    @Test
    void galleonAndSpeculatorProgressEachReserveASlot() {
        Player galleonPlayer = new Player("Galleon player");
        ContractsBoard galleonBoard = new ContractsBoard(List.of(new GalleonNemesis(1)), 1, 2);
        galleonPlayer.renounceShip(new Ship(2, 1, 1, ShipColor.BLACK));
        galleonBoard.updateAutomaticContractProgress(galleonPlayer);

        Player speculatorPlayer = new Player("Speculator player");
        ContractsBoard speculatorBoard = new ContractsBoard(List.of(new Speculator(3)), 1, 2);
        speculatorPlayer.increaseMinorSpeculator();
        speculatorBoard.updateAutomaticContractProgress(speculatorPlayer);

        assertEquals(1, galleonBoard.getContractsInProgressCount(galleonPlayer));
        assertEquals(1, speculatorBoard.getContractsInProgressCount(speculatorPlayer));
    }
}
