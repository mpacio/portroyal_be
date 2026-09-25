package com.matteopaciolla.portroyal.core;

import com.matteopaciolla.portroyal.core.cards.Card;
import com.matteopaciolla.portroyal.core.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
public class Event {

    private final EventType type;
    private final Player player;
    private final List<Card> involvedCards;
    private final Integer value;

    public Event(EventType type, Player player) {
        this.type = type;
        this.player = player;
        this.involvedCards = null;
        this.value = null;
    }

    public Event(EventType type, Player player, Card involvedCard) {
        this.type = type;
        this.player = player;
        this.involvedCards = involvedCard != null ? List.of(involvedCard) : null;
        this.value = null;
    }

    public Event(EventType type, Player player, List<Card> involvedCards) {
        this.type = type;
        this.player = player;
        this.involvedCards = involvedCards;
        this.value = null;
    }

    public Event(EventType type, Player player, Integer value) {
        this.type = type;
        this.player = player;
        this.involvedCards = null;
        this.value = value;
    }

    public Event(EventType type, Player player, Integer value, Card involvedCard) {
        this.type = type;
        this.player = player;
        this.involvedCards = involvedCard != null ? List.of(involvedCard) : null;
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder(player.getName() + "->" + type + "[");
        if (involvedCards != null && !involvedCards.isEmpty()) {
            for (Card card : involvedCards) {
                res.append('#').append(card.getId()).append(",");
            }
            res = new StringBuilder(res.substring(0, res.length() - 1)); //remove last comma
        }
        res.append("]");
        if (value != null) {
            res.append("@");
            res.append(value);
        }
         return res.toString();
    }
}
