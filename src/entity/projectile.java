package entity;

import main.GamePanel;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

// A bullet fired by an enemy. Dies when it hits a wall, the player, or runs out of life.
public class projectile {

    private static final String TRAIL_SHEET = "/Basic Bullet Trail Free/Part 1/Part1_Column_1.png";
    private static final int TRAIL_TILE_SIZE = 32;
    private static final int TRAIL_TILE_COUNT = 16;
    private static final long BULLET_FRAME_TIME_MS = 50;
    private static final BufferedImage[] SHOOTER_BULLET_SPRITES = loadShooterBulletSprites();

    GamePanel gp;
    double x, y, vx, vy;
    private final boolean shooterProjectile;
    private final Animation tailBulletAnimation;
    private final Animation headBulletAnimation;
    private double rotationDegrees;
    int damage;
    int radius = 7;
    int life = 240;                 // ticks before it disappears (4 seconds)
    public boolean alive = true;

    public projectile(GamePanel gp, double startX, double startY, double targetX, double targetY,
                      double speed, int damage) {
        this(gp, startX, startY, targetX, targetY, speed, damage, false);
    }

    public projectile(GamePanel gp, double startX, double startY, double targetX, double targetY,
                      double speed, int damage, boolean shooterProjectile) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        double dx = targetX - startX, dy = targetY - startY;
        double len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        vx = dx / len * speed;
        vy = dy / len * speed;
        this.shooterProjectile = shooterProjectile && SHOOTER_BULLET_SPRITES != null;
        tailBulletAnimation = this.shooterProjectile ? createBulletAnimation(0) : null;
        headBulletAnimation = this.shooterProjectile ? createBulletAnimation(1) : null;
        rotationDegrees = Math.toDegrees(Math.atan2(vy, vx));
    }

    // Copilot: Edit - Load 32x32 tiles for looping TailBullet and HeadBullet animations.
    private static BufferedImage[] loadShooterBulletSprites() {
        try {
            java.net.URL resource = projectile.class.getResource(TRAIL_SHEET);
            if (resource == null) throw new IOException("Missing resource " + TRAIL_SHEET);
            BufferedImage sheet = ImageIO.read(resource);
            if (sheet == null) throw new IOException("Unsupported image " + TRAIL_SHEET);
            if (sheet.getWidth() < TRAIL_TILE_SIZE * TRAIL_TILE_COUNT
                    || sheet.getHeight() < TRAIL_TILE_SIZE) {
                throw new IOException("Sprite sheet is too small for 16 32x32 tiles");
            }
            BufferedImage[] sprites = new BufferedImage[TRAIL_TILE_COUNT];
            for (int tile = 0; tile < TRAIL_TILE_COUNT; tile++) {
                sprites[tile] = sheet.getSubimage(tile * TRAIL_TILE_SIZE, 0,
                        TRAIL_TILE_SIZE, TRAIL_TILE_SIZE);
            }
            return sprites;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Projectile: could not load shooter bullet sprites: " + e.getMessage());
            return null;
        }
    }

    // Copilot: Add - Loop TailBullet through even tiles and HeadBullet through odd tiles until the projectile dies.
    private static Animation createBulletAnimation(int startingTile) {
        BufferedImage[] frames = new BufferedImage[TRAIL_TILE_COUNT / 2];
        for (int frame = 0; frame < frames.length; frame++) {
            frames[frame] = SHOOTER_BULLET_SPRITES[startingTile + frame * 2];
        }
        return new Animation(frames, BULLET_FRAME_TIME_MS, true);
    }

    public void setRotationDegrees(double rotationDegrees) {
        if (!Double.isFinite(rotationDegrees)) {
            throw new IllegalArgumentException("Rotation must be a finite number of degrees");
        }
        this.rotationDegrees = rotationDegrees;
    }

    public void update() {
        x += vx;
        y += vy;
        if (shooterProjectile) {
            tailBulletAnimation.tick();
            headBulletAnimation.tick();
        }
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
        if (shooterProjectile) {
            int size = 32;
            double angle = Math.toRadians(rotationDegrees);
            double tailOffset = size * 0.8;
            drawSprite(g2, tailBulletAnimation.current(),
                    sx - Math.cos(angle) * tailOffset, sy - Math.sin(angle) * tailOffset, size);
            drawSprite(g2, headBulletAnimation.current(), sx, sy, size);
            return;
        }
        g2.setColor(new Color(255, 140, 30));
        g2.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
        g2.setColor(new Color(255, 230, 150));
        g2.fillOval(sx - radius / 2, sy - radius / 2, radius, radius);
    }

    private void drawSprite(Graphics2D g2, BufferedImage sprite, double centerX, double centerY, int size) {
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX - size / 2.0, centerY - size / 2.0);
        transform.rotate(Math.toRadians(rotationDegrees), size / 2.0, size / 2.0);
        transform.scale(size / (double) sprite.getWidth(), size / (double) sprite.getHeight());
        g2.drawImage(sprite, transform, null);
    }
}