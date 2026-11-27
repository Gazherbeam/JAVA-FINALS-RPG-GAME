package main;

// One looping quest: collect 10 "Trash or scrap" (dropped by any enemy). Resets when completed.
public class Quest {

    public static final String NAME = "Trash or scrap";
    public static final int REQUIRED = 10;

    private static final Item SCRAP = new Item(NAME, "Junk left behind by monsters.", false, null,
            Item.Category.MATERIAL, 99, 0);

    private final GamePanel gp;
    public int timesCompleted = 0;

    public Quest(GamePanel gp) {
        this.gp = gp;
    }

    // Call when any enemy dies.
    public void onEnemyKilled() {
        gp.inventory.add(SCRAP, 1);
        if (getProgress() >= REQUIRED) {
            gp.inventory.remove(findSlot(), REQUIRED);   // hand in the scrap; quest starts over
            timesCompleted++;
        }
    }

    // How many Trash or scrap the player has.
    public int getProgress() {
        int slot = findSlot();
        return slot < 0 ? 0 : gp.inventory.getCount(slot);
    }

    public String getText() {
        return "Quest: " + NAME + " " + getProgress() + "/" + REQUIRED + "   Done: " + timesCompleted;
    }

    private int findSlot() {
        for (int i = 0; i < Inventory.SIZE; i++) {
            Item item = gp.inventory.getItem(i);
            if (item != null && item.name.equals(NAME)) return i;
        }
        return -1;
    }
}