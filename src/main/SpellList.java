
package main;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * EDITABLE SPELL REGISTRY.
 * Add, remove or change spell definitions here.
 */
public final class SpellList {
    private SpellList() { }

    public enum Effect {
        DASH, HEAL, ARC_BOLT
    }

    public static final class Spell {
        public final String id;
        public final String name;
        public final String description;
        public final int manaCost;
        public final Effect effect;

        public Spell(String id, String name, String description,
                     int manaCost, Effect effect) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.manaCost = manaCost;
            this.effect = effect;
        }
    }

    public static List<Spell> defaults() {
        return Collections.unmodifiableList(Arrays.asList(
                new Spell(
                        "dash",
                        "Dash",
                        "Burst forward in the direction you are moving. Grants brief invulnerability.",
                        0,
                        Effect.DASH
                ),
                new Spell(
                        "mend",
                        "Mend",
                        "Restore 35 HP.",
                        15,
                        Effect.HEAL
                ),
                new Spell(
                        "arc_bolt",
                        "Arc Bolt",
                        "Release a short-range bolt of magic in front of you.",
                        10,
                        Effect.ARC_BOLT
                )
        ));
    }
}
