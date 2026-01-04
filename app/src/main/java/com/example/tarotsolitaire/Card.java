package com.example.tarotsolitaire;

public class Card {

    public enum Suit { HEARTS, DIAMONDS, CLUBS, SPADES }

    private Suit suit;
    private int rank; // 1 = Ace, 11 = Jack, 12 = Queen, 13 = King
    private boolean faceUp;

    private Pile pile; // reference to the pile the card belongs to

    public Card(Suit suit, int rank) {
        this.suit = suit;
        this.rank = rank;
        this.faceUp = true; // default face up
    }

    public Suit getSuit() { return suit; }
    public int getRank() { return rank; }
    public boolean isFaceUp() { return faceUp; }
    public void setFaceUp(boolean faceUp) { this.faceUp = faceUp; }

    public Pile getPile() { return pile; }
    public void setPile(Pile pile) { this.pile = pile; }

    @Override
    public String toString() {
        return rank + " of " + suit;
    }
}
