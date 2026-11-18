package entity;

import main.GamePanel;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

// Owns every enemy and enemy projectile: updates them, draws them, and handles hits on them.
public class EnemyManager {

    GamePanel gp;
    public final List<enemy> enemies = new ArrayList<>();
    public final List<projectile> projectiles = new ArrayList<>();

    public EnemyManager(GamePanel gp) {
        this.gp = gp;
        spawnDefaults();
    }

    // The placeholder bullet-hell monster, 4 tiles from the player's start (tile 10, 10).
    // Other types you can spawn the same way: "melee", "fast", "tank", "elite", "ranged".
    public void spawnDefaults() {
        enemies.clear();
        projectiles.clear();
        spawn("dummy", 14, 10);
        spawn("melee", 10, 14);    // 10 EXP
        spawn("fast", 6, 10);      // 12 EXP
        spawn("ranged", 10, 6);    // 15 EXP
        spawn("ranged", 15,10);
        spawn("shooter", 16,10);


        System.out.println("Enemies spawned: " + enemies.size());
    }

    public void respawnAll() { spawnDefaults(); }

    // Places an enemy on a tile, or on the nearest free tile if that one is a wall.
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

    public void update() {
        for (enemy e : enemies) e.update();
        for (projectile p : projectiles) p.update();
        enemies.removeIf(enemy::isRemovable);
        projectiles.removeIf(p -> !p.alive);
    }

    // Draws enemies (lowest on screen last, so they overlap correctly), then bullets.
    public void draw(Graphics2D g2) {
        List<enemy> sorted = new ArrayList<>(enemies);
        sorted.sort((a, b) -> Integer.compare(a.y, b.y));
        for (enemy e : sorted) e.draw(g2);
        for (projectile p : projectiles) p.draw(g2);
    }

    // Damages every living enemy touching the area. Returns how many were hit.
    public int hitEnemies(Rectangle area, int damage, int fromX, int fromY, double knockback) {
        int hits = 0;
        for (enemy e : enemies) {
            if (e.state == enemy.State.DEAD) continue;
            if (area.intersects(e.hitbox()) && e.takeDamage(damage, fromX, fromY, knockback)) hits++;
        }
        return hits;
    }
}