package entity;

import main.GamePanel;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Owns every enemy and enemy projectile: updates them, draws them, and handles hits on them.
public class EnemyManager {

    GamePanel gp;
    public final List<enemy> enemies = new ArrayList<>();
    public final List<projectile> projectiles = new ArrayList<>();
    public final List<MagicProjectile> magicProjectiles = new ArrayList<>();
    private static final int MAX_SPAWNED = 4;
    private static final int SPAWN_INTERVAL = 120;
 // ticks between spawn attempts (2 s)
    private static final int MIN_PLAYER_DIST = 6;    // tiles
    private static final String[] SPAWN_TYPES = {"melee", "fast", "tank", "elite", "ranged", "shooter"};
    private final Random rng = new Random();
    private int spawnTimer = SPAWN_INTERVAL;

    public EnemyManager(GamePanel gp) {
        this.gp = gp;
        spawnDefaults();
    }

    // The placeholder bullet-hell monster, 4 tiles from the player's start (tile 10, 10).
    // Other types you can spawn the same way: "melee", "fast", "tank", "elite", "ranged".
    public void spawnDefaults() {
        enemies.clear();
        projectiles.clear();


        System.out.println("Enemies spawned: " + enemies.size());
    }

    public void respawnAll() { spawnDefaults(); }

    public void spawn(String type, int col, int row) {
        enemy e = new enemy(gp, type);
        for (int radius = 0; radius <= 3; radius++) {
            for (int dc = -radius; dc <= radius; dc++) {
                for (int dr = -radius; dr <= radius; dr++) {
                    if (Math.max(Math.abs(dc), Math.abs(dr)) != radius) continue;
                    e.placeOnTile(gp, col + dc, row + dr);
                    if (!gp.tileM.isSolid(e.x + e.hitX, e.y + e.hitY, e.hitW, e.hitH)) {
                        enemies.add(e);
                        return;
                    }
                }
            }
        }
        System.out.println("EnemyManager: no free tile near (" + col + ", " + row + ") for " + type);
    }
    // Spawns one random monster on a random free tile, keeping at most MAX_SPAWNED alive.
    private void spawnRandom() {

        if (++spawnTimer < SPAWN_INTERVAL) return;
        spawnTimer = 0;

        int alive = 0;
        for (enemy e : enemies) {
            if (e.state != enemy.State.DEAD && !e.type.equals("dummy")) alive++;
        }
        if (alive >= MAX_SPAWNED) return;

        int ts = gp.tileSize;
        for (int tries = 0; tries < 20; tries++) {
            int col = 1 + rng.nextInt(gp.tileM.worldCols - 2);
            int row = 1 + rng.nextInt(gp.tileM.worldRows - 2);
            if (gp.tileM.isSolidTile(col, row)) continue;

            int dx = col * ts + ts / 2 - gp.Player.centerX();
            int dy = row * ts + ts / 2 - gp.Player.centerY();
            if (dx * dx + dy * dy < (MIN_PLAYER_DIST * ts) * (MIN_PLAYER_DIST * ts)) continue;

            spawn(SPAWN_TYPES[rng.nextInt(SPAWN_TYPES.length)], col, row);
            return;
        }
    }
    public void update() {
        spawnRandom();
        for (enemy e : enemies) e.update();
        for (projectile p : projectiles) p.update();
        for (MagicProjectile p : magicProjectiles) p.update();
        magicProjectiles.removeIf(p -> !p.alive);
        enemies.removeIf(enemy::isRemovable);
        projectiles.removeIf(p -> !p.alive);
    }

    // Draws enemies (lowest on screen last, so they overlap correctly), then bullets.
    public void draw(Graphics2D g2) {
        List<enemy> sorted = new ArrayList<>(enemies);
        sorted.sort((a, b) -> Integer.compare(a.y, b.y));
        for (enemy e : sorted) e.draw(g2);
        for (projectile p : projectiles) p.draw(g2);
        for (MagicProjectile p : magicProjectiles) p.draw(g2);
    }

    // Damages every living enemy touching the area. Returns how many were hit.

    public int hitEnemies(Rectangle area, int damage, int fromX, int fromY, double knockback) {
        int hits = 0;

        for (enemy e : enemies) {
            if (e.state == enemy.State.DEAD) continue;

            if (area.intersects(e.hitbox())
                    && e.takeDamage(damage, fromX, fromY, knockback)) {
                hits++;
            }
        }

        // Restore 2 MP for every enemy successfully hit.
        if (hits > 0) {
            int manaRestored = hits * 2;

            gp.playerMP = Math.min(
                    gp.playerMaxMP,
                    gp.playerMP + manaRestored
            );
        }

        return hits;
    }

}