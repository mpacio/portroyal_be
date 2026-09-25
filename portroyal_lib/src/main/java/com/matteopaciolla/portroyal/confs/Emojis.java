package com.matteopaciolla.portroyal.confs;

import com.matteopaciolla.portroyal.core.cards.enums.ShipColor;

import java.util.Map;

public class Emojis {

    //--------------general--------------//
    public static String OK = "✅";
    public static String KO = "❌";
    public static String WARNING = "⚠️";
    public static String ERROR = "🚫";
    public static String INFO = "ℹ️";
    public static String QUESTION = "❓";
    public static String CROWN = "👑";
    public static String HANDSHAKE = "🤝";
    public static String TROPHY = "🏆";
    public static String JOGGING = "🏃";
    public static String BOMB = "💣";
    public static String FIRE = "🔥";
    public static String PERSON = "👤";
    public static String PERSONS = "👥";
    public static String GLOBE = "🌍";
    public static String CARGO_SHIP = "🚢";

    //-------------entities-------------//
    public static String MONEY = "💲";
    public static String POWER = "⚔️";
    public static String POINTS = "🛡️";
    public static String INFINITE_POWER = "💀";

    //--------------cards--------------//
    //ships
    public static String SHIP = "⛵️";
    public static String COLOR_BLACK = "⚫";
    public static String COLOR_BLUE = "🔵";
    public static String COLOR_GREEN = "🟢";
    public static String COLOR_RED = "🔴";
    public static String COLOR_YELLOW = "🟡";
    //employees
    public static String CAPTAIN = "⚓";
    public static String PRIEST = "✝️";
    public static String SETTLER = "🏠";
    public static String HANDYMAN = "🧽";
    public static String TAX = "💰";
    public static String ARROW_UP = "⬆️";
    public static String ARROW_DOWN = "⬇️";
    public static String EXPEDITION = "🚀";
    public static String CONTRACT = "📜";
    public static String JESTER = "🃏";
    public static String ADMIRAL = "👮";
    public static String PIRATE = "🏴‍☠️";
    public static String SAILOR = "🦜";
    public static String MERCHANT = "🛍️";
    public static String MADEMOISELLE = "💃";
    public static String GOVERNOR = "🎩";
    public static String DEPUTY = "👒";
    public static String GUNNER = "🔫";
    public static String CLERK = "👔";

    //--------------phases--------------//
    public static String PHASE_DISCOVER = "🔍";
    public static String PHASE_REPEL = "🗡️";
    public static String PHASE_TRADE = "🛒";
    public static String PHASE_SUB_TRADE = "👀";

    public static Map<ShipColor, String> SHIP_COLORS_MAP = Map.of(
            ShipColor.BLACK, COLOR_BLACK,
            ShipColor.BLUE, COLOR_BLUE,
            ShipColor.GREEN, COLOR_GREEN,
            ShipColor.RED, COLOR_RED,
            ShipColor.YELLOW, COLOR_YELLOW
    );
}
