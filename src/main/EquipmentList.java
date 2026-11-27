package main;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * EDITABLE EQUIPMENT REGISTRY.
 * Add, remove or change equipment pieces and their stat bonuses here.
 * (Which pieces the player owns / wears is handled by EquipmentManager.)
 */
public final class EquipmentList {
    private EquipmentList() { }

    // The places gear can be worn. Add a new one here and it shows up in the menu automatically.
    public enum Slot {
        WEAPON("Weapon"), HEAD("Head"), BODY("Body"), ACCESSORY("Accessory");

        public final String label;

        Slot(String label) {
            this.label = label;
        }
    }

    // The stats gear can change. These are added on top of the level-based stats.
    public enum Stat {
        ATTACK("ATK"), DEFENSE("DEF"), MAX_HP("HP"), MAX_MP("MP"), SPEED("SPD");

        public final String label;

        Stat(String label) {
            this.label = label;
        }
    }

    public static final class Equipment {
        public final String id;
        public final String name;
        public final String description;
        public final Slot slot;

        // Stat bonuses (use negative numbers for a penalty).
        public final int attack;
        public final int defense;
        public final int maxHP;
        public final int maxMP;
        public final double speed;

        public Equipment(String id, String name, String description, Slot slot,
                         int attack, int defense, int maxHP, int maxMP, double speed) {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Equipment id is required");
            }
            if (slot == null) {
                throw new IllegalArgumentException("Equipment slot is required");
            }
            this.id = id;
            this.name = name;
            this.description = description == null ? "" : description;
            this.slot = slot;
            this.attack = attack;
            this.defense = defense;
            this.maxHP = maxHP;
            this.maxMP = maxMP;
            this.speed = speed;
        }

        // The bonus this piece gives to one stat.
        public double get(Stat stat) {
            switch (stat) {
                case ATTACK:  return attack;
                case DEFENSE: return defense;
                case MAX_HP:  return maxHP;
                case MAX_MP:  return maxMP;
                case SPEED:   return speed;
                default:      return 0;
            }
        }

        // For example "ATK +3  DEF +1". Only stats that actually change are listed.
        public String bonusText() {
            StringBuilder text = new StringBuilder();
            for (Stat stat : Stat.values()) {
                double value = get(stat);
                if (Math.abs(value) < 0.0001) continue;
                if (text.length() > 0) text.append("  ");
                text.append(stat.label).append(' ');
                if (stat == Stat.SPEED) {
                    text.append(String.format(Locale.US, "%+.1f", value));
                } else {
                    text.append(String.format(Locale.US, "%+d", (int) Math.round(value)));
                }
            }
            return text.length() == 0 ? "No bonuses" : text.toString();
        }
    }

    public static List<Equipment> defaults() {
        return Collections.unmodifiableList(Arrays.asList(

                // ----- Weapons -----
                new Equipment("rusty_blade", "Rusty Blade",
                        "An old blade with a dull edge. Better than nothing.",
                        Slot.WEAPON, 3, 0, 0, 0, 0),
                new Equipment("iron_sword", "Iron Sword",
                        "A sturdy, well-balanced sword made for real fights.",
                        Slot.WEAPON, 7, 0, 0, 0, 0),
                new Equipment("mage_rod", "Mage Rod",
                        "A short rod that hums softly with stored mana.",
                        Slot.WEAPON, 3, 0, 0, 10, 0),

                // ----- Head -----
                new Equipment("cloth_hood", "Cloth Hood",
                        "A simple hood. It will not stop much, but it helps.",
                        Slot.HEAD, 0, 1, 0, 0, 0),
                new Equipment("iron_helm", "Iron Helm",
                        "A heavy helmet that protects your head but slows you a little.",
                        Slot.HEAD, 0, 2, 10, 0, -0.1),

                // ----- Body -----
                new Equipment("leather_vest", "Leather Vest",
                        "Light, flexible armor that does not get in your way.",
                        Slot.BODY, 0, 2, 0, 0, 0),
                new Equipment("chain_mail", "Chain Mail",
                        "Interlocked metal rings. Strong, but quite heavy.",
                        Slot.BODY, 0, 4, 0, 0, -0.2),

                // ----- Accessories -----
                new Equipment("swift_charm", "Swift Charm",
                        "A small charm that makes your steps feel lighter.",
                        Slot.ACCESSORY, 0, 0, 0, 0, 0.3),
                new Equipment("mana_ring", "Mana Ring",
                        "A silver ring that widens your pool of magic.",
                        Slot.ACCESSORY, 0, 0, 0, 15, 0),
                new Equipment("vitality_amulet", "Vitality Amulet",
                        "An amulet that fills the wearer with stubborn life.",
                        Slot.ACCESSORY, 0, 0, 20, 0, 0)
        ));
    }
}