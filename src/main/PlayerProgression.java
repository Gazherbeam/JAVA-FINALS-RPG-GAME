package main;

/** Owns player EXP and level-up stat growth, then syncs gains to existing gameplay stats. */
public class PlayerProgression {
    public static final int MAX_LEVEL = 20;

    /**
     * EXP needed to go from level N to N+1 is EXP_TO_NEXT[N - 1].
     * 19 level-ups (Lv 1 -> Lv 20). The values add up to exactly 4,095 EXP.
     */
    public static final int[] EXP_TO_NEXT = {
            35, 45, 60, 75, 90, 110, 130, 150, 170, 195,
            220, 250, 275, 305, 335, 365, 395, 430, 460
    };
    public static final int TOTAL_EXP_TO_MAX = 4095;

    // ----- Attributes: index 0..5 = STR, VIT, DEX, INT, AGI, LCK -----
    public static final int STR = 0, VIT = 1, DEX = 2, INT = 3, AGI = 4, LCK = 5;
    private static final int[] ATTR_AT_LV1   = {  8, 12,  7, 10, 14,  5 };
    private static final int[] ATTR_AT_LV20  = { 42, 55, 38, 47, 52, 33 };
    public final int[] attributes = ATTR_AT_LV1.clone();

    public int level = 1;
    public int experience = 0;
    public int experienceToNext = EXP_TO_NEXT[0];

    public int maxHP = 100;
    public int maxMP = 30;
    public int attack = 10;
    public int defense = 0;
    public double movementSpeed = 4.0;
    public double criticalChance = 0.05;
    public double attackSpeed = 1.0;

    public PlayerProgression() {
        recalculateStats();
    }

    /** Adds EXP, processes multiple level-ups, and returns the number of levels gained. */
    public int awardExperience(int amount, GamePanel gp) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        experience += amount;
        int gained = 0;
        while (level < MAX_LEVEL && experience >= experienceToNext) {
            experience -= experienceToNext;
            level++;
            gained++;
            experienceToNext = level >= MAX_LEVEL ? 0 : EXP_TO_NEXT[level - 1];
        }
        if (level >= MAX_LEVEL) experience = 0;   // nothing left to earn at the cap
        if (gained > 0) {
            recalculateStats();
            if (gp != null && gp.Player != null) {
                applyTo(gp);
                gp.playerHP = gp.playerMaxHP;
                gp.playerMP = gp.playerMaxMP;
            }
        }
        return gained;
    }

    /** Sets every attribute for the current level (straight line from the Lv 1 value to the Lv 20 value). */
    private void recalculateStats() {
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = ATTR_AT_LV1[i]
                    + (int) Math.round((ATTR_AT_LV20[i] - ATTR_AT_LV1[i]) * (level - 1) / (double) (MAX_LEVEL - 1));
        }
        // Attributes -> gameplay stats. At Lv 1 these equal the old starting values.
        maxHP = 100 + (attributes[VIT] - ATTR_AT_LV1[VIT]) * 5;
        maxMP = 30 + (attributes[INT] - ATTR_AT_LV1[INT]) * 2;
        attack = 10 + (attributes[STR] - ATTR_AT_LV1[STR]);
        defense = (attributes[VIT] - ATTR_AT_LV1[VIT]) / 4;
        movementSpeed = 4.0 + (attributes[AGI] - ATTR_AT_LV1[AGI]) * 0.02;
        criticalChance = 0.05 + (attributes[LCK] - ATTR_AT_LV1[LCK]) * 0.005;
        attackSpeed = 1.0 + (attributes[DEX] - ATTR_AT_LV1[DEX]) * 0.01;
    }

    public void applyTo(GamePanel gp) {
        gp.playerMaxHP = maxHP;
        gp.playerMaxMP = maxMP;
        gp.Player.speed = movementSpeed;
        gp.Player.attackDamage = attack;
        gp.Player.defense = defense;
    }

    public double experienceRatio() {
        if (level >= MAX_LEVEL) return 1.0;
        return experienceToNext <= 0 ? 0 : experience / (double) experienceToNext;
    }
}
