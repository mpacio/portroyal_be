package com.matteopaciolla.portroyal.core.cards.contracts.abst;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.Card;
import lombok.Getter;

import java.util.Arrays;

@Getter
public abstract class ContractCard extends Card {

    public ContractCard(int id) {
        super(id);
    }

    public abstract int[] getRewards();

    @Override
    public String getIcon() {
        return Emojis.CONTRACT;
    }

    public abstract boolean requirementsMet(Player player);

    public abstract String getName();
    public abstract String getDescription();

    @Override
    public String toString() {
        return getName() + super.toString() + " Rewards=" + Arrays.toString(getRewards());
    }
}