package entity;

import main.GamePanel;
import main.KeyHandler;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class player extends entity {

    GamePanel gp;
    KeyHandler KeyH;

    Animation walkUp, walkDown, walkLeft, walkRight;
    Animation idleUp, idleDown, idleLeft, idleRight;
    Animation attackUp, attackDown, attackLeft, attackRight;
    Animation currentAnim;
    boolean moving = false;
    boolean attacking = false;
    boolean attackLatch = false;   // one attack per J press

    // ----- Attack settings -----
    int attackTicks = 0;                    // how long the current swing has lasted
    static final int ATTACK_HIT_TICK = 8;   // the swing damages at this tick (about when the blade is out)
    public int attackDamage = 10;
    public double attackKnockback = 9;

    // ----- Camera -----
    // camX, camY = the world position at the top-left of the screen. Clamped to the map.
    public int camX, camY;
    // Where the player sprite is drawn on screen (changes near the map edges)
    public int screenX, screenY;

    public player(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.KeyH = keyH;
        setDefaultValues();
        getPlayerImage();
        updateCamera();
    }



    public void setDefaultValues() {
        speed = 4;
        direction = "down";
        if (gp.tileM != null) placeOnTile(gp, 10, 10);   // start at tile (10, 10)
        else { x = gp.tileSize * 10; y = gp.tileSize * 10; }
    }

    // ===== ANIMATIONS =====

    private Animation makeAnim(SpriteSheet sheet, int row, int startCol, int count, long frameTimeMs) {
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) frames[i] = sheet.get(row, startCol + i);
        return new Animation(frames, frameTimeMs);
    }

    private Animation makeAttackAnim(SpriteSheet sheet, int row, int startCol, int count, long frameTimeMs) {
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) frames[i] = sheet.get(row, startCol + i);
        return new Animation(frames, frameTimeMs, false);
    }

    public void getPlayerImage() {
        try {
            SpriteSheet sheet = new SpriteSheet("/PostApocalypse AssetPack/player/Warrior-Red.png", 32, 32);

            // rows: down = 0, right = 2, up = 4, left = 6
            idleDown  = makeAnim(sheet, 0, 0, 2, 500);
            walkDown  = makeAnim(sheet, 0, 2, 2, 150);
            idleRight = makeAnim(sheet, 2, 0, 2, 500);
            walkRight = makeAnim(sheet, 2, 2, 2, 150);
            idleUp    = makeAnim(sheet, 4, 0, 2, 500);
            walkUp    = makeAnim(sheet, 4, 2, 2, 150);
            idleLeft  = makeAnim(sheet, 6, 0, 2, 500);
            walkLeft  = makeAnim(sheet, 6, 2, 2, 150);

            // Attack: columns x4, x5, x6 -> start column 3, 5 frames
            attackDown  = makeAttackAnim(sheet, 0, 3, 5, 120);
            attackRight = makeAttackAnim(sheet, 2, 3, 5, 120);
            attackUp    = makeAttackAnim(sheet, 4, 3, 5, 120);
            attackLeft  = makeAttackAnim(sheet, 6, 3, 5, 120);

            currentAnim = idleDown;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===== DAMAGE (the player's HP lives in GamePanel so the UI bar keeps working) =====

    @Override protected void applyDamage(int dmg) { gp.playerHP = Math.max(0, gp.playerHP - dmg); }
    @Override public boolean isDead() { return gp.playerHP <= 0; }

    // Death = respawn at the start with full HP and fresh enemies (until a game-over screen exists).
    private void respawn() {
        gp.playerHP = gp.playerMaxHP;
        setDefaultValues();
        attacking = false;
        kbTimer = 0;
        invincibleTimer = 60;
        gp.enemyM.respawnAll();
    }

    // ===== CAMERA =====

    // Follows the player, but never shows anything outside the map.
    public void updateCamera() {
        if (gp.tileM == null) return;
        camX = x + gp.tileSize - gp.screenWidth / 2;    // sprite centered on screen
        camY = y + gp.tileSize - gp.screenHeight / 2;
        camX = Math.max(0, Math.min(camX, gp.tileM.worldWidthPx()  - gp.screenWidth));
        camY = Math.max(0, Math.min(camY, gp.tileM.worldHeightPx() - gp.screenHeight));
        screenX = x - camX;
        screenY = y - camY;
    }

    // ===== UPDATE =====

    public void update() {
        if (isDead()) { respawn(); updateCamera(); return; }

        tickCombat();

        // Hit-stun: while being knocked back, the player can't act
        if (kbTimer > 0) {
            attacking = false;
            updateKnockback(gp);
            updateCamera();
            return;
        }

        // ----- ATTACK -----
        if (KeyH.attackPressed && !attacking && !attackLatch) {
            attacking = true;
            attackLatch = true;
            attackTicks = 0;
            switch (direction) {
                case "up":    currentAnim = attackUp;    break;
                case "down":  currentAnim = attackDown;  break;
                case "left":  currentAnim = attackLeft;  break;
                case "right": currentAnim = attackRight; break;
            }
            currentAnim.reset();
        }
        if (!KeyH.attackPressed) attackLatch = false;

        if (attacking) {
            currentAnim.tick();
            attackTicks++;
            if (attackTicks == ATTACK_HIT_TICK) swing();
            if (currentAnim.isFinished()) attacking = false;
            updateCamera();
            return;   // no walking while attacking
        }

        // ----- MOVEMENT (with tile collision) -----
        moving = false;

        if (KeyH.upPressed) {
            moveWithCollision(gp, 0, -speed);
            direction = "up";
            moving = true;
        } else if (KeyH.downPressed) {
            moveWithCollision(gp, 0, speed);
            direction = "down";
            moving = true;
        } else if (KeyH.leftPressed) {
            moveWithCollision(gp, -speed, 0);
            direction = "left";
            moving = true;
        } else if (KeyH.rightPressed) {
            moveWithCollision(gp, speed, 0);
            direction = "right";
            moving = true;
        }

        Animation next = idleDown;
        switch (direction) {
            case "up":    next = moving ? walkUp    : idleUp;    break;
            case "down":  next = moving ? walkDown  : idleDown;  break;
            case "left":  next = moving ? walkLeft  : idleLeft;  break;
            case "right": next = moving ? walkRight : idleRight; break;
        }
        if (next != currentAnim) {
            currentAnim = next;
            currentAnim.reset();
        }
        currentAnim.tick();

        updateCamera();
    }

    // The sword swing: damages every enemy inside a box in front of the player.
    private void swing() {
        int cx = centerX(), cy = centerY();
        int reach = 60, width = 56;
        Rectangle area;
        switch (direction) {
            case "up":    area = new Rectangle(cx - width / 2, cy - reach, width, reach); break;
            case "left":  area = new Rectangle(cx - reach, cy - width / 2, reach, width); break;
            case "right": area = new Rectangle(cx, cy - width / 2, reach, width); break;
            default:      area = new Rectangle(cx - width / 2, cy, width, reach); break;
        }
        gp.enemyM.hitEnemies(area, attackDamage, cx, cy, attackKnockback);
    }

    // ===== DRAW =====

    public void draw(Graphics2D g2) {
        // Blink while invulnerable after a hit
        if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) return;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(currentAnim.current(), screenX, screenY, gp.tileSize * 2, gp.tileSize * 2, null);
    }
}
