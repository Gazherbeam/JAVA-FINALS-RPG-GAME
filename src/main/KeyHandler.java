package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    GamePanel gp;
    public boolean upPressed, downPressed, leftPressed, rightPressed, attackPressed;

    public KeyHandler(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (gp.gameState == gp.titleState)         titleInput(code);
        else if (gp.gameState == gp.settingsState) settingsInput(code);
        else if (gp.gameState == gp.creditsState)  creditsInput(code);
        else if (gp.gameState == gp.playState)     playInput(code);
    }

    // ----- Main menu -----
    private void titleInput(int code) {
        int count = gp.ui.titleOptions.length;

        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            gp.ui.menuIndex = (gp.ui.menuIndex - 1 + count) % count;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            gp.ui.menuIndex = (gp.ui.menuIndex + 1) % count;
        }
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_J) {
            switch (gp.ui.menuIndex) {
                case 0: gp.gameState = gp.playState; break;                  // Start Game
                case 1: gp.gameState = gp.settingsState; gp.ui.menuIndex = 0; break;
                case 2: gp.gameState = gp.creditsState;  gp.ui.menuIndex = 0; break;
                case 3: System.exit(0); break;                               // Quit
            }
        }
    }

    // ----- Settings -----
    private void settingsInput(int code) {
        int count = gp.ui.settingsOptionCount;

        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            gp.ui.menuIndex = (gp.ui.menuIndex - 1 + count) % count;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            gp.ui.menuIndex = (gp.ui.menuIndex + 1) % count;
        }

        // Left / right changes the highlighted setting
        boolean left  = code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT;
        boolean right = code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT;
        if (gp.ui.menuIndex == 1) {                                  // Volume
            if (left  && gp.ui.volume > 0)  gp.ui.volume--;
            if (right && gp.ui.volume < 10) gp.ui.volume++;
        }

        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_J) {
            if (gp.ui.menuIndex == 0) gp.ui.showFPS = !gp.ui.showFPS; // toggle
            if (gp.ui.menuIndex == 2) backToTitle();                  // Back
        }
        if (code == KeyEvent.VK_ESCAPE) backToTitle();
    }

    // ----- Credits -----
    private void creditsInput(int code) {
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_J || code == KeyEvent.VK_ESCAPE) {
            backToTitle();
        }
    }

    // ----- In the game -----
    private void playInput(int code) {
        if (code == KeyEvent.VK_W) upPressed = true;
        if (code == KeyEvent.VK_S) downPressed = true;
        if (code == KeyEvent.VK_A) leftPressed = true;
        if (code == KeyEvent.VK_D) rightPressed = true;
        if (code == KeyEvent.VK_J) attackPressed = true;

        if (code == KeyEvent.VK_ESCAPE) {        // Esc returns to the main menu
            upPressed = downPressed = leftPressed = rightPressed = attackPressed = false;
            backToTitle();
        }
    }

    private void backToTitle() {
        gp.gameState = gp.titleState;
        gp.ui.menuIndex = 0;
    }

    // Releases are always processed, so keys never get "stuck" after a state change
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) upPressed = false;
        if (code == KeyEvent.VK_S) downPressed = false;
        if (code == KeyEvent.VK_A) leftPressed = false;
        if (code == KeyEvent.VK_D) rightPressed = false;
        if (code == KeyEvent.VK_J) attackPressed = false;
    }
}