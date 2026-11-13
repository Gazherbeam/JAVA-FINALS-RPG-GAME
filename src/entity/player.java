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
    Animation currentAnim;
    boolean moving = false;

    // Where the player is drawn on the SCREEN. Always the center, because the camera follows the player.
    // (x and y from the entity class are now the player's position in the WORLD.)
    public final int screenX;
    public final int screenY;

    public player(GamePanel gp, KeyHandler keyH) {
        this.gp = gp;
        this.KeyH = keyH;

        // Center of the screen, minus half of the drawn character size (96 px / 2 = one tileSize)
        screenX = gp.screenWidth / 2 - gp.tileSize;
        screenY = gp.screenHeight / 2 - gp.tileSize;

        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        // Starting spot in the WORLD, in pixels (tile column 10, row 10).
        // Your map must be at least this big.
        x = gp.tileSize * 10;
        y = gp.tileSize * 10;
        speed = 4;
        direction = "down";   // 'direction' comes from the entity class
    }

    private Animation makeAnim(SpriteSheet sheet, int row, int startCol, int count, long frameTimeMs) {
        BufferedImage[] frames = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            frames[i] = sheet.get(row, startCol + i);
        }
        return new Animation(frames, frameTimeMs);
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

            currentAnim = idleDown;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {
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
    }

    public void draw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Drawn at the fixed screen position (the center), NOT at x, y.
        g2.drawImage(currentAnim.current(), screenX, screenY, gp.tileSize * 2, gp.tileSize * 2, null);
    }
}