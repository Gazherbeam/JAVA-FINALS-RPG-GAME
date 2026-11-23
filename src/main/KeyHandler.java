package main;

import java.awt.event.KeyEvent;

// Stores held player controls. GamePanel routes key bindings here only while playing.
public class KeyHandler {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean attackPressed, dashPressed, bigSwingPressed;

    public void keyPressed(int code) {
        if (code == KeyEvent.VK_UP) upPressed = true;
        if (code == KeyEvent.VK_DOWN) downPressed = true;
        if (code == KeyEvent.VK_LEFT) leftPressed = true;
        if (code == KeyEvent.VK_RIGHT) rightPressed = true;
        if (code == KeyEvent.VK_Z) attackPressed = true;
        if (code == KeyEvent.VK_X) dashPressed = true;
        if (code == KeyEvent.VK_C) bigSwingPressed = true;
    }

    public void keyReleased(int code) {
        if (code == KeyEvent.VK_UP) upPressed = false;
        if (code == KeyEvent.VK_DOWN) downPressed = false;
        if (code == KeyEvent.VK_LEFT) leftPressed = false;
        if (code == KeyEvent.VK_RIGHT) rightPressed = false;
        if (code == KeyEvent.VK_Z) attackPressed = false;
        if (code == KeyEvent.VK_X) dashPressed = false;
        if (code == KeyEvent.VK_C) bigSwingPressed = false;
    }
}
