package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.cards.enums.ExpeditionEmployee;
import com.matteopaciolla.portroyal.core.enums.MoveAction;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MoveRecord {
    private int timeIndex;
    private int runningPlayerIndex;
    private MoveAction move;
    private int choiceIndex;
    private int pickPlayerIndex;
    private List<ExpeditionEmployee> expeditionEmployees;
    private String notes;
    private Event mainEvent;
    private List<Event> sideEvents;

    /**
     * The most general constructor
     * @param timeIndex the index of the time when the move was made
     * @param runningPlayerIndex the index of the player who made the move
     * @param move the move action type
     * @param notes additional notes describing the move
     * @param mainEvent the main event that happened during the move
     * @param sideEvents the list of side events that happened during the move
     */
    public MoveRecord(int timeIndex, int runningPlayerIndex, MoveAction move, String notes, Event mainEvent, List<Event> sideEvents) {
        this.timeIndex = timeIndex;
        this.runningPlayerIndex = runningPlayerIndex;
        this.move = move;
        this.choiceIndex = -1;
        this.pickPlayerIndex = -1;
        this.expeditionEmployees = null;
        this.notes = notes;
        this.mainEvent = mainEvent;
        this.sideEvents = sideEvents;
    }

    /**
     * Constructor for a move with a choice index, typically used for the trade/hire phase
     * also can be handy for the end turn move
     * @param timeIndex the index of the time when the move was made
     * @param runningPlayerIndex the index of the player who made the move
     * @param move the move action type
     * @param choiceIndex the index of the card chosen for the action
     * @param pickPlayerIndex the index of the player chosen for the action
     * @param notes additional notes describing the move
     * @param mainEvent the main event that happened during the move
     * @param sideEvents the list of side events that happened during the move
     */
    public MoveRecord(int timeIndex, int runningPlayerIndex, MoveAction move, int choiceIndex, int pickPlayerIndex, String notes, Event mainEvent, List<Event> sideEvents) {
        this.timeIndex = timeIndex;
        this.runningPlayerIndex = runningPlayerIndex;
        this.move = move;
        this.choiceIndex = choiceIndex;
        this.pickPlayerIndex = pickPlayerIndex;
        this.expeditionEmployees = null;
        this.notes = notes;
        this.mainEvent = mainEvent;
        this.sideEvents = sideEvents;
    }

    /**
     * Constructor for a move with a choice index and expedition employees, typically used for the commit expedition phase
     * @param timeIndex the index of the time when the move was made
     * @param runningPlayerIndex the index of the player who made the move
     * @param move the move action type
     * @param choiceIndex the index of the card chosen for the action (the expedition card index)
     * @param expeditionEmployees the list of employees chosen for committing the expedition
     * @param notes additional notes describing the move
     * @param mainEvent the main event that happened during the move
     * @param sideEvents the list of side events that happened during the move
     */
    public MoveRecord(int timeIndex, int runningPlayerIndex, MoveAction move, int choiceIndex, List<ExpeditionEmployee> expeditionEmployees, String notes, Event mainEvent, List<Event> sideEvents) {
        this.timeIndex = timeIndex;
        this.runningPlayerIndex = runningPlayerIndex;
        this.move = move;
        this.choiceIndex = choiceIndex;
        this.pickPlayerIndex = -1;
        this.expeditionEmployees = expeditionEmployees;
        this.notes = notes;
        this.mainEvent = mainEvent;
        this.sideEvents = sideEvents;
    }

    /**
     * Constructor for a move with a choice index and notes, typically used for sign the contract phase
     * @param timeIndex the index of the time when the move was made
     * @param runningPlayerIndex the index of the player who made the move
     * @param move the move action type
     * @param choiceIndex the index of the card chosen for the action (the contract card index)
     * @param notes additional notes describing the move
     * @param mainEvent the main event that happened during the move
     * @param sideEvents the list of side events that happened during the move
     */
    public MoveRecord(int timeIndex, int runningPlayerIndex, MoveAction move, int choiceIndex, String notes, Event mainEvent, List<Event> sideEvents) {
        this.timeIndex = timeIndex;
        this.runningPlayerIndex = runningPlayerIndex;
        this.move = move;
        this.choiceIndex = choiceIndex;
        this.pickPlayerIndex = -1;
        this.expeditionEmployees = null;
        this.notes = notes;
        this.mainEvent = mainEvent;
        this.sideEvents = sideEvents;
    }

    @Override
    public String toString() {
        return "#" + timeIndex
                + "(" + runningPlayerIndex
                + "," + move
                + "," + choiceIndex
                + "," + pickPlayerIndex
                + "," + expeditionEmployees
                + (mainEvent != null ? ", mainEvent: " + mainEvent : "")
                + (sideEvents != null && !sideEvents.isEmpty() ? ", sideEvents: " + sideEvents : "")
                + (notes != null && !notes.isEmpty() ? " Notes: " + notes : "")
                +')';
    }
}
