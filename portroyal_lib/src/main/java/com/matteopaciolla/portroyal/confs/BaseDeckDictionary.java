package com.matteopaciolla.portroyal.confs;

import com.matteopaciolla.portroyal.core.cards.*;
import com.matteopaciolla.portroyal.core.cards.taxes.*;
import com.matteopaciolla.portroyal.core.cards.employees.*;
import com.matteopaciolla.portroyal.core.cards.expeditions.*;
import com.matteopaciolla.portroyal.core.cards.ships.*;
import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BaseDeckDictionary {

    private static final int INITIAL_COUNT_NUM = 1;
    private static int cont = INITIAL_COUNT_NUM;

    public static final List<Card> DECK_LIST = List.of(
        //--taxes--
        //max power taxes
        new MaxPowerTax(cont++),
        new MaxPowerTax(cont++),
        //min points taxes
        new MinPointsTax(cont++),
        new MinPointsTax(cont++),
        //--power cards--
        //sailors
        new Sailor(cont++,1,3),
        new Sailor(cont++,1,3),
        new Sailor(cont++,1,3),
        new Sailor(cont++,1,3),
        new Sailor(cont++,1,3),
        new Sailor(cont++,1,3),
        new Sailor(cont++,1,3),
        new Sailor(cont++,2,5),
        new Sailor(cont++,2,5),
        new Sailor(cont++,3,7),
        //pirates
        new Pirate(cont++,1,5),
        new Pirate(cont++,2,7),
        new Pirate(cont++,3,9),
        //--expedition eployees--
        //captains
        new Captain(cont++,1,4),
        new Captain(cont++,1,4),
        new Captain(cont++,1,4),
        new Captain(cont++,1,4),
        new Captain(cont++,1,4),
        //priests
        new Priest(cont++,1,4),
        new Priest(cont++,1,4),
        new Priest(cont++,1,4),
        new Priest(cont++,1,4),
        new Priest(cont++,1,4),
        //settlers
        new Settler(cont++,1,4),
        new Settler(cont++,1,4),
        new Settler(cont++,1,4),
        new Settler(cont++,1,4),
        new Settler(cont++,1,4),
        //handymen
        new Handyman(cont++,1,6),
        new Handyman(cont++,1,6),
        new Handyman(cont++,1,6),
        //--employees--
        //merchants
        new Merchant(cont++,1,3,ShipColor.BLACK),
        new Merchant(cont++,1,3,ShipColor.BLACK),
        new Merchant(cont++,2,5,ShipColor.BLUE),
        new Merchant(cont++,1,3,ShipColor.BLUE),
        new Merchant(cont++,1,3,ShipColor.GREEN),
        new Merchant(cont++,1,3,ShipColor.GREEN),
        new Merchant(cont++,1,3,ShipColor.RED),
        new Merchant(cont++,1,3,ShipColor.RED),
        new Merchant(cont++,1,3,ShipColor.YELLOW),
        new Merchant(cont++,2,5,ShipColor.YELLOW),
        //mademoiselles
        new Mademoiselle(cont++,2,7),
        new Mademoiselle(cont++,2,7),
        new Mademoiselle(cont++,3,9),
        new Mademoiselle(cont++,3,9),
        //jesters
        new Jester(cont++,1,5),
        new Jester(cont++,2,7),
        new Jester(cont++,2,7),
        new Jester(cont++,2,7),
        new Jester(cont++,3,9),
        //governors
        new Governor(cont++,0,8),
        new Governor(cont++,0,8),
        new Governor(cont++,0,8),
        new Governor(cont++,0,8),
        //admirals
        new Admiral(cont++,1,5),
        new Admiral(cont++,2,7),
        new Admiral(cont++,2,7),
        new Admiral(cont++,2,7),
        new Admiral(cont++,3,9),
        new Admiral(cont++,3,9),

        //--ships--
        //black ships
        new Ship(cont++,1,2,ShipColor.BLACK),
        new Ship(cont++,1,2,ShipColor.BLACK),
        new Ship(cont++,1,2,ShipColor.BLACK),
        new Ship(cont++,2,4,ShipColor.BLACK),
        new Ship(cont++,2,4,ShipColor.BLACK),
        new Ship(cont++,2,4,ShipColor.BLACK),
        new Ship(cont++,3,7,ShipColor.BLACK),
        new Ship(cont++,3,7,ShipColor.BLACK),
        new Ship(cont++,3,100,ShipColor.BLACK),
        new Ship(cont++,4,100,ShipColor.BLACK),
        //blue ships
        new Ship(cont++,1,1,ShipColor.BLUE),
        new Ship(cont++,1,1,ShipColor.BLUE),
        new Ship(cont++,1,1,ShipColor.BLUE),
        new Ship(cont++,2,1,ShipColor.BLUE),
        new Ship(cont++,2,2,ShipColor.BLUE),
        new Ship(cont++,2,2,ShipColor.BLUE),
        new Ship(cont++,3,2,ShipColor.BLUE),
        new Ship(cont++,3,5,ShipColor.BLUE),
        new Ship(cont++,3,5,ShipColor.BLUE),
        new Ship(cont++,4,5,ShipColor.BLUE),
        //green ships
        new Ship(cont++,1,1,ShipColor.GREEN),
        new Ship(cont++,1,1,ShipColor.GREEN),
        new Ship(cont++,1,1,ShipColor.GREEN),
        new Ship(cont++,2,1,ShipColor.GREEN),
        new Ship(cont++,2,3,ShipColor.GREEN),
        new Ship(cont++,2,3,ShipColor.GREEN),
        new Ship(cont++,3,3,ShipColor.GREEN),
        new Ship(cont++,3,5,ShipColor.GREEN),
        new Ship(cont++,3,5,ShipColor.GREEN),
        new Ship(cont++,4,5,ShipColor.GREEN),
        //red ships
        new Ship(cont++,1,1,ShipColor.RED),
        new Ship(cont++,1,1,ShipColor.RED),
        new Ship(cont++,1,1,ShipColor.RED),
        new Ship(cont++,2,3,ShipColor.RED),
        new Ship(cont++,2,3,ShipColor.RED),
        new Ship(cont++,2,3,ShipColor.RED),
        new Ship(cont++,3,6,ShipColor.RED),
        new Ship(cont++,3,6,ShipColor.RED),
        new Ship(cont++,3,100,ShipColor.RED),
        new Ship(cont++,4,100,ShipColor.RED),
        //yellow ships
        new Ship(cont++,1,1,ShipColor.YELLOW),
        new Ship(cont++,1,1,ShipColor.YELLOW),
        new Ship(cont++,1,1,ShipColor.YELLOW),
        new Ship(cont++,2,1,ShipColor.YELLOW),
        new Ship(cont++,2,2,ShipColor.YELLOW),
        new Ship(cont++,2,2,ShipColor.YELLOW),
        new Ship(cont++,3,2,ShipColor.YELLOW),
        new Ship(cont++,3,4,ShipColor.YELLOW),
        new Ship(cont++,3,4,ShipColor.YELLOW),
        new Ship(cont++,4,4,ShipColor.YELLOW),
        //--Expeditions--
        new Expedition(cont++,2,4,2,0, 0),
        new Expedition(cont++,2,4,0,2, 0),
        new Expedition(cont++,2,4,0,0, 2),
        new Expedition(cont++,3,6,2,0, 1),
        new Expedition(cont++,3,6,0,2, 1),
        //5 players expedition
        new Expedition(cont++,3,5,1,1, 1)
    );

    public static final Map<Integer, Card> DECK_MAP = DECK_LIST.stream()
            .collect(Collectors.toMap(Card::getId, Function.identity(), (oldValue, newValue) -> oldValue, TreeMap::new));
}
