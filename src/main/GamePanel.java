package main;

import entity.player;
import tile.TileManager;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {

    // ===== SCREEN SETTINGS (unchanged) =====
    final int originalTileSize = 16;
    final int scale = 3;

    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 16;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;

    // ----- Game states -----
    public final int titleState = 0;
    public final int settingsState = 1;
    public final int creditsState = 2;
    public final int playState = 3;
    public final int battleState = 4;
    public final int inventoryState = 5;
    public int gameState = titleState;       // the game starts on the main menu

    // ===== FPS =====
    int FPS = 60;
    public int currentFPS = 0;               // measured FPS, shown if enabled in Settings

    // ===== PLAYER HP (read by the HUD bar and the battle screen) =====
    public int playerHP = 100;
    public int playerMaxHP = 100;

    // ===== SYSTEM =====
    public TileManager tileM = new TileManager(this);
    public UI ui = new UI(this);
    public Inventory inventory = new Inventory();
    public KeyHandler keyH = new KeyHandler();
    public player Player = new player(this, keyH);
    Thread gameThread;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);           // existing handler: player movement
        this.addKeyListener(new MenuKeys()); // new handler: menus and state changes
        this.setFocusable(true);

        // TEMPORARY starter items so you can see the menu working. Remove or replace later.
        inventory.add(new Item("Potion", "Restores a little health. Tastes awful.", true, null), 3);
        inventory.add(new Item("Bread", "A day-old loaf. Better than nothing.", true, null), 2);
        inventory.add(new Item("Rusty Key", "Opens an old door somewhere.", false, null), 1);
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        int drawCount = 0;
        long timer = 0;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            timer += (currentTime - lastTime);       // fixed: the timer now counts up
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
                drawCount++;
            }

            if (timer >= 1000000000L) {
                currentFPS = (int) drawCount;
                drawCount = 0;
                timer = 0;
            }

            // stops the loop from using 100% of a CPU core
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public void update() {
        // The world only updates while playing. In menus, everything is paused.
        if (gameState == playState) {
            Player.update();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // the map is drawn under the inventory so the menu appears over the game
        if (gameState == playState || gameState == inventoryState) {
            tileM.draw(g2);
            Player.draw(g2);
        }

        if (gameState == playState) {
            ui.drawPlayOverlay(g2);
        } else {
            ui.draw(g2);
        }

        g2.dispose();
    }

    // ===== MENU INPUT =====
    // KeyHandler is left alone. This second listener only deals with menus and state changes.

    private class MenuKeys extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            boolean up      = k == KeyEvent.VK_W || k == KeyEvent.VK_UP;
            boolean down    = k == KeyEvent.VK_S || k == KeyEvent.VK_DOWN;
            boolean left    = k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT;
            boolean right   = k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT;
            boolean confirm = k == KeyEvent.VK_ENTER || k == KeyEvent.VK_J;
            boolean back    = k == KeyEvent.VK_ESCAPE;

            if (gameState == titleState)         handleTitle(up, down, confirm);
            else if (gameState == settingsState) handleSettings(up, down, left, right, confirm, back);
            else if (gameState == creditsState)  handleCredits(confirm, back);
            else if (gameState == playState)     handlePlay(k, back);
            else if (gameState == battleState)   handleBattle(up, down, left, right, confirm, back);
            else if (gameState == inventoryState) handleInventory(k, up, down, left, right, confirm, back);
        }
    }

    private void handleTitle(boolean up, boolean down, boolean confirm) {
        int n = ui.titleOptions.length;
        if (up)   ui.menuIndex = (ui.menuIndex + n - 1) % n;
        if (down) ui.menuIndex = (ui.menuIndex + 1) % n;

        if (confirm) {
            switch (ui.menuIndex) {
                case 0: gameState = playState; break;
                case 1: gameState = settingsState; ui.menuIndex = 0; break;
                case 2: gameState = creditsState; break;
                case 3: System.exit(0); break;
            }
        }
    }

    private void handleSettings(boolean up, boolean down, boolean left, boolean right,
                                boolean confirm, boolean back) {
        int n = ui.settingsOptionCount;
        if (up)   ui.menuIndex = (ui.menuIndex + n - 1) % n;
        if (down) ui.menuIndex = (ui.menuIndex + 1) % n;

        if (ui.menuIndex == 0 && (left || right || confirm)) {
            ui.showFPS = !ui.showFPS;
        }
        if (ui.menuIndex == 1) {
            if (left)  ui.volume = Math.max(0, ui.volume - 1);
            if (right) ui.volume = Math.min(10, ui.volume + 1);
        }
        if ((ui.menuIndex == 2 && confirm) || back) {
            gameState = titleState;
            ui.menuIndex = 1;
        }
    }

    private void handleCredits(boolean confirm, boolean back) {
        if (confirm || back) {
            gameState = titleState;
            ui.menuIndex = 2;
        }
    }

    private void handlePlay(int keyCode, boolean back) {
        if (back) {
            gameState = titleState;
            ui.menuIndex = 0;
        }
        // TEMPORARY: H = lose 10 HP, G = gain 10 HP, so you can watch the bar move
        if (keyCode == KeyEvent.VK_H) playerHP = Math.max(0, playerHP - 10);
        if (keyCode == KeyEvent.VK_G) playerHP = Math.min(playerMaxHP, playerHP + 10);

        // I opens the inventory
        if (keyCode == KeyEvent.VK_I) {
            ui.invMessage = "";
            gameState = inventoryState;
        }
        // TEMPORARY: press B to open the battle screen so you can see it
        if (keyCode == KeyEvent.VK_B) {
            ui.resetBattleDummy();
            ui.menuIndex = 0;
            gameState = battleState;
        }
    }

    private void handleInventory(int keyCode, boolean up, boolean down, boolean left, boolean right,
                                 boolean confirm, boolean back) {
        int cols = Inventory.COLS;

        // move the cursor around the grid
        if (up    && ui.invIndex >= cols)                  ui.invIndex -= cols;
        if (down  && ui.invIndex < Inventory.SIZE - cols)  ui.invIndex += cols;
        if (left  && ui.invIndex % cols > 0)               ui.invIndex--;
        if (right && ui.invIndex % cols < cols - 1)        ui.invIndex++;
        if (up || down || left || right) ui.invMessage = "";

        // Esc or I closes it
        if (back || keyCode == KeyEvent.VK_I) {
            ui.invMessage = "";
            gameState = playState;
            return;
        }

        // TEMPORARY item use: consumables are used up, nothing else happens yet.
        // This is where real item effects (heal, equip, etc.) will go.
        if (confirm) {
            Item item = inventory.getItem(ui.invIndex);
            if (item == null) {
                ui.invMessage = "That slot is empty.";
            } else if (item.consumable) {
                inventory.remove(ui.invIndex, 1);
                ui.invMessage = "Used " + item.name + ".";
            } else {
                ui.invMessage = "Can't use that right now.";
            }
        }
    }

    private void handleBattle(boolean up, boolean down, boolean left, boolean right,
                              boolean confirm, boolean back) {
        // 2x2 grid: 0 1 / 2 3
        if (up    && ui.menuIndex >= 2)     ui.menuIndex -= 2;
        if (down  && ui.menuIndex < 2)      ui.menuIndex += 2;
        if (left  && ui.menuIndex % 2 == 1) ui.menuIndex--;
        if (right && ui.menuIndex % 2 == 0) ui.menuIndex++;

        if (back) {
            gameState = playState;
        }
        // TEMPORARY dummy behaviour, replace with your real battle logic
        if (confirm) {
            if (ui.menuIndex == 0) {
                ui.enemyHP = Math.max(0, ui.enemyHP - 10);
                ui.battleMessage = ui.enemyHP == 0 ? "Enemy fainted!" : "Enemy took 10 damage!";
            } else if (ui.menuIndex == 3) {
                gameState = playState;
            } else {
                ui.battleMessage = "Not added yet.";
            }
        }
    }
}