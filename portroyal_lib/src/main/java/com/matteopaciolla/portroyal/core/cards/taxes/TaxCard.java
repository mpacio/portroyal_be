package com.matteopaciolla.portroyal.core.cards.taxes;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.cards.Card;

import lombok.Getter;

@Getter
public abstract class TaxCard extends Card {
    public TaxCard(int id) {
        super(id);
    }

    @Override
    public String getIcon() {
        return Emojis.TAX;
    }
}
