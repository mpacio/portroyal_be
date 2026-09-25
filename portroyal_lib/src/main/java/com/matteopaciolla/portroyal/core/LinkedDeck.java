package com.matteopaciolla.portroyal.core;

import java.util.*;
import java.util.function.Consumer;

public class LinkedDeck<E> implements Deck<E> {

    private List<E> internalColl;
    private final Random random;

    public LinkedDeck(Random random, List<E> list){
        this.internalColl = new LinkedList<>(list);
        this.random = random;
    }
    public LinkedDeck(Random random){
        this(random, new LinkedList<>());
    }

    public Deck<E> getNewDeck(){
        return new LinkedDeck<>(this.random);
    }

    public Deck<E> getNewDeck(List<E> list){
        return new LinkedDeck<>(this.random, list);
    }

    private int randomIndex(){
        return random.nextInt(this.size());
    }

    /**
     * Shuffles the elements in the deck.
     */
    @Override
    public void shuffle() {
        if (!isEmpty()) {
            List<E> shuffled = new LinkedList<>();
            for (int i = this.size(); i > 0; i--) {
                shuffled.add(this.getOne(random.nextInt(i)));
            }
            this.internalColl = shuffled;
        }
    }

    /**
     * Reverses the order of the elements in the deck.
     */
    @Override
    public void reverse() {
        Collections.reverse(internalColl);
    }

    @Override
    public E getFirst() {
        return this.getOne(0);
    }

    @Override
    public Deck<E> getFirst(int number) {
        return this.getSlice(0, number);
    }

    @Override
    public E getLast() {
        return getOne(this.size() - 1);
    }

    @Override
    public Deck<E> getLast(int number) {
        Deck<E> res = getSlice(size() - number, number);
        res.reverse();
        return res;
    }

    @Override
    public E getRandom() {
        return getOne(randomIndex());
    }

    @Override
    public Deck<E> getRandom(int number) {
        Deck<E> res = getNewDeck();
        for (int i = 0; i < number; i++) {
            res.insertTop(this.getRandom());
        }
        return res;
    }

    /**
     * Removes and returns the element at the specified index in the deck.
     *
     * @param  index  the index of the element to remove
     * @return        the removed element
     */
    @Override
    public E getOne(int index) {
        return internalColl.remove(index);
    }
    
    /**
     * Returns a new Deck containing the specified range of elements in the deck.
     *
     * @param  fromIndex  the starting index of the range to be returned
     * @param  number     the number of elements to be returned
     * @return            a new Deck containing the specified range of elements
     * @throws IllegalArgumentException if fromIndex or number is negative
     * @throws IndexOutOfBoundsException if fromIndex is greater than or equal to the size of the deck,
     *                                   or fromIndex + number is greater than the size of the deck
     */
    @Override
    public Deck<E> getSlice(int fromIndex, int number) {
        if(fromIndex < 0 || number < 0){
            throw new IllegalArgumentException();
        }
        if (fromIndex >= this.size() || fromIndex + number > this.size()) {
            throw new IndexOutOfBoundsException();
        }
        int toIndex = fromIndex + number;
        List<E> templist = internalColl.subList(fromIndex, toIndex);
        Deck<E> res = getNewDeck(templist);
        List<E> part1;
        if (fromIndex == 0) {
            part1 = new LinkedList<>();
        } else {
            part1 = internalColl.subList(0, fromIndex);
        }
        if (number < this.size()) {
            List<E> part2 = internalColl.subList(toIndex, this.size());
            part1.addAll(part2);
        }
        internalColl = part1;
        return res;
    }

    @Override
    public void insert(int index, E elem) {
        this.internalColl.add(index, elem);
    }

    @Override
    public void insertTop(E elem) {
        this.internalColl.addFirst(elem);
    }

    @Override
    public void insertTop(Deck<E> deck) {
        List<E> tmp = this.internalColl;
        this.internalColl = deck.toList();
        this.internalColl.addAll(tmp);
        
    }

    @Override
    public void insertBottom(E elem) {
        this.internalColl.add(elem);
    }

    @Override
    public void insertBottom(Deck<E> deck) {
        this.internalColl.addAll(deck.toList());
    }

    @Override
    public void insertRandom(E elem) {
        this.internalColl.add(random.nextInt(size() + 1), elem);
    }

    @Override
    public boolean isEmpty() {
        return internalColl.isEmpty();
    }

    @Override
    public int size() {
        return internalColl.size();
    }

    @Override
    public List<E> toList() {
        return internalColl;
    }

    @Override
    public String toString() {
        return internalColl.toString();
    }

    @Override
    public Iterator<E> iterator() {
        return this.internalColl.iterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        this.internalColl.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return this.internalColl.spliterator();
    }
}
