package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.core.enums.MoveAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveRecordTest {

    @Test
    void testEquals() {
        MoveRecord moveRecord1 = new MoveRecord(1, 2, MoveAction.REPEL, 3, List.of(ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.PRIEST), "notes", null, null);
        MoveRecord moveRecord2 = new MoveRecord(1, 2, MoveAction.REPEL, 3, List.of(ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.PRIEST), "notes", null, null);
        MoveRecord moveRecord3 = new MoveRecord(1, 2, MoveAction.REPEL, 3, List.of(ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.PRIEST), "notes", null, null);
        assertEquals(moveRecord1, moveRecord2);
        assertEquals(moveRecord2, moveRecord3);
        assertEquals(moveRecord1, moveRecord3);
    }

    @Test
    void testNotEquals() {
        MoveRecord moveRecord1 = new MoveRecord(1, 2, MoveAction.REPEL, 3, List.of(ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.PRIEST, ExpeditionEmployee.CAPTAIN), "notes", null, null);
        MoveRecord moveRecord2 = new MoveRecord(1, 2, MoveAction.REPEL, 3, List.of(ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.PRIEST), "notes", null, null);
        MoveRecord moveRecord3 = new MoveRecord(1, 2, MoveAction.REPEL, 3, List.of(ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.PRIEST, ExpeditionEmployee.SETTLER), "notes", null, null);
        assertNotEquals(moveRecord1, moveRecord2);
        assertNotEquals(moveRecord2, moveRecord3);
        assertNotEquals(moveRecord1, moveRecord3);
    }
}