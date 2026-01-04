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
