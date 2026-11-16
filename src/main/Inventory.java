package main;

// Holds the player's items in a fixed number of slots. Items with the same name stack.
// This class only stores data. UI draws it and GamePanel handles the keys.
public class Inventory {

    public static final int COLS = 5;
    public static final int ROWS = 4;
    public static final int SIZE = COLS * ROWS;
    public static final int MAX_STACK = 99;

    private final Item[] items = new Item[SIZE];
    private final int[] counts = new int[SIZE];

    public Item getItem(int slot) {
        return inBounds(slot) ? items[slot] : null;
    }

    public int getCount(int slot) {
        return inBounds(slot) ? counts[slot] : 0;
    }

    // Adds to an existing stack, or the first empty slot.
    // Returns false (and adds nothing) if there's no room.
    public boolean add(Item item, int amount) {
        if (item == null || amount <= 0 || amount > MAX_STACK) return false;

        for (int i = 0; i < SIZE; i++) {
            if (items[i] != null && items[i].name.equals(item.name)) {
                if (counts[i] + amount > MAX_STACK) return false;
                counts[i] += amount;
                return true;
            }
        }
        for (int i = 0; i < SIZE; i++) {
            if (items[i] == null) {
                items[i] = item;
                counts[i] = amount;
                return true;
            }
        }
        return false;
    }

    // Removes some from a slot. The slot empties when it reaches 0.
    public boolean remove(int slot, int amount) {
        if (!inBounds(slot) || items[slot] == null || amount <= 0 || counts[slot] < amount) {
            return false;
        }
        counts[slot] -= amount;
        if (counts[slot] == 0) items[slot] = null;
        return true;
    }

    private boolean inBounds(int slot) {
        return slot >= 0 && slot < SIZE;
    }
}