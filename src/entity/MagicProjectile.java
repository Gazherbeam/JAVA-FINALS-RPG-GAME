
package entity;

import main.GamePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class MagicProjectile {

    private final GamePanel gp;

    private double x;
    private double y;

    private final double dx;
    private final double dy;

    private final int damage;
    private final int sourceX;
    private final int sourceY;

    private final int radius = 10;
    private int life = 90;

    public boolean alive = true;

    public MagicProjectile(GamePanel gp, int startX, int startY,
                           String direction, int damage) {

        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.damage = damage;

        this.sourceX = startX;
        this.sourceY = startY;

        double speed = 8.0;

        switch (direction) {
            case "up":
                dx = 0;
                dy = -speed;
                break;

            case "down":
                dx = 0;
                dy = speed;
                break;

            case "left":
                dx = -speed;
                dy = 0;
                break;

            default:
                dx = speed;
                dy = 0;
                break;
        }
    }

    public void update() {

        if (!alive) return;

        x += dx;
        y += dy;

        life--;

        if (life <= 0) {
            alive = false;
            return;
        }

        Rectangle area = new Rectangle(
                (int) x - radius,
                (int) y - radius,
                radius * 2,
                radius * 2
        );

        // Stop when the projectile hits a wall.
        if (gp.tileM.isSolid(
                area.x, area.y, area.width, area.height)) {

            alive = false;
            return;
        }

        // Damage enemies in contact with the projectile.
        int hits = gp.enemyM.hitEnemies(
                area,
                damage,
                sourceX,
                sourceY,
                5
        );

        if (hits > 0) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {

        if (!alive) return;

        int screenX = (int) x - gp.Player.camX;
        int screenY = (int) y - gp.Player.camY;

        // Outer magical glow.
        g2.setColor(new Color(80, 140, 255, 100));
        g2.fillOval(
                screenX - 15,
                screenY - 15,
                30,
                30
        );

        // Main projectile.
        g2.setColor(new Color(80, 170, 255));
        g2.fillOval(
                screenX - radius,
                screenY - radius,
                radius * 2,
                radius * 2
        );

        // Bright center.
        g2.setColor(Color.WHITE);
        g2.fillOval(
                screenX - 4,
                screenY - 4,
                8,
                8
        );
    }
}
