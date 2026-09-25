package com.matteopaciolla.portroyal.core.effects;

import com.matteopaciolla.portroyal.core.Event;
import com.matteopaciolla.portroyal.core.Match;
import com.matteopaciolla.portroyal.core.Player;
import com.matteopaciolla.portroyal.core.cards.taxes.MaxPowerTax;
import com.matteopaciolla.portroyal.core.cards.taxes.MinPointsTax;
import com.matteopaciolla.portroyal.core.cards.taxes.TaxCard;
import com.matteopaciolla.portroyal.core.enums.EventType;

public class TaxEffect extends Effect {

    public static void activate(Match match, TaxCard card) {
        for (Player player : match.getPlayers()) {
            if (isPlayerTooRich(player, match)) {
                taxPlayer(player, match);
            }
        }
        if (card instanceof MinPointsTax) {
            prizeMinPointsPlayer(match, card);
        } else if (card instanceof MaxPowerTax) {
            prizeMaxPowerPlayer(match, card);
        }
    }

    private static boolean isPlayerTooRich(Player player, Match match) {
        return player.getMoneyValue() > match.getConfiguration().getTaxedMoney();
    }

    private static void taxPlayer(Player player, Match match) {
        int taxDue = player.getMoneyValue() / match.getConfiguration().getTaxRate();
        match.getTable().discardCards(player.removeMoney(taxDue));
        player.setTaxed(true);
        match.addNotes(player.getName() + " taxed of " + taxDue + " coins");
        match.getSideEvents().add(new Event(EventType.BEING_TAXED,player, taxDue));
    }

    private static void prizeMinPointsPlayer(Match match, TaxCard card) {
        // find the minimum points number among the players
        int minPoints = match.getPlayers().stream().mapToInt(Player::getPointsValue).min().orElse(0);
        // prize every player with the minimum points number
        if (minPoints > 0) {
            match.getPlayers().stream()
                    .filter(player -> player.getPointsValue() == minPoints)
                    .forEach(player -> {
                        player.addMoney(match.getTable().getMoney(1));
                        match.addNotes(player.getName() + " tax-prized for minimum points");
                    match.getSideEvents().add(new Event(EventType.PRIZED_FROM_TAX, player, 1, card));
                    });
        }
    }

    private static void prizeMaxPowerPlayer(Match match, TaxCard card) {
        // find the maximum power number among the players
        int maxPower = match.getPlayers().stream().mapToInt(Player::getPowerValue).max().orElse(0);
        // prize every player with the maximum power number
        if (maxPower > 0) {
            match.getPlayers().stream()
                    .filter(player -> player.getPowerValue() == maxPower)
                    .forEach(player -> {
                        player.addMoney(match.getTable().getMoney(1));
                        match.addNotes(player.getName() + " tax-prized for maximum power");
                        match.getSideEvents().add(new Event(EventType.PRIZED_FROM_TAX, player, 1, card));
                    });
        }
    }
}
