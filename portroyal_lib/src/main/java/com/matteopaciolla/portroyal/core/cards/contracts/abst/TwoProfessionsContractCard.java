package com.matteopaciolla.portroyal.core.cards.contracts.abst;

import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.employees.EmployeeCard;

public abstract class TwoProfessionsContractCard extends ManualContractCard {

    public TwoProfessionsContractCard(int id) {
        super(id);
    }


    public abstract String getProfession1Name();
    public abstract String getProfession2Name();

    public abstract Class<? extends EmployeeCard> getProfession1Class();
    public abstract Class<? extends EmployeeCard> getProfession2Class();

    @Override
    public boolean requirementsMet(Player player) {
        // return true if the player has both professions in his personal display at least once per profession
        return player.getEmployees().stream().anyMatch(card -> card.getClass().equals(getProfession1Class())) &&
                player.getEmployees().stream().anyMatch(card -> card.getClass().equals(getProfession2Class()));
    }

    @Override
    public String getDescription() {
        String requiredProfessions = getProfession1Class().equals(getProfession2Class())
                ? "two cards of the " + getProfession1Name() + " Employee"
                : "a " + getProfession1Name() + " and a " + getProfession2Name();
        return "If your personal display contains " + requiredProfessions + ", " +
                "you may sign this Contract. " +
                "The Handyman does not count for any depicted Professions. " +
                "(Once you complete this Contract you can still use your Settlers, Priests, " +
                "and Captains for Expeditions.)";
    }
}