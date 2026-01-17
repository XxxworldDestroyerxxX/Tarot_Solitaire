package com.example.tarotsolitaire;

import java.util.ArrayList;
import java.util.List;

public class Pile {

    private final List<Card> cards = new ArrayList<>();

    /**
     * Determines if a given card can be placed on this pile based on game rules.
     * @param cardToPlace The card the user is trying to place.
     * @return True if the move is legal, false otherwise.
     */
    public boolean canPlaceCard(Card cardToPlace) {
        // Rule 1: Any card can be placed on an empty pile.
        if (this.isEmpty()) {
            return true;
        }

        // Rule 2: If not empty, check the rules against the top card.
        Card topCard = this.getTopCard();
        if (topCard == null) return false; // Safety check

        // Delegate the rule check to the Card's logic.
        return cardToPlace.canBePlacedOn(topCard);
    }

    /**
     * Returns how much vertical stack offset (as a fraction of pile height) should be applied
     * per card in this pile when laying out card views. Default is 0.28 (28%).
     * Special piles can override this to return 0 so cards fully overlap.
     */
    public float getStackOffsetMultiplier() {
        return 0.28f;
    }

    // --- Standard list management methods ---
    public void addCard(Card card) {
        if (!cards.contains(card)) {
            cards.add(card);
            card.setPile(this);
        }
    }

    public void removeCard(Card card) {
        if (cards.contains(card)) {
            cards.remove(card);
            card.setPile(null);
        }
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
}
