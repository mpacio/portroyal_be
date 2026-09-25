package com.matteopaciolla.prbe.converter;

import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.employees.*;
import com.matteopaciolla.portroyal.core.cards.expeditions.Expedition;
import com.matteopaciolla.portroyal.core.cards.ships.CargoShip;
import com.matteopaciolla.portroyal.core.cards.ships.Ship;
import com.matteopaciolla.portroyal.core.cards.taxes.MaxPowerTax;
import com.matteopaciolla.portroyal.core.cards.taxes.MinPointsTax;
import com.matteopaciolla.prbe.dto.CardDto;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CardConverter {

    public static CardDto toDto(Card card) {
        CardDto cardDto = new CardDto();
        cardDto.setId(card.getId());
        cardDto.setType(card.getClass().getSimpleName());
        cardDto.setIcon(card.getIcon());
        if (card instanceof EmployeeCard employeeCard) {
            cardDto.setPoints(employeeCard.getPoints());
            cardDto.setCost(employeeCard.getCost());
            if (employeeCard instanceof ColoredEmployeeCard coloredEmployeeCard) {
                cardDto.setColor(coloredEmployeeCard.getColor().toString());
            }
            if (employeeCard instanceof PowerCard powerCard) {
                cardDto.setPower(powerCard.getPower());
            }
        }
        if (card instanceof Ship ship) {
            cardDto.setMoney(ship.getGain());
            cardDto.setColor(ship.getColor().toString());
            cardDto.setPower(ship.getPower());
        }
        if (card instanceof Expedition expedition) {
            cardDto.setMoney(expedition.getMoney());
            cardDto.setPoints(expedition.getPoints());
            cardDto.setExpeditionEmployees(new ArrayList<>());
            expedition.getNeededEmployeesTypesList().forEach(employeeType -> cardDto.getExpeditionEmployees().add(employeeType.toString()));
        }
        return cardDto;
    }

    public static <T extends Card> List<CardDto> toDtoList(List<T> cards) {
        List<CardDto> cardDtos = new LinkedList<>();
        cards.forEach(card -> cardDtos.add(toDto(card)));
        return cardDtos;
    }

    public enum CardType {
        SHIP(Ship.class),
        CARGO_SHIP(CargoShip.class),
        EXPEDITION(Expedition.class),
        ADMIRAL(Admiral.class),
        CAPTAIN(Captain.class),
        CLERK(Clerk.class),
        DEPUTY(Deputy.class),
        GOVERNOR(Governor.class),
        GUNNER(Gunner.class),
        HANDYMAN(Handyman.class),
        JESTER(Jester.class),
        MADEMOISELLE(Mademoiselle.class),
        MERCHANT(Merchant.class),
        PIRATE(Pirate.class),
        PRIEST(Priest.class),
        SAILOR(Sailor.class),
        SETTLER(Settler.class),
        MAX_POWER_TAX(MaxPowerTax.class),
        MIN_POINTS_TAX(MinPointsTax.class);

        public final Class<? extends Card> cardClass;

        public String toString() {
            return cardClass.getSimpleName();
        }

        CardType(Class<? extends Card> cardClass) {
            this.cardClass = cardClass;
        }
    }
}
