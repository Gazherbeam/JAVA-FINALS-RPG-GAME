package entity;

import main.GamePanel;

import java.awt.*;

// A bullet fired by an enemy. Dies when it hits a wall, the player, or runs out of life.
public class projectile {

    GamePanel gp;
    double x, y, vx, vy;
    int damage;
    int radius = 7;
    int life = 240;                 // ticks before it disappears (4 seconds)
    public boolean alive = true;

    public projectile(GamePanel gp, double startX, double startY, double targetX, double targetY,
                      double speed, int damage) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        double dx = targetX - startX, dy = targetY - startY;
        double len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        vx = dx / len * speed;
        vy = dy / len * speed;
    }

    public void update() {
        x += vx;
        y += vy;
        if (--life <= 0) { alive = false; return; }

        // Walls stop bullets
        if (gp.tileM.isSolid((int) x - 2, (int) y - 2, 4, 4)) { alive = false; return; }

        // Hit the player
        Rectangle box = new Rectangle((int) x - radius, (int) y - radius, radius * 2, radius * 2);
        if (gp.playerHP > 0 && box.intersects(gp.Player.hitbox())) {
            gp.Player.takeDamage(damage, (int) (x - vx * 4), (int) (y - vy * 4), 6);
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {
        int sx = (int) x - gp.Player.camX, sy = (int) y - gp.Player.camY;
        g2.setColor(new Color(255, 140, 30));
        g2.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
        g2.setColor(new Color(255, 230, 150));
        g2.fillOval(sx - radius / 2, sy - radius / 2, radius, radius);
    }
}