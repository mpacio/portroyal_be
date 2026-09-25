package com.matteopaciolla.portroyal.core.enums;

import lombok.Getter;

@Getter
public enum EventType {
    // side effects that can happen during another player's turn
    AUTO_SIGNED_CONTRACT("Auto signed contract"),
    BEING_TAXED         ("Being taxed"),
    PRIZED_FROM_TAX     ("Got prized from tax"),
    GOT_JESTER_MONEY_EFF("Got money because of the jester effect"),
    GOT_CARGO_SHIP_MONEY("Got money because of the cargo ship effect"),
    TRIGGERED_FINAL_TURN("Triggered final turn condition"),// maybe it is not side effect because it happens at the same player that is currently playing
    GOT_AP_FEE          ("Got active player fee"),

    // main effects that happen during the player's turn,
    DISCOVERED_CARD     ("Discovered a card"),
    REPELLED_SHIP       ("Repelled a ship"),
    ACCEPTED_SHIP       ("Accepted a ship"),
    HIRED_EMPLOYEE      ("Hired an employee"),
    TRADED_SHIP         ("Traded a ship"),
    RENOUNCED_SHIP      ("Renounced a ship"),
    COMMITTED_EXPEDITION("Committed an expedition"),
    SIGNED_CONTRACT     ("Signed a contract"),
    ;

    private final String description;

    EventType(String description) {
        this.description = description;
    }
}
