
package main;

import java.awt.event.KeyEvent;

// Stores held player controls.
// GamePanel routes key bindings here only while playing.
public class KeyHandler {

    // Movement
    public boolean upPressed;
    public boolean downPressed;
    public boolean leftPressed;
    public boolean rightPressed;

    // Combat
    public boolean attackPressed;

    // Spell quick-cast keys
    // X = Spell Slot 1
    // C = Spell Slot 2
    // V = Spell Slot 3
    public boolean spell1Pressed;
    public boolean spell2Pressed;
    public boolean spell3Pressed;

    // Legacy controls retained temporarily for compatibility.
    // These will be handled by the updated player class.
    public boolean dashPressed;
    public boolean bigSwingPressed;

    public void keyPressed(int code) {

        // Movement
        if (code == KeyEvent.VK_UP) upPressed = true;
        if (code == KeyEvent.VK_DOWN) downPressed = true;
        if (code == KeyEvent.VK_LEFT) leftPressed = true;
        if (code == KeyEvent.VK_RIGHT) rightPressed = true;

        // Basic attack
        if (code == KeyEvent.VK_Z) attackPressed = true;

        // Spell quick-casting
        if (code == KeyEvent.VK_X) spell1Pressed = true;
        if (code == KeyEvent.VK_C) spell2Pressed = true;
        if (code == KeyEvent.VK_V) spell3Pressed = true;
    }

    public void keyReleased(int code) {

        // Movement
        if (code == KeyEvent.VK_UP) upPressed = false;
        if (code == KeyEvent.VK_DOWN) downPressed = false;
        if (code == KeyEvent.VK_LEFT) leftPressed = false;
        if (code == KeyEvent.VK_RIGHT) rightPressed = false;

        // Basic attack
        if (code == KeyEvent.VK_Z) attackPressed = false;

        // Spell quick-casting
        if (code == KeyEvent.VK_X) spell1Pressed = false;
        if (code == KeyEvent.VK_C) spell2Pressed = false;
        if (code == KeyEvent.VK_V) spell3Pressed = false;
    }
}
