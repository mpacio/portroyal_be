package com.matteopaciolla.portroyal.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class LinkedDeckTest {

    @Test
    public void testGetSliceFromStart() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(), Arrays.asList(1, 2, 3, 4, 5));
        Deck<Integer> slice = deck.getSlice(0, 3);
        assertEquals(Arrays.asList(1, 2, 3), slice.toList());
        assertEquals(Arrays.asList(4, 5), deck.toList());
    }
    
    @Test
    public void testGetSliceFromMiddle() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(), Arrays.asList(1, 2, 3, 4, 5));
        Deck<Integer> slice = deck.getSlice(2, 3);
        assertEquals(Arrays.asList(3, 4, 5), slice.toList());
        assertEquals(Arrays.asList(1, 2), deck.toList());
    }

    @Test
    public void testGetSliceFromEnd() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(), Arrays.asList(1, 2, 3, 4, 5));
        Deck<Integer> slice = deck.getSlice(3, 2);
        assertEquals(Arrays.asList(4, 5), slice.toList());
        assertEquals(Arrays.asList(1, 2, 3), deck.toList());
    }

    @Test
    public void testGetSliceBeyondEnd() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(), Arrays.asList(1, 2, 3, 4, 5));
        assertThrowsExactly(IndexOutOfBoundsException.class, () -> deck.getSlice(3, 6));
    }

    @Test
    public void testGetEmptySlice() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(), Arrays.asList(1, 2, 3, 4, 5));
        Deck<Integer> slice = deck.getSlice(3, 0);
        assertEquals(Collections.emptyList(), slice.toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), deck.toList());
    }

    @Test
    public void testInsertTop() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(110), Arrays.asList(1, 2, 3, 4, 5));
        Deck<Integer> toInsert = new LinkedDeck<>(new Random(110), Arrays.asList(6, 7, 8));
        deck.insertTop(toInsert);
        assertEquals(Arrays.asList(6, 7, 8, 1, 2, 3, 4, 5), deck.toList());
    }

    @Test
    public void testInsertBottom() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(111), Arrays.asList(1, 2, 3, 4, 5));
        Deck<Integer> toInsert = new LinkedDeck<>(new Random(111), Arrays.asList(6, 7, 8));
        deck.insertBottom(toInsert);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), deck.toList());
    }

    @Test
    public void testGetFirst() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(), Arrays.asList(1, 2, 3, 4, 5));
        Deck<Integer> first = deck.getFirst(3);
        assertEquals(Arrays.asList(1, 2, 3), first.toList());
    }

    @Test
    public void testGetLast() {
        Deck<Integer> deck = new LinkedDeck<>(new Random(), Arrays.asList(1, 2, 3, 4, 5));
        Deck<Integer> last = deck.getLast(3);
        assertEquals(Arrays.asList(5, 4, 3), last.toList());
    }

    @Test
    public void testLoopInsertTop() {
        Deck<Integer> deck = new LinkedDeck<>(new Random());
        for (int i = 0; i < 10; i++) {
            deck.insertTop(i);
        }
        assertEquals(Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), deck.toList());
    }
}
