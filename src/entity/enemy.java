package entity;

import main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// One enemy. The type decides its stats and behavior:
//   "melee"  walks up and hits you        "fast"   quick and weak
//   "tank"   slow, tough, hits hard       "elite"  stronger all round
//   "ranged" keeps its distance and shoots one bullet
//   "shooter" the bullet-hell monster: fans, rings and aimed bursts of bullets
// AI states: IDLE (wander) -> CHASE -> ATTACK -> back to CHASE. RETREAT is for ranged enemies. DEAD fades out.
public class enemy extends entity {

    public enum State { IDLE, CHASE, ATTACK, RETREAT, DEAD }

    GamePanel gp;
    public String type;
    public State state = State.IDLE;

    // ----- Stats (set by configure) -----
    double moveSpeed;
    int damage;
    double knockbackPower = 8;
    int detectRadius, loseRadius, attackRange, keepDistance;   // pixels
    int attackCooldown, windup;                                 // ticks
    boolean ranged = false;
    public int xpReward = 10;                                   // EXP given to the player on death (set per type in configure)
    Color tint = Color.WHITE;

    // ----- AI timers -----
    int cooldownTimer = 0, windupTimer = 0, wanderTimer = 0, deathTimer = 0;
    double wanderX = 0, wanderY = 0;
    double remX = 0, remY = 0;
    double playerDashDX, playerDashDY;
    boolean moving = false;

    // ----- Animations: [0 idle, 1 walk, 2 attack][0 down, 1 right, 2 up, 3 left] -----
    Animation[][] anims = new Animation[3][4];
    Animation currentAnim;

    static final String SHEET = "/PostApocalypse AssetPack/player/Warrior-Red.png";
    // Copilot: Add - Provide character sheets for random enemy appearances.
    private static final String[] PUNY_MONSTER_SHEETS = {
            "/PunyMonsters/Black-Panther-32x32.png",
            "/PunyMonsters/Blonde-Dog-32x32.png",
            "/PunyMonsters/Brown-Boar-32x32.png",
            "/PunyMonsters/Brown-Spider-32x32.png",
            "/PunyMonsters/Dirty-Gray-Boar-32x32.png",
            "/PunyMonsters/Gray-Wolf-32x32.png",
            "/PunyMonsters/Navy-Blue-Rat-32x32.png",
            "/PunyMonsters/Orange-Cat-32x32.png",
            "/PunyMonsters/Purple-Spider-32x32.png"
    };
    static final int[] ROWS = {0, 2, 4, 6};   // sprite sheet row for down, right, up, left
    static final Map<String, BufferedImage[][]> FRAME_CACHE = new HashMap<>();
    // Copilot: Edit - Allow configured enemy types to override their randomly selected character sheet.
    private static final Map<String, String> ENEMY_SHEETS = new HashMap<>();
    private final String spriteSheetPath;

    public enemy(GamePanel gp, String type) {
        this.gp = gp;
        this.type = type;
        // Copilot: Edit - Randomly assign each enemy its own PunyMonsters character sheet unless its type is configured.
        String configuredSheet = ENEMY_SHEETS.get(type);
        spriteSheetPath = configuredSheet != null ? configuredSheet
                : PUNY_MONSTER_SHEETS[ThreadLocalRandom.current().nextInt(PUNY_MONSTER_SHEETS.length)];
        this.direction = "down";
        configure(type);
        hp = maxHP;
        buildAnimations();
    }

    // Copilot: Add - Register a 32x32-frame, six-column enemy sheet before spawning that enemy type.
    public static void registerSpriteSheet(String enemyType, String resourcePath) {
        if (enemyType == null || enemyType.trim().isEmpty()) {
            throw new IllegalArgumentException("Enemy type must not be blank");
        }
        if (resourcePath == null || !resourcePath.startsWith("/") || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Sprite resource path must be an absolute classpath path");
        }
        ENEMY_SHEETS.put(enemyType, resourcePath);
        FRAME_CACHE.remove(resourcePath);
    }

    // ===== TYPES =====

    private void configure(String type) {
        int ts = gp.tileSize;
        switch (type) {
            case "fast":
                maxHP = 20; moveSpeed = 3.0; damage = 5; xpReward = 12;
                detectRadius = 6 * ts; attackRange = 44; attackCooldown = 40; windup = 8;
                tint = new Color(255, 255, 110);
                break;
            case "tank":
                maxHP = 90; moveSpeed = 0.9; damage = 18; xpReward = 25;
                defense = 2; knockbackResist = 0.8;
                detectRadius = 4 * ts; attackRange = 56; attackCooldown = 110; windup = 28;
                tint = new Color(120, 160, 255);
                break;
            case "elite":
                maxHP = 70; moveSpeed = 2.2; damage = 14; xpReward = 40;
                defense = 1; knockbackResist = 0.4;
                detectRadius = 6 * ts; attackRange = 56; attackCooldown = 60; windup = 14;
                tint = new Color(225, 120, 255);
                break;
            case "shooter":   // bullet-hell monster: cycles through three bullet patterns
                maxHP = 60; moveSpeed = 1.0; damage = 6; xpReward = 30;
                ranged = true;
                detectRadius = 9 * ts; attackRange = 8 * ts; keepDistance = 3 * ts;
                attackCooldown = 50; windup = 10;
                tint = new Color(255, 90, 90);
                break;
            case "ranged":
                maxHP = 24; moveSpeed = 1.4; damage = 7; xpReward = 15;
                ranged = true;
                detectRadius = 7 * ts; attackRange = 6 * ts; keepDistance = 4 * ts;
                attackCooldown = 100; windup = 18;
                tint = new Color(255, 170, 80);
                break;
            case "dummy":
                maxHP = 999; moveSpeed = 0.0; damage = 0; xpReward = 0;
                ranged = false;
                knockbackResist = 1;
                detectRadius = 0; attackRange = 0; keepDistance = 0;
                tint = new Color(210, 180, 140);
                break;
            default:   // "melee"
                maxHP = 30; moveSpeed = 1.6; damage = 8; xpReward = 10;
                detectRadius = 5 * ts; attackRange = 52; attackCooldown = 70; windup = 16;
                tint = new Color(120, 255, 120);
                break;
        }
        loseRadius = (int) (detectRadius * 1.6);
    }

    // ===== ENEMY SPRITES =====

    private BufferedImage[][] loadFrames() {
        BufferedImage[][] cached = FRAME_CACHE.get(spriteSheetPath);
        if (cached != null) return cached;

        BufferedImage[][] f = new BufferedImage[8][6];
        try {
            SpriteSheet sheet = new SpriteSheet(spriteSheetPath, 32, 32);
            for (int r : ROWS) {
                for (int c = 0; c < 6; c++) {
                    BufferedImage frame = sheet.get(r, c);
                    f[r][c] = spriteSheetPath.equals(SHEET) ? tint(frame, tint) : frame;
                }
            }
        } catch (Exception e) {
            // Sheet missing: draw plain colored squares so enemies are never invisible
            for (int r : ROWS) for (int c = 0; c < 6; c++) f[r][c] = fallback(tint);
        }
        FRAME_CACHE.put(spriteSheetPath, f);
        return f;
    }

    private static BufferedImage tint(BufferedImage src, Color t) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < src.getHeight(); py++) {
            for (int px = 0; px < src.getWidth(); px++) {
                int argb = src.getRGB(px, py);
                int a = argb >>> 24;
                if (a == 0) continue;
                int r = Math.min(255, (int) (((argb >> 16) & 255) * t.getRed()   / 255.0 * 1.35));
                int g = Math.min(255, (int) (((argb >> 8)  & 255) * t.getGreen() / 255.0 * 1.35));
                int b = Math.min(255, (int) ((argb         & 255) * t.getBlue()  / 255.0 * 1.35));
                out.setRGB(px, py, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    private static BufferedImage fallback(Color c) {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(9, 8, 14, 18);
        g.setColor(Color.BLACK);
        g.drawRect(9, 8, 13, 17);
        g.dispose();
        return img;
    }

    private void buildAnimations() {
        BufferedImage[][] f = loadFrames();
        for (int d = 0; d < 4; d++) {
            int r = ROWS[d];
            anims[0][d] = new Animation(new BufferedImage[]{f[r][0], f[r][1]}, 500);
            anims[1][d] = new Animation(new BufferedImage[]{f[r][2], f[r][3]}, 150);
            anims[2][d] = new Animation(new BufferedImage[]{f[r][3], f[r][4], f[r][5]}, 100, false);
        }
        currentAnim = anims[0][0];
    }

    int volley = 0;   // counts attacks so the shooter can cycle its patterns

    // Copilot: Edit - Give shooter shots static TailBullet/HeadBullet sprites without animating them.
    // Fires one bullet at an angle (radians)
    private void fire(double angle, double bulletSpeed) {
        double tx = centerX() + Math.cos(angle) * 100, ty = centerY() + Math.sin(angle) * 100;
        gp.enemyM.projectiles.add(new projectile(gp, centerX(), centerY(), tx, ty, bulletSpeed, damage, true));
    }

    public void onPlayerDash(double dx, double dy) {
        if (state == State.IDLE || state == State.DEAD) return;
        playerDashDX = dx;
        playerDashDY = dy;
    }

    // Fires `count` bullets spread evenly across `spread` radians, centered on `centerAngle`
    private void fan(double centerAngle, double spread, int count, double bulletSpeed) {
        for (int i = 0; i < count; i++) {
            double a = count == 1 ? centerAngle : centerAngle - spread / 2 + spread * i / (count - 1);
            fire(a, bulletSpeed);
        }
    }

    // The shooter's three patterns, in a loop: aimed fan -> full ring -> wide fan
    private void shootPattern() {
        double aim = Math.atan2(gp.Player.centerY() - centerY(), gp.Player.centerX() - centerX());
        switch (volley % 3) {
            case 0: fan(aim, Math.toRadians(40), 3, 3.5); break;                     // 3 aimed bullets
            case 1:                                                                  // ring of 12, rotating
                for (int i = 0; i < 12; i++) fire(aim + Math.PI * 2 * i / 12 + volley * 0.2, 2.8);
                break;
            default: fan(aim, Math.toRadians(90), 7, 3.2); break;                    // wide fan of 7
        }
        volley++;
    }

    private int dirIndex() {
        switch (direction) {
            case "right": return 1;
            case "up":    return 2;
            case "left":  return 3;
            default:      return 0;
        }
    }

    // ===== AI =====

    public boolean isRemovable() { return state == State.DEAD && deathTimer <= 0; }

    public void update() {
        if (state == State.DEAD) { deathTimer--; return; }

        tickCombat();
        if (isDead()) {
            state = State.DEAD;
            deathTimer = 40;
            gp.progression.awardExperience(xpReward, gp);   // runs once: DEAD returns early above
            gp.progression.awardExperience(xpReward, gp);
            gp.quest.onEnemyKilled();
            return;
        }

        int startX = x, startY = y;

        if (kbTimer > 0) {                       // hit-stun: can't think while being pushed
            updateKnockback(gp);
        } else {
            think();
        }

        moving = (x != startX || y != startY) && kbTimer <= 0;
        animate();
    }

    private void think() {
        if (cooldownTimer > 0) cooldownTimer--;

        player p = gp.Player;
        int dx = p.centerX() - centerX(), dy = p.centerY() - centerY();
        double dist = Math.sqrt((double) dx * dx + (double) dy * dy);
        boolean playerAlive = gp.playerHP > 0;

        switch (state) {
            case IDLE:
                wander();
                if (playerAlive && dist <= detectRadius) state = State.CHASE;
                break;

            case CHASE:
                if (!playerAlive || dist > loseRadius) { state = State.IDLE; break; }
                face(dx, dy);
                if (ranged && dist < keepDistance) { state = State.RETREAT; break; }
                if (dist <= attackRange) {
                    if (cooldownTimer == 0) startAttack();   // otherwise hold position and wait
                } else {
                    moveToward(dx, dy, dist, 1.0);
                }
                break;

            case RETREAT:
                if (!playerAlive) { state = State.IDLE; break; }
                face(dx, dy);
                if (dist >= keepDistance + 48) { state = State.CHASE; break; }
                // Back away; if a wall is behind us, shoot instead of freezing
                if (!moveToward(-dx, -dy, dist, 1.0) && cooldownTimer == 0) startAttack();
                break;

            case ATTACK:
                if (--windupTimer <= 0) {
                    resolveAttack(dx, dy, dist);
                    cooldownTimer = attackCooldown;
                    state = State.CHASE;
                }
                break;

            default:
                break;
        }
    }

    private void wander() {
        if (--wanderTimer <= 0) {
            wanderTimer = 60 + (int) (Math.random() * 90);
            if (Math.random() < 0.5) { wanderX = wanderY = 0; }            // stand still
            else {
                double a = Math.random() * Math.PI * 2;
                wanderX = Math.cos(a); wanderY = Math.sin(a);
            }
        }
        if (wanderX != 0 || wanderY != 0) {
            face((int) (wanderX * 100), (int) (wanderY * 100));
            step(wanderX * moveSpeed * 0.4, wanderY * moveSpeed * 0.4);
        }
    }

    private void startAttack() {
        gp.soundEffects.play("Monster roar (Rpg).wav");
        state = State.ATTACK;
        windupTimer = windup;
        currentAnim = anims[2][dirIndex()];
        currentAnim.reset();
    }

    // The actual hit or shot, at the end of the wind-up
    private void resolveAttack(int dx, int dy, double dist) {
        player p = gp.Player;
        if (type.equals("shooter")) {
            shootPattern();
        } else if (ranged) {
            gp.enemyM.projectiles.add(new projectile(gp, centerX(), centerY(),
                    p.centerX(), p.centerY(), 5.0, damage));
        } else if (dist <= attackRange + 24) {            // a little slack so you can't dodge by a pixel
            p.takeDamage(damage, centerX(), centerY(), knockbackPower);
        }
    }

    // Moves toward a direction vector at full speed. Returns false if a wall stopped it.
    private boolean moveToward(int dx, int dy, double dist, double factor) {
        if (dist < 1) return true;
        return step(dx / dist * moveSpeed * factor, dy / dist * moveSpeed * factor);
    }

    // Movement with fractional speeds (a speed of 1.6 means some ticks move 1 px, some 2)
    private boolean step(double vx, double vy) {
        remX += vx;
        remY += vy;
        int mx = (int) remX, my = (int) remY;
        remX -= mx;
        remY -= my;
        return moveWithCollision(gp, mx, my);
    }

    private void face(int dx, int dy) {
        if (Math.abs(dx) > Math.abs(dy)) direction = dx > 0 ? "right" : "left";
        else direction = dy > 0 ? "down" : "up";
    }

    private void animate() {
        int d = dirIndex();
        Animation next;
        if (state == State.ATTACK) next = anims[2][d];
        else if (moving)           next = anims[1][d];
        else                       next = anims[0][d];

        if (next != currentAnim) {
            currentAnim = next;
            currentAnim.reset();
        }
        currentAnim.tick();
    }

    // ===== DRAW =====

    public void draw(Graphics2D g2) {
        int sx = x - gp.Player.camX, sy = y - gp.Player.camY;
        int size = gp.tileSize * 2;
        if (sx + size < 0 || sx > gp.screenWidth || sy + size < 0 || sy > gp.screenHeight) return;

        float alpha = 1f;
        if (state == State.DEAD) alpha = Math.max(0f, deathTimer / 40f);               // fade out
        else if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) alpha = 0.5f;   // flicker when hit

        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.drawImage(currentAnim.current(), sx, sy, size, size, null);
        g2.setComposite(old);

        // Health bar, only after taking damage
        if (state != State.DEAD && hp < maxHP) {
            int bw = 44, bx = sx + size / 2 - bw / 2, by = sy + 14;
            g2.setColor(new Color(40, 10, 10));
            g2.fillRect(bx - 1, by - 1, bw + 2, 7);
            g2.setColor(new Color(200, 30, 30));
            g2.fillRect(bx, by, (int) (bw * hp / (double) maxHP), 5);
        }
    }
}