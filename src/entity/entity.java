package entity;

import main.GamePanel;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

// Shared base for everything that lives in the world: the player, enemies, NPCs, companions.
// x, y = top-left corner of the drawn sprite, in WORLD pixels.
public class entity {

    public int x, y;
    public int speed;

    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;
    public String direction;

    // ----- Hitbox (collision with walls) -----
    // The drawn sprite is 96x96 but the body is much smaller, so collision uses a box
    // around the feet. These are offsets from (x, y).
    public int hitX = 33, hitY = 50, hitW = 30, hitH = 22;

    // ----- Hurtbox (where bullets can hit) -----
    // The visible body of the sprite. It is bigger than the feet box above, so a bullet
    // touching any part of the character registers. Set to 0, 0, 96, 96 to use the whole image.
    public int hurtX = 24, hurtY = 14, hurtW = 48, hurtH = 66;

    // ----- Combat stats -----
    public int maxHP = 100, hp = 100;
    public int defense = 0;
    public double knockbackResist = 0;    // 0 = full knockback, 1 = immune
    public int invincibleTimer = 0;       // update ticks of invulnerability left
    public int invincibleTime = 30;       // how long a hit makes you invulnerable (30 ticks = 0.5 s)

    // ----- Knockback -----
    public double kbX, kbY;               // current push, pixels per tick
    public int kbTimer = 0;               // ticks of push left (also acts as hit-stun)
    private double kbRemX, kbRemY;

    // Leftover fractions of a pixel for moveBy()
    private double moveRemX, moveRemY;

    // ===== POSITION AND HITBOX =====

    public Rectangle hitbox()  { return new Rectangle(x + hitX, y + hitY, hitW, hitH); }
    public Rectangle hurtbox() { return new Rectangle(x + hurtX, y + hurtY, hurtW, hurtH); }
    public int centerX() { return x + hitX + hitW / 2; }
    public int centerY() { return y + hitY + hitH / 2; }

    // Puts the entity so its hitbox is centered on a tile (clamped inside the map border).
    public void placeOnTile(GamePanel gp, int col, int row) {
        int ts = gp.tileSize;
        col = Math.max(1, Math.min(col, gp.tileM.worldCols - 2));
        row = Math.max(1, Math.min(row, gp.tileM.worldRows - 2));
        x = col * ts - hitX + (ts - hitW) / 2;
        y = row * ts - hitY + (ts - hitH) / 2;
    }

    // ===== COLLISION MOVEMENT =====
    // Moves one pixel at a time so the entity ends exactly against a wall.
    // X and Y are separate, so you slide along walls instead of sticking to them.
    // Returns true if it moved the full distance.
    public boolean moveWithCollision(GamePanel gp, int dx, int dy) {
        boolean full = true;

        int sx = Integer.signum(dx);
        for (int i = 0; i < Math.abs(dx); i++) {
            if (gp.tileM.isSolid(x + sx + hitX, y + hitY, hitW, hitH)) { full = false; break; }
            x += sx;
        }
        int sy = Integer.signum(dy);
        for (int i = 0; i < Math.abs(dy); i++) {
            if (gp.tileM.isSolid(x + hitX, y + sy + hitY, hitW, hitH)) { full = false; break; }
            y += sy;
        }
        return full;
    }

    // Movement with fractional speeds, in ANY direction (this is what makes diagonals work).
    // Give it a velocity vector; a speed of 2.83 moves 2 px on some ticks and 3 px on others,
    // so a diagonal walk is exactly as fast as a straight one.
    // Returns true if it moved the full distance (false if a wall stopped it).
    public boolean moveBy(GamePanel gp, double vx, double vy) {
        moveRemX += vx;
        moveRemY += vy;
        int mx = (int) moveRemX, my = (int) moveRemY;
        moveRemX -= mx;
        moveRemY -= my;
        return moveWithCollision(gp, mx, my);
    }

    // ===== DAMAGE AND KNOCKBACK =====

    // Returns true if the hit landed (false while invulnerable or already dead).
    // fromX, fromY = where the attack came from (the push goes away from this point).
    public boolean takeDamage(int rawDamage, int fromX, int fromY, double knockbackPower) {
        if (invincibleTimer > 0 || isDead()) return false;

        int dmg = Math.max(1, rawDamage - defense);
        applyDamage(dmg);
        invincibleTimer = invincibleTime;

        double power = knockbackPower * (1.0 - knockbackResist);
        if (power > 0) {
            double dx = centerX() - fromX, dy = centerY() - fromY;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1) { dx = 0; dy = 1; len = 1; }
            kbX = dx / len * power;
            kbY = dy / len * power;
            kbRemX = kbRemY = 0;
            kbTimer = 10;
        }
        return true;
    }

    // Subclasses override these two when their HP lives somewhere else (the player's HP is in GamePanel).
    protected void applyDamage(int dmg) { hp = Math.max(0, hp - dmg); }
    public boolean isDead() { return hp <= 0; }

    // Call once per update.
    public void tickCombat() {
        if (invincibleTimer > 0) invincibleTimer--;
    }

    // Applies the push, slowing it down each tick. Can't push through solid tiles.
    public void updateKnockback(GamePanel gp) {
        if (kbTimer <= 0) return;
        kbRemX += kbX;
        kbRemY += kbY;
        int mx = (int) kbRemX, my = (int) kbRemY;
        kbRemX -= mx;
        kbRemY -= my;
        moveWithCollision(gp, mx, my);
        kbX *= 0.8;
        kbY *= 0.8;
        kbTimer--;
        if (kbTimer == 0) { kbX = kbY = 0; kbRemX = kbRemY = 0; }
    }
}
