package main;

/** Immutable item definition. */
public class Item {
    public enum Category { WEAPON, ARMOR, CONSUMABLE, QUEST, MATERIAL, MISCELLANEOUS }

    public final String name;
    public final String description;
    public final boolean consumable;
    public final String iconFile;
    public final Category category;
    public final int maxStack;
    public final int healAmount;

    public Item(String name, String description, boolean consumable, String iconFile) {
        this(name, description, consumable, iconFile,
                consumable ? Category.CONSUMABLE : Category.MISCELLANEOUS,
                consumable ? 99 : 1, 0);
    }

    public Item(String name, String description, boolean consumable, String iconFile,
                Category category, int maxStack, int healAmount) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Item name cannot be blank");
        if (category == null) throw new IllegalArgumentException("Item category cannot be null");
        if (maxStack < 1) throw new IllegalArgumentException("Maximum stack must be positive");
        if (healAmount < 0) throw new IllegalArgumentException("Healing amount cannot be negative");
        this.name = name;
        this.description = description == null ? "" : description;
        this.consumable = consumable;
        this.iconFile = iconFile;
        this.category = category;
        this.maxStack = maxStack;
        this.healAmount = healAmount;
    }

    public boolean stacksWith(Item other) {
        return other != null && name.equals(other.name) && category == other.category
                && consumable == other.consumable && healAmount == other.healAmount
                && maxStack == other.maxStack;
    }
}
