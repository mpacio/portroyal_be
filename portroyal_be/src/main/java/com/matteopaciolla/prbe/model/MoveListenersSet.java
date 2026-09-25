package com.matteopaciolla.prbe.model;

import lombok.Getter;

import java.util.*;

public class MoveListenersSet {

    @Getter
    private final String matchKeyCode;

    private final Map<Integer, String> playersUrlMap = new TreeMap<>();

    public MoveListenersSet(String matchKeyCode) {
        this.matchKeyCode = matchKeyCode;
    }

    /**
     * Add a player's url to the set of urls
     * @param playerIndex the index of the player who is adding the url
     * @param url the url to be added
     * @return true if the urlMap has been updated, false otherwise
     */
    public boolean addUrl(int playerIndex, String url) {
        if (url.equals(playersUrlMap.get(playerIndex))) {
            return false;
        }
        playersUrlMap.put(playerIndex, url);
        return true;
    }

    public Set<String> getUrls() {
        return new TreeSet<>(playersUrlMap.values());
    }
}