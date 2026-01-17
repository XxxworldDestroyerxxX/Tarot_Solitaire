package com.example.tarotsolitaire;

public class SpecialPile extends Pile {

    // Functional interface for deciding if a card may be placed on this special pile
    public interface PlacementRule {
        boolean canPlace(Pile pile, Card cardToPlace);
    }

    private final PlacementRule rule;

    public SpecialPile(PlacementRule rule) {
        this.rule = rule;
    }

    public SpecialPile() {
        // Default placement rule: only TAROT cards may be placed on special piles
        this((pile, card) -> card != null && card.getType() == Card.Type.TAROT);
    }

    @Override
    public boolean canPlaceCard(Card cardToPlace) {
        if (cardToPlace == null) return false;
        // Empty pile allowed only if rule accepts placement on empty pile
        if (this.isEmpty()) {
            return rule.canPlace(this, cardToPlace);
        }
        // Otherwise also run the rule (it can choose to check top card via pile.getTopCard())
        return rule.canPlace(this, cardToPlace);
    }

    @Override
    public float getStackOffsetMultiplier() {
        // Special piles overlay cards (no vertical offset)
        return 0f;
    }
}

