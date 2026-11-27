package main;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import main.SpellList.Spell;
import main.SpellList.Effect;

public class SpellManager {

    private final GamePanel gp;

    private final Map<String, Spell> spells = new LinkedHashMap<>();

    // Quick-cast slots: X, C, V
    private final String[] quickBinds = new String[3];

    private final int[] cooldowns = new int[3];

    public SpellManager(GamePanel gp) {
        this.gp = gp;

        for (Spell spell : SpellList.defaults()) {
            register(spell);
        }

        quickBinds[0] = "dash";
        quickBinds[1] = "mend";
        quickBinds[2] = "arc_bolt";
    }

    public void register(Spell spell) {
        if (spell == null || spell.id == null
                || spell.id.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Spell and spell id are required"
            );
        }

        spells.put(spell.id, spell);
    }

    public List<Spell> getSpells() {
        return Collections.unmodifiableList(
                new ArrayList<>(spells.values())
        );
    }

    public Spell getSpell(String id) {
        return spells.get(id);
    }

    public Spell getBoundSpell(int slot) {
        if (slot < 0 || slot >= quickBinds.length) {
            return null;
        }

        return spells.get(quickBinds[slot]);
    }

    public String getBindName(int slot) {
        return new String[]{"X", "C", "V"}[slot];
    }

    public int getBindCount() {
        return quickBinds.length;
    }

    // Ticks left before the spell in this quick-cast slot can be used again (HUD uses this).
    public int getCooldown(int slot) {
        if (slot < 0 || slot >= cooldowns.length) {
            return 0;
        }

        return cooldowns[slot];
    }

    public void assign(int spellIndex, int slot) {
        List<Spell> list = getSpells();

        if (spellIndex < 0 || spellIndex >= list.size()
                || slot < 0 || slot >= quickBinds.length) {
            return;
        }

        quickBinds[slot] = list.get(spellIndex).id;
    }

    public void update() {
        for (int i = 0; i < cooldowns.length; i++) {
            if (cooldowns[i] > 0) {
                cooldowns[i]--;
            }
        }
    }

    public boolean castBound(int slot) {
        Spell spell = getBoundSpell(slot);

        if (spell == null || cooldowns[slot] > 0) {
            return false;
        }

        if (gp.playerMP < spell.manaCost) {
            return false;
        }

        if (!apply(spell)) {
            return false;
        }

        gp.playerMP -= spell.manaCost;

        cooldowns[slot] =
                spell.effect == Effect.DASH ? 40 : 18;

        return true;
    }

    private boolean apply(Spell spell) {

        switch (spell.effect) {

            case DASH:
                if (!gp.Player.dashReady()) {
                    return false;
                }

                gp.Player.activateDashSpell();
                return true;

            case HEAL:
                if (gp.playerHP >= gp.playerMaxHP) {
                    return false;
                }

                gp.playerHP = Math.min(
                        gp.playerMaxHP,
                        gp.playerHP + 35
                );

                return true;

            case ARC_BOLT:

                gp.enemyM.magicProjectiles.add(
                        new entity.MagicProjectile(
                                gp,
                                gp.Player.centerX(),
                                gp.Player.centerY(),
                                gp.Player.direction,
                                28
                        )
                );

                return true;

            default:
                return false;
        }
    }
}