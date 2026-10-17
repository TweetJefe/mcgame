package game.state;


/** The catalog is deliberately empty until actual items are designed. */
public record ItemSlot(String itemId, int charges, int cooldown) {
    public ItemSlot {
        if (charges < 0 || cooldown < 0 || (itemId != null && itemId.isBlank()))
            throw new IllegalArgumentException("Invalid item slot");
    }
    public static ItemSlot empty() { return new ItemSlot(null, 0, 0); }
    public boolean isEmpty() { return itemId == null; }
}
