package com.example.tarotsolitaire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class Deck {

    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        // Add standard cards (optionally skipping aces as per existing code)
        for (Card.Suit suit : Card.Suit.values()) {
            for (int rank = 1; rank <= 13; rank++) {
                if (rank != 1) { // existing rule: skip Ace if intended
                    cards.add(new Card(suit, rank));
                }
            }
        }

        // Add tarot deck cards 0..21
        for (int t = 0; t <= 21; t++) {
            cards.add(new Card(t));
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    @SuppressWarnings("unused")
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    @SuppressWarnings("unused")
    public Card drawCard() {
        if (cards.isEmpty()) return null;
        return cards.remove(cards.size() - 1);
    }

    public List<Card> getCards() {
        return cards;
    }
}
