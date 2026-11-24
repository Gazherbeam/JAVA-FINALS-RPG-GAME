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
    boolean attackLatch = false;   // one attack per Z press
    int attackBufferTimer = 0;     // a Z press is remembered this many ticks (so Z just before X still swings in the dash)
    static final int ATTACK_BUFFER_TICKS = 8;

    // ----- Attack settings -----
    int attackTicks = 0;                    // how long the current swing has lasted
    static final int ATTACK_HIT_TICK = 8;   // the swing damages at this tick (about when the blade is out)
    public int attackDamage = 40;          // same as the C attack (BIG_SWING_DAMAGE)
    public double attackKnockback = 18;    // same as the C attack (BIG_SWING_KNOCKBACK)
    int attackCooldownTimer = 0;           // Z uses the same cooldown as C (BIG_SWING_COOLDOWN)

    // ----- Dash settings (press X) -----
    public static final int DASH_TICKS = 10;                       // how long a dash lasts (10 ticks = 0.17 s)
    public static final int DASH_SPEED = 14;                       // pixels per tick
    public static final int DASH_DISTANCE = DASH_TICKS * DASH_SPEED;   // 140 px, a bit under 3 tiles
    public static final int DASH_COOLDOWN = 40;                    // ticks after a dash before the next one
    static final int GHOST_EVERY = 2;                              // drop an after-image every 2 ticks

    boolean dashing = false;       // true = invincible, moving fast
    int dashTimer = 0;             // ticks of dash left
    int dashCooldownTimer = 0;
    boolean dashLatch = false;     // one dash per key press
    double dashDX, dashDY;         // dash direction (unit vector)

    // ----- Big swing settings (press C) -----
    public static final int BIG_SWING_COOLDOWN = 90;
    public static final int BIG_SWING_DAMAGE = 40;
    public static final double BIG_SWING_KNOCKBACK = 18;
    static final int BIG_SWING_REACH = 100, BIG_SWING_WIDTH = 100;
    boolean bigSwinging = false;
    int bigSwingCooldownTimer = 0;
    boolean bigSwingLatch = false;
    public final SlashEffect slashFx = new SlashEffect();

    // The white after-images: they last 14 ticks and start at 50% opacity
    public final AfterImages ghosts = new AfterImages(14, 0.5f);

    // Turn this on to see the hurtbox (where bullets can hit) as a red outline
    static final boolean DEBUG_HURTBOX = false;

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

    // The walk / idle animation for whichever way the player is facing
    private Animation walkAnim() {
        switch (direction) {
            case "up":    return walkUp;
            case "left":  return walkLeft;
            case "right": return walkRight;
            default:      return walkDown;
        }
    }

    private Animation idleAnim() {
        switch (direction) {
            case "up":    return idleUp;
            case "left":  return idleLeft;
            case "right": return idleRight;
            default:      return idleDown;
        }
    }

    // ===== DAMAGE (the player's HP lives in GamePanel so the UI bar keeps working) =====

    @Override protected void applyDamage(int dmg) { gp.playerHP = Math.max(0, gp.playerHP - dmg); }
    @Override public boolean isDead() { return gp.playerHP <= 0; }

    // Invincibility frames: nothing can hurt the player while dashing.
    // (Bullets check the return value, so they fly right through.)
    @Override
    public boolean takeDamage(int rawDamage, int fromX, int fromY, double knockbackPower) {
        if (dashing) return false;
        return super.takeDamage(rawDamage, fromX, fromY, knockbackPower);
    }

    public boolean isDashing() { return dashing; }
    public boolean isMoving() { return moving; }

    // True when a dash could be started right now (enemies peek at this to read you)
    public boolean dashReady() { return !dashing && !bigSwinging && dashCooldownTimer <= 0 && kbTimer <= 0; }

    public boolean isBigSwinging() { return bigSwinging; }
    public boolean bigSwingReady() { return !bigSwinging && bigSwingCooldownTimer <= 0 && kbTimer <= 0; }
    // 0 = ready, 1 = just used. Handy for a cooldown icon in the HUD.
    public double bigSwingCooldownRatio() { return bigSwingCooldownTimer / (double) BIG_SWING_COOLDOWN; }

    // Death = respawn at the start with full HP and fresh enemies (until a game-over screen exists).
    private void respawn() {
        gp.playerHP = gp.playerMaxHP;
        setDefaultValues();
        attacking = false;
        dashing = false;
        dashTimer = 0;
        dashCooldownTimer = 0;
        bigSwinging = false;
        bigSwingCooldownTimer = 0;
        attackCooldownTimer = 0;
        ghosts.clear();
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
        ghosts.tick();                                   // after-images keep fading even after the dash
        if (dashCooldownTimer > 0) dashCooldownTimer--;
        if (bigSwingCooldownTimer > 0) bigSwingCooldownTimer--;
        if (attackBufferTimer > 0) attackBufferTimer--;
        if (attackCooldownTimer > 0) attackCooldownTimer--;
        slashFx.tick();

        // Hit-stun: while being knocked back, the player can't act
        if (kbTimer > 0) {
            attacking = false;
            bigSwinging = false;
            attackBufferTimer = 0;
            updateKnockback(gp);
            updateCamera();
            return;
        }

        // Pressing X and C together starts the dash and big swing as one combo.
        boolean dashSwingCombo = KeyH.dashPressed && KeyH.bigSwingPressed
                && !dashLatch && !bigSwingLatch && dashReady() && bigSwingReady();
        if (dashSwingCombo) {
            dashLatch = true;
            bigSwingLatch = true;
            startDash();
            startAttack(true);
        }

        // ----- DASH -----
        // One dash per key press. Dashing also cancels a sword swing, like a dodge-cancel.
        if (!dashSwingCombo && KeyH.dashPressed) {
            if (!dashLatch) {
                dashLatch = true;
                if (dashReady()) startDash();
            }
        } else if (!KeyH.dashPressed) {
            dashLatch = false;
        }

        // The big swing can be started during a dash; its attack animation and hit run alongside the dash.
        if (!dashSwingCombo && KeyH.bigSwingPressed) {
            if (!bigSwingLatch) {
                bigSwingLatch = true;
                if (!attacking && bigSwingReady()) startAttack(true);
            }
        } else if (!KeyH.bigSwingPressed) {
            bigSwingLatch = false;
        }

        // ----- ATTACK (Z) -----
        // Allowed during a dash too: the swing starts right away and keeps going after the dash.
        if (KeyH.attackPressed && !attackLatch) attackBufferTimer = ATTACK_BUFFER_TICKS;
        if (KeyH.attackPressed && !attacking && !attackLatch && attackCooldownTimer <= 0) {
            attackLatch = true;
            startAttack(false);
        }
        if (!KeyH.attackPressed) attackLatch = false;

        // Z pressed just before X: the dash cancelled that swing, so swing again inside the dash
        if (dashing && !attacking && attackBufferTimer > 0 && attackCooldownTimer <= 0) {
            attackBufferTimer = 0;
            startAttack(false);
        }

        if (dashing) {
            updateDash();
            updateCamera();
            return;   // no walking mid-dash (a swing keeps running alongside it)
        }

        if (attacking) {
            updateAttack();
            updateCamera();
            return;   // no walking while attacking
        }

        // ----- MOVEMENT (8 directions, with tile collision) -----
        moving = false;

        int dx = (KeyH.rightPressed ? 1 : 0) - (KeyH.leftPressed ? 1 : 0);
        int dy = (KeyH.downPressed  ? 1 : 0) - (KeyH.upPressed   ? 1 : 0);

        if (dx != 0 || dy != 0) {
            // Dividing by the length keeps diagonals the same speed as straight lines
            double len = Math.sqrt(dx * dx + dy * dy);
            moveBy(gp, dx / len * speed, dy / len * speed);
            faceInput(dx, dy);
            moving = true;
        }

        Animation next = moving ? walkAnim() : idleAnim();
        if (next != currentAnim) {
            currentAnim = next;
            currentAnim.reset();
        }
        currentAnim.tick();

        updateCamera();
    }

    // The sprite sheet only has 4 directions. Going diagonally keeps the current facing if it
    // matches one of the held keys (so it doesn't flicker), otherwise it faces left/right.
    private void faceInput(int dx, int dy) {
        String h = dx > 0 ? "right" : "left";
        String v = dy > 0 ? "down" : "up";
        if (dx != 0 && dy != 0) {
            if (!direction.equals(h) && !direction.equals(v)) direction = h;
        } else if (dx != 0) {
            direction = h;
        } else if (dy != 0) {
            direction = v;
        }
    }

    // ===== DASH =====

    // Dashes the way you are holding the keys (diagonals included), or forward if no key is held.
    private void startDash() {
        int dx = (KeyH.rightPressed ? 1 : 0) - (KeyH.leftPressed ? 1 : 0);
        int dy = (KeyH.downPressed  ? 1 : 0) - (KeyH.upPressed   ? 1 : 0);

        if (dx == 0 && dy == 0) {                       // no keys held: dash where you're facing
            switch (direction) {
                case "up":    dy = -1; break;
                case "left":  dx = -1; break;
                case "right": dx = 1;  break;
                default:      dy = 1;  break;
            }
        }
        double len = Math.sqrt(dx * dx + dy * dy);
        dashDX = dx / len;
        dashDY = dy / len;
        faceInput(dx, dy);

        dashing = true;
        gp.soundEffect.play("Invisibility Smoke Sound.wav");
        dashTimer = DASH_TICKS;
        attacking = false;
        moving = true;
        currentAnim = walkAnim();
        currentAnim.reset();

        ghosts.add(currentAnim.current(), x, y);        // first after-image where the dash began

        // Enemies that are paying attention take note of this dash (they learn your habits)
        for (enemy e : gp.enemyM.enemies) e.onPlayerDash(dashDX, dashDY);
    }

    private void updateDash() {
        moveBy(gp, dashDX * DASH_SPEED, dashDY * DASH_SPEED);
        if (attacking) {
            updateAttack();
            if (!attacking) {
                currentAnim = walkAnim();
                currentAnim.reset();
            }
        } else {
            currentAnim.tick();
        }
        if (dashTimer % GHOST_EVERY == 0) ghosts.add(currentAnim.current(), x, y);

        if (--dashTimer <= 0) {
            dashing = false;
            dashCooldownTimer = DASH_COOLDOWN;
        }
    }

    private void startAttack(boolean bigSwing) {
        attacking = true;
        bigSwinging = bigSwing;
        attackTicks = 0;
        switch (direction) {
            case "up":    currentAnim = attackUp;    break;
            case "down":  currentAnim = attackDown;  break;
            case "left":  currentAnim = attackLeft;  break;
            case "right": currentAnim = attackRight; break;
        }
        currentAnim.reset();
        if (!bigSwing) {
            slashFx.start();   // the Slash Effect plays on Z only (not on C)
            gp.soundEffects.play("Sword Slash (Rpg).wav");
        }
    }

    private void updateAttack() {
        currentAnim.tick();
        attackTicks++;
        if (attackTicks == ATTACK_HIT_TICK) {
            if (bigSwinging) bigSwing();
            else swing();
        }
        if (currentAnim.isFinished()) {
            attacking = false;
            if (bigSwinging) {
                bigSwinging = false;
                bigSwingCooldownTimer = BIG_SWING_COOLDOWN;
            } else {
                attackCooldownTimer = BIG_SWING_COOLDOWN;
            }
        }
    }

    // The sword swing: damages every enemy inside a box in front of the player.
    private void swing() {
        swing(BIG_SWING_REACH, BIG_SWING_WIDTH, attackDamage, attackKnockback);
    }

    private void bigSwing() {
        swing(BIG_SWING_REACH, BIG_SWING_WIDTH, BIG_SWING_DAMAGE, BIG_SWING_KNOCKBACK);
    }

    private void swing(int reach, int width, int damage, double knockback) {
        int cx = centerX(), cy = centerY();
        Rectangle area;
        switch (direction) {
            case "up":    area = new Rectangle(cx - width / 2, cy - reach, width, reach); break;
            case "left":  area = new Rectangle(cx - reach, cy - width / 2, reach, width); break;
            case "right": area = new Rectangle(cx, cy - width / 2, reach, width); break;
            default:      area = new Rectangle(cx - width / 2, cy, width, reach); break;
        }
        gp.enemyM.hitEnemies(area, damage, cx, cy, knockback);
    }

    // ===== DRAW =====

    public void draw(Graphics2D g2) {
        int size = gp.tileSize * 2;

        // After-images go underneath the real sprite
        ghosts.draw(g2, camX, camY, size);

        // Blink while invulnerable after a hit
        boolean blinkOff = invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0;

        if (!blinkOff) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(currentAnim.current(), screenX, screenY, size, size, null);

            if (DEBUG_HURTBOX) {
                g2.setColor(Color.RED);
                g2.drawRect(screenX + hurtX, screenY + hurtY, hurtW, hurtH);
            }
        }

        slashFx.draw(g2, centerX() - camX, centerY() - camY, direction);
    }
}