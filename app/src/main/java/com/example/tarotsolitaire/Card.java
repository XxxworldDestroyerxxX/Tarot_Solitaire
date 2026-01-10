package com.example.tarotsolitaire;

public class Card {

    public enum Suit { HEARTS, DIAMONDS, CLUBS, SPADES }

    private Suit suit;
    private int rank;        // 1 = Ace, 11 = Jack, etc.
    private Pile currentPile; // logical pile this card belongs to

    public Card(Suit suit, int rank) {
        this.suit = suit;
        this.rank = rank;
        this.currentPile = null;
    }

    // --- NEW METHOD TO ENFORCE GAME RULES ---
    /**
     * Checks if this card can be legally placed on top of another card.
     * @param topCard The card currently at the top of the pile.
     * @return True if the placement is legal, false otherwise.
     */
    public boolean canBePlacedOn(Card topCard) {
        // Rule 1: Must be the same suit.
        if (this.getSuit() != topCard.getSuit()) {
            return false;
        }

        return Math.abs(this.getRank() - topCard.getRank()) == 1;
    }


    /* ---------- GETTERS ---------- */
    public Suit getSuit() { return suit; }
    public int getRank() { return rank; }
    public Pile getCurrentPile() { return currentPile; }

    /* ---------- SETTER ---------- */
    public void setPile(Pile pile) {
        this.currentPile = pile;
    }

    /* ---------- HELPERS FOR DISPLAY ---------- */
    public String getRankString() {
        switch (rank) {
            case 1:  return "A";
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            default: return String.valueOf(rank);
        }
    }

    public String getSuitSymbol() {
        switch (suit) {
            case HEARTS:   return "♥";
            case DIAMONDS: return "♦";
            case CLUBS:    return "♣";
            case SPADES:   return "♠";
        }
        return "?";
    }
}
