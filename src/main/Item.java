package main;

// One kind of item. Make one Item object per item type, then put copies into the Inventory.
public class Item {

    public final String name;
    public final String description;
    public final boolean consumable;   // true = used up when you press Enter on it
    public final String iconFile;      // optional png in res/items/ (e.g. "potion.png"), or null

    public Item(String name, String description, boolean consumable, String iconFile) {
        this.name = name;
        this.description = description;
        this.consumable = consumable;
        this.iconFile = iconFile;
    }
}