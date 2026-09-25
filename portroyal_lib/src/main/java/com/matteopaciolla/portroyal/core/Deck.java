package com.matteopaciolla.portroyal.core;

import java.util.List;

public interface Deck<E> extends Iterable<E>{

    void shuffle();
    void reverse();
    E getFirst();
    Deck<E> getFirst(int number);
    E getLast();
    Deck<E> getLast(int number);
    E getRandom();
    Deck<E> getRandom(int number);
    E getOne(int index);
    Deck<E> getSlice(int fromIndex, int number);
    void insert(int index, E elem);
    void insertTop(E elem);
    void insertTop(Deck<E> deck);
    void insertBottom(E elem);
    void insertBottom(Deck<E> deck);
    void insertRandom(E elem);
    boolean isEmpty();
    int size();
    List<E> toList();
}
