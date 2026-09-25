package com.matteopaciolla.portroyal.core.cards;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public abstract class Card {
    private final int id;

    public Card(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        String name = this.getIcon() != null ? this.getIcon() : this.getClass().getSimpleName();
        return String.format("%s#%d", name, this.getId());
    }

    public abstract String getIcon();
}