package com.example.tarotsolitaire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class Deck {

    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        // Add standard cards (exclude Aces explicitly by starting at rank 2)
        for (Card.Suit suit : Card.Suit.values()) {
            for (int rank = 2; rank <= 13; rank++) {
                cards.add(new Card(suit, rank));
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

    public List<Card> getCards() {
        return cards;
    }
}
