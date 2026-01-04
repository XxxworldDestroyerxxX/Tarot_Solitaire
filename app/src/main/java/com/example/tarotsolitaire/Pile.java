package com.example.tarotsolitaire;

import java.util.ArrayList;
import java.util.List;

public class Pile {

    private final List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(card);
        card.setPile(this);
    }

    public void removeCard(Card card) {
        cards.remove(card);
        card.setPile(null);
    }

    public Card getTopCard() {
        if (cards.isEmpty()) return null;
        return cards.get(cards.size() - 1);
    }

    public List<Card> getCards() {
        return cards;
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int size() {
        return cards.size();
    }
}
