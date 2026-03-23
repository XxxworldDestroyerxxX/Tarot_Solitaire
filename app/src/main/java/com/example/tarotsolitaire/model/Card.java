package com.example.tarotsolitaire.model;

@SuppressWarnings("unused")
public class Card {

    public enum Suit { HEARTS, DIAMONDS, CLUBS, SPADES }
    public enum Type { STANDARD, TAROT }

    private final Type type;
    private final Suit suit; // only for STANDARD
    private final int rank;        // For STANDARD: 1 = Ace ... 13 = King. For TAROT: 0..21
    private Pile currentPile; // logical pile this card belongs to

    // Standard card constructor
    public Card(Suit suit, int rank) {
        this.type = Type.STANDARD;
        this.suit = suit;
        this.rank = rank;
        this.currentPile = null;
    }

    // Tarot card constructor (numbered 0..21)
    public Card(int tarotNumber) {
        this.type = Type.TAROT;
        this.suit = null;
        this.rank = tarotNumber;
        this.currentPile = null;
    }

    // --- PLACEMENT RULES ---
    /**
     * Checks if this card can be legally placed on top of another card.
     * Placement rules:
     * - STANDARD cards: may be placed on top of another STANDARD card of the same suit and rank difference of 1.
     * - TAROT cards: may be placed on another TAROT card whose number differs by exactly 1.
     * Note: empty-pile placement is handled by Pile (pile allows any card on empty piles).
     */
    public boolean canBePlacedOn(Card topCard) {
        if (topCard == null) return false; // safety; empty piles are checked earlier

        // If both are tarot, allow +/-1
        if (this.type == Type.TAROT) {
            if (topCard.type != Type.TAROT) return false;
            return Math.abs(this.rank - topCard.rank) == 1;
        }

        // If this is standard, require same suit and +/-1 rank
        if (this.type == Type.STANDARD) {
            if (topCard.type != Type.STANDARD) return false;
            if (this.suit != topCard.suit) return false;
            return Math.abs(this.rank - topCard.rank) == 1;
        }

        return false;
    }

    /* ---------- GETTERS ---------- */
    public Type getType() { return type; }
    public Suit getSuit() { return suit; }
    public int getRank() { return rank; }
    @SuppressWarnings("unused")
    public Pile getCurrentPile() { return currentPile; }

    /* ---------- SETTER ---------- */
    public void setPile(Pile pile) {
        this.currentPile = pile;
    }

    /* ---------- HELPERS FOR DISPLAY ---------- */
    public String getRankString() {
        if (type == Type.TAROT) {
            return String.valueOf(rank);
        }
        switch (rank) {
            case 1:  return "A";
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            default: return String.valueOf(rank);
        }
    }

    public String getSuitSymbol() {
        if (type == Type.TAROT) {
            return "T"; // simple marker for tarot cards
        }
        // Guard against null suit to satisfy static analysis (tarot uses null suit)
        if (suit == null) return "?";
        switch (suit) {
            case HEARTS:   return "♥";
            case DIAMONDS: return "♦";
            case CLUBS:    return "♣";
            case SPADES:   return "♠";
        }
        return "?";
    }
}
