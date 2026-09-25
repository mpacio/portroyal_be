package com.matteopaciolla.portroyal.core.cards.taxes;

import com.matteopaciolla.portroyal.confs.Emojis;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;

import java.util.Collections;
import java.util.Comparator;

public class MinPointsTax extends TaxCard{

    public MinPointsTax(int id) {
        super(id);
    }

    @Override
    public String toString() {
        return super.toString() + Emojis.ARROW_DOWN + Emojis.POINTS;
    }

}
