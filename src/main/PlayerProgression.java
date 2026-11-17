package main;

/** Owns player EXP and level-up stat growth, then syncs gains to existing gameplay stats. */
public class PlayerProgression {
    public int level = 1;
    public int experience = 0;
    public int experienceToNext = 100;

    public int maxHP = 100;
    public int maxMP = 30;
    public int attack = 10;
    public int defense = 0;
    public double movementSpeed = 4.0;
    public double criticalChance = 0.05;
    public double attackSpeed = 1.0;

    /** Adds EXP, processes multiple level-ups, and returns the number of levels gained. */
    public int awardExperience(int amount, GamePanel gp) {
        if (amount <= 0) return 0;
        experience += amount;
        int gained = 0;
        while (experience >= experienceToNext) {
            experience -= experienceToNext;
            level++;
            gained++;
            maxHP += 10;
            maxMP += 5;
            attack += 2;
            defense += 1;
            movementSpeed += 0.1;
            criticalChance = Math.min(0.50, criticalChance + 0.01);
            attackSpeed += 0.025;
            experienceToNext = 100 + (level - 1) * 50;
        }
        if (gained > 0 && gp != null && gp.Player != null) {
            applyTo(gp);
            gp.playerHP = gp.playerMaxHP;
            gp.playerMP = gp.playerMaxMP;
        }
        return gained;
    }

    public void applyTo(GamePanel gp) {
        gp.playerMaxHP = maxHP;
        gp.playerMaxMP = maxMP;
        gp.Player.speed = movementSpeed;
        gp.Player.attackDamage = attack;
        gp.Player.defense = defense;
    }

    public double experienceRatio() {
        return experienceToNext <= 0 ? 0 : experience / (double) experienceToNext;
    }
}
