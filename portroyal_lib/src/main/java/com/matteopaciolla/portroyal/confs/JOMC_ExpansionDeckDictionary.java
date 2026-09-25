package com.matteopaciolla.portroyal.confs;

import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.contracts.abst.ContractCard;
import com.matteopaciolla.portroyal.core.cards.contracts.impl.*;
import com.matteopaciolla.portroyal.core.cards.employees.Clerk;
import com.matteopaciolla.portroyal.core.cards.employees.Deputy;
import com.matteopaciolla.portroyal.core.cards.employees.Gunner;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;
import com.matteopaciolla.portroyal.core.cards.ships.CargoShip;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JOMC_ExpansionDeckDictionary {

    private static final int INITIAL_COUNT_NUM = 121;
    private static int cont = INITIAL_COUNT_NUM;

    public static final List<Card> DECK_LIST = List.of(
        //--employees--
        //gunners
        new Gunner(cont++, 1, 4),
        new Gunner(cont++, 2, 6),
        new Gunner(cont++, 2, 6),
        new Gunner(cont++, 3, 8),
        //deputies
        new Deputy(cont++, 1, 5),
        new Deputy(cont++, 1, 5),
        new Deputy(cont++, 2, 7),
        new Deputy(cont++, 2, 7),
        new Deputy(cont++, 3, 9),
        //clerks
        new Clerk(cont++, 1, 4, ShipColor.BLACK),
        new Clerk(cont++, 2, 6, ShipColor.BLUE),
        new Clerk(cont++, 3, 9, ShipColor.GREEN),
        new Clerk(cont++, 2, 6, ShipColor.RED),
        new Clerk(cont++, 1, 4, ShipColor.YELLOW),
        //--ships--
        //cargo ships
        new CargoShip(cont++, 2, ShipColor.BLACK),
        new CargoShip(cont++, 100, ShipColor.BLACK),
        new CargoShip(cont++, 1, ShipColor.BLUE),
        new CargoShip(cont++, 5, ShipColor.BLUE),
        new CargoShip(cont++, 1, ShipColor.GREEN),
        new CargoShip(cont++, 3, ShipColor.GREEN),
        new CargoShip(cont++, 1, ShipColor.RED),
        new CargoShip(cont++, 6, ShipColor.RED),
        new CargoShip(cont++, 2, ShipColor.YELLOW),
        new CargoShip(cont++, 4, ShipColor.YELLOW)
    );

    public static final List<ContractCard> CONTRACTS_DECK_LIST = List.of(
        new Luminary(cont++),
        new Bruja(cont++),
        new MaritimeSupremacy(cont++),
        new NewColony(cont++),
        new TradeOutpost(cont++),
        new TradeMaster(cont++),
        new ThriftyStaff(cont++),
        new Mercenary(cont++),
        new Explorer(cont++),
        new FrigateNemesis(cont++),
        new GalleonNemesis(cont++),
        new TaxInspector(cont++),
        new Jinx(cont++),
        new Speculator(cont++),
        new MajorSpeculator(cont++),
        new PiratesNest(cont++)
    );

    public static final Map<Integer, Card> DECK_MAP = DECK_LIST.stream()
            .collect(Collectors.toMap(Card::getId, Function.identity(), (oldValue, newValue) -> oldValue, TreeMap::new));

    public static final Map<Integer, ContractCard> CONTRACTS_DECK_MAP = CONTRACTS_DECK_LIST.stream()
            .collect(Collectors.toMap(ContractCard::getId, Function.identity(), (oldValue, newValue) -> oldValue, TreeMap::new));
}
