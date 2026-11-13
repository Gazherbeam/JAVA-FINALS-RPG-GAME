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

    public final int screenX;
    public final int screenY;

    public player(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.KeyH = keyH;

        screenX = gp.screenWidth / 2 - gp.tileSize;
        screenY = gp.screenHeight / 2 - gp.tileSize;

        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        x = gp.tileSize * 10;
        y = gp.tileSize * 10;
        speed = 4;
        direction = "down";
    }

    // Looping animation (idle / walk)
    private Animation makeAnim(SpriteSheet sheet, int row, int startCol, int count, long frameTimeMs) {
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            frames[i] = sheet.get(row, startCol + i);
        }
        return new Animation(frames, frameTimeMs);
    }

    // Plays once (attack)
    private Animation makeAttackAnim(SpriteSheet sheet, int row, int startCol, int count, long frameTimeMs) {
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            frames[i] = sheet.get(row, startCol + i);
        }
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

            // Attack frames: (sheet, row, startColumn, frameCount, msPerFrame). Columns are a guess.
            attackDown  = makeAttackAnim(sheet, 0, 4, 3, 120);
            attackRight = makeAttackAnim(sheet, 2, 4, 3, 120);
            attackUp    = makeAttackAnim(sheet, 4, 4, 3, 120);
            attackLeft  = makeAttackAnim(sheet, 6, 4, 3, 120);

            currentAnim = idleDown;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {

        // ----- ATTACK -----
        if (KeyH.attackPressed && !attacking && !attackLatch) {
            attacking = true;
            attackLatch = true;
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
            if (currentAnim.isFinished()) {
                attacking = false;
            }
            return;   // no walking while attacking
        }

        // ----- MOVEMENT -----
        moving = false;

        if (KeyH.upPressed) {
            y -= speed;
            direction = "up";
            moving = true;
        } else if (KeyH.downPressed) {
            y += speed;
            direction = "down";
            moving = true;
        } else if (KeyH.leftPressed) {
            x -= speed;
            direction = "left";
            moving = true;
        } else if (KeyH.rightPressed) {
            x += speed;
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

        currentAnim.tick();   // advance idle / walk frames
    }

    public void draw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        g2.drawImage(currentAnim.current(), screenX, screenY, gp.tileSize * 2, gp.tileSize * 2, null);
    }
}