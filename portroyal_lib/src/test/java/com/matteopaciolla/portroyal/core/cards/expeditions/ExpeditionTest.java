package com.matteopaciolla.portroyal.core.cards.expeditions;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.Captain;
import com.matteopaciolla.portroyal.core.cards.employees.Handyman;
import com.matteopaciolla.portroyal.core.cards.employees.Priest;
import com.matteopaciolla.portroyal.core.cards.employees.Settler;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExpeditionTest {

    private static Expedition expedition1;
    private static Expedition expedition2;
    private static Expedition expedition3;
    private static Expedition expedition4;
    private static Expedition expedition5;
    private static Expedition expedition6;

    @BeforeAll
    static void setUp() {
        int cont = 1;
        expedition1 = new Expedition(cont++, 2, 4, 2, 0, 0);
        expedition2 = new Expedition(cont++, 2, 4, 0, 2, 0);
        expedition3 = new Expedition(cont++, 2, 4, 0, 0, 2);
        expedition4 = new Expedition(cont++, 3, 6, 2, 0, 1);
        expedition5 = new Expedition(cont++, 3, 6, 0, 2, 1);
        expedition6 = new Expedition(cont++, 3, 5, 1, 1, 1);
    }

    @Test
    void getPossibleEmployeesTypesListTest1() {

        Player alfio = new Player("Alfio");
        alfio.hireEmployee(new Captain(1, 1, 4));
        alfio.hireEmployee(new Priest(1, 1, 4));
        alfio.hireEmployee(new Settler(1, 1, 4));
        alfio.hireEmployee(new Handyman(1, 1, 4));

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.HANDYMAN)),
                Expedition.getEmpTypesCountFromTypesList(expedition1.getPossibleEmployeesTypesList(alfio))
        );
        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.PRIEST, ExpeditionEmployee.HANDYMAN)),
                Expedition.getEmpTypesCountFromTypesList(expedition2.getPossibleEmployeesTypesList(alfio))
        );
        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.SETTLER, ExpeditionEmployee.HANDYMAN)),
                Expedition.getEmpTypesCountFromTypesList(expedition3.getPossibleEmployeesTypesList(alfio))
        );
    }

    @Test
    void getPossibleEmployeesTypesListTest2() {

        Player bruno = new Player("Bruno");
        bruno.hireEmployee(new Captain(1, 1, 4));
        bruno.hireEmployee(new Captain(1, 1, 4));
        bruno.hireEmployee(new Handyman(1, 1, 4));
        bruno.hireEmployee(new Priest(1, 1, 4));
        bruno.hireEmployee(new Handyman(1, 1, 4));

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition4.getPossibleEmployeesTypesList(bruno)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.HANDYMAN))
        );

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition6.getPossibleEmployeesTypesList(bruno)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.PRIEST, ExpeditionEmployee.HANDYMAN))
        );
    }

    @Test
    void getPossibleEmployeesTypesListTest3() {

        Player bruno = new Player("Bruno");
        bruno.hireEmployee(new Captain(1, 1, 4));
        bruno.hireEmployee(new Settler(1, 1, 4));
        bruno.hireEmployee(new Handyman(1, 1, 4));
        bruno.hireEmployee(new Handyman(1, 1, 4));
        bruno.hireEmployee(new Handyman(1, 1, 4));

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition4.getPossibleEmployeesTypesList(bruno)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.SETTLER))
        );

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition6.getPossibleEmployeesTypesList(bruno)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.SETTLER))
        );
    }

    @Test
    void getPossibleEmployeesTypesListTest4() {

        Player carlo = new Player("Carlo");
        carlo.hireEmployee(new Captain(1, 1, 4));
        carlo.hireEmployee(new Handyman(1, 1, 4));
        carlo.hireEmployee(new Handyman(1, 1, 4));
        carlo.hireEmployee(new Handyman(1, 1, 4));

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition4.getPossibleEmployeesTypesList(carlo)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))
        );

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition6.getPossibleEmployeesTypesList(carlo)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))
        );
    }

    @Test
    void getPossibleEmployeesTypesListTest5() {

        Player carlo = new Player("Carlo");
        carlo.hireEmployee(new Handyman(1, 1, 4));
        carlo.hireEmployee(new Handyman(1, 1, 4));
        carlo.hireEmployee(new Handyman(1, 1, 4));

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition1.getPossibleEmployeesTypesList(carlo)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))
        );

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition2.getPossibleEmployeesTypesList(carlo)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))
        );

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition3.getPossibleEmployeesTypesList(carlo)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))
        );

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition4.getPossibleEmployeesTypesList(carlo)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))
        );

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition5.getPossibleEmployeesTypesList(carlo)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))
        );

        assertEquals(
                Expedition.getEmpTypesCountFromTypesList(expedition6.getPossibleEmployeesTypesList(carlo)),
                Expedition.getEmpTypesCountFromTypesList(List.of(
                        ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))
        );
    }

    @Test
    void isCommittableTest() {
        assertTrue(expedition1.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.HANDYMAN))));
        assertTrue(expedition2.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.PRIEST, ExpeditionEmployee.HANDYMAN))));
        assertTrue(expedition3.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.SETTLER, ExpeditionEmployee.HANDYMAN))));
        assertTrue(expedition3.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))));
        assertTrue(expedition4.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))));
        assertTrue(expedition4.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.CAPTAIN, ExpeditionEmployee.SETTLER, ExpeditionEmployee.HANDYMAN))));
        assertTrue(expedition4.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN, ExpeditionEmployee.HANDYMAN))));
        assertTrue(expedition4.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.HANDYMAN,
                ExpeditionEmployee.PRIEST,
                ExpeditionEmployee.HANDYMAN,
                ExpeditionEmployee.PRIEST,
                ExpeditionEmployee.SETTLER))));
        assertFalse(expedition4.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.HANDYMAN,
                ExpeditionEmployee.PRIEST,
                ExpeditionEmployee.PRIEST,
                ExpeditionEmployee.SETTLER,
                ExpeditionEmployee.SETTLER))));
        assertFalse(expedition6.isCommittable(Expedition.getEmpTypesCountFromTypesList(List.of(
                ExpeditionEmployee.HANDYMAN,
                ExpeditionEmployee.PRIEST,
                ExpeditionEmployee.PRIEST,
                ExpeditionEmployee.PRIEST))));
    }
}