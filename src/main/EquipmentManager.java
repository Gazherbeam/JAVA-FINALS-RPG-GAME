package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.EquipmentList.Equipment;
import main.EquipmentList.Slot;
import main.EquipmentList.Stat;

/**
 * Keeps track of the equipment the player OWNS and what is currently WORN.
 * The definitions (names, stats) live in EquipmentList.
 *
 * To hand the player a piece (a chest, a shop, an enemy drop...):
 *     gp.equipment.give("iron_sword");
 */
public class EquipmentManager {

    private final GamePanel gp;

    // Every piece that exists, by id.
    private final Map<String, Equipment> all = new LinkedHashMap<>();

    // Ids of the pieces the player owns, in the order they were obtained.
    private final Set<String> owned = new LinkedHashSet<>();

    // What is worn in each slot (a missing entry means the slot is empty).
    private final Map<Slot, Equipment> equipped = new EnumMap<>(Slot.class);

    public EquipmentManager(GamePanel gp) {
        this.gp = gp;

        for (Equipment piece : EquipmentList.defaults()) {
            register(piece);
        }

        giveStarterGear();
    }

    public void register(Equipment piece) {
        if (piece == null) {
            throw new IllegalArgumentException("Equipment is required");
        }
        all.put(piece.id, piece);
    }

    // Edit this to change what the player starts with. Nothing is worn at the start,
    // so the stats stay exactly as before until the player equips something.
    private void giveStarterGear() {
        give("rusty_blade");
        give("mage_rod");
        give("cloth_hood");
        give("leather_vest");
        give("swift_charm");
        give("mana_ring");
    }

    // ===== OWNING =====

    // Adds a piece to what the player owns. Returns false if the id is unknown or already owned.
    public boolean give(String id) {
        if (id == null || !all.containsKey(id) || owned.contains(id)) return false;
        owned.add(id);
        return true;
    }

    public boolean isOwned(String id) {
        return owned.contains(id);
    }

    // Owned pieces that fit a slot.
    public List<Equipment> getOwned(Slot slot) {
        List<Equipment> result = new ArrayList<>();
        for (String id : owned) {
            Equipment piece = all.get(id);
            if (piece != null && piece.slot == slot) result.add(piece);
        }
        return Collections.unmodifiableList(result);
    }

    // ===== WEARING =====

    public Equipment getEquipped(Slot slot) {
        return equipped.get(slot);
    }

    public boolean isEquipped(Equipment piece) {
        return piece != null && equipped.get(piece.slot) == piece;
    }

    // Wears a piece the player owns, replacing whatever was in that slot.
    public boolean equip(Equipment piece) {
        if (piece == null || !owned.contains(piece.id)) return false;
        equipped.put(piece.slot, piece);
        refreshStats();
        return true;
    }

    public void unequip(Slot slot) {
        if (equipped.remove(slot) != null) {
            refreshStats();
        }
    }

    // ===== STATS =====

    // The stat from the player's level, before any gear.
    public double getBase(Stat stat) {
        switch (stat) {
            case ATTACK:  return gp.progression.attack;
            case DEFENSE: return gp.progression.defense;
            case MAX_HP:  return gp.progression.maxHP;
            case MAX_MP:  return gp.progression.maxMP;
            case SPEED:   return gp.progression.movementSpeed;
            default:      return 0;
        }
    }

    // What all the worn gear adds to a stat.
    public double getBonus(Stat stat) {
        double total = 0;
        for (Equipment piece : equipped.values()) {
            total += piece.get(stat);
        }
        return total;
    }

    // Same as getBonus, but pretends "replacement" (or nothing, if null) is worn in "slot".
    // The equipment menu uses this to preview a change before it is made.
    public double getBonusWith(Stat stat, Slot slot, Equipment replacement) {
        double total = 0;
        for (Slot s : Slot.values()) {
            Equipment piece = (s == slot) ? replacement : equipped.get(s);
            if (piece != null) total += piece.get(stat);
        }
        return total;
    }

    public double getTotal(Stat stat) {
        return getBase(stat) + getBonus(stat);
    }

    public double getTotalWith(Stat stat, Slot slot, Equipment replacement) {
        return getBase(stat) + getBonusWith(stat, slot, replacement);
    }

    // Pushes the new totals into the game and keeps HP / MP from sitting above the new maximums.
    private void refreshStats() {
        gp.progression.applyTo(gp);
        gp.playerHP = Math.min(gp.playerHP, gp.playerMaxHP);
        gp.playerMP = Math.min(gp.playerMP, gp.playerMaxMP);
    }
}