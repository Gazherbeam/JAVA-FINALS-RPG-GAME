package main;

import entity.EnemyManager;
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
    public int playerMP = 30;
    public int playerMaxMP = 30;

    // ===== PROGRESSION =====
    public final PlayerProgression progression = new PlayerProgression();

    // ===== SYSTEM =====
    public TileManager tileM = new TileManager(this);
    public UI ui = new UI(this);
    public final Music music = new Music();
    public Inventory inventory = new Inventory();
    public KeyHandler keyH = new KeyHandler();
    public player Player = new player(this, keyH);
    public EnemyManager enemyM = new EnemyManager(this);
    Thread gameThread;

    public GamePanel() {
        Player.slashFx.setScale(4); // COPILOT EDIT
        music.setEnabled(ui.sfxEnabled);
        music.setVolume(ui.volume / 10.0f);
        // Keep the existing area soundtrack playing.
        music.play("Battle01.wav");
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);           // existing handler: player movement
        this.addKeyListener(new MenuKeys()); // new handler: menus and state changes
        this.setFocusable(true);

        // Starter items exercise the inventory's category, stacking, and use-effect support.
        inventory.add(new Item("Potion", "Restores 30 HP.", true, null,
                Item.Category.CONSUMABLE, 99, 30), 3);
        inventory.add(new Item("Bread", "A day-old loaf. Restores 10 HP.", true, null,
                Item.Category.CONSUMABLE, 99, 10), 2);
        inventory.add(new Item("Rusty Key", "Opens an old door somewhere.", false, null,
                Item.Category.QUEST, 1, 0), 1);
        progression.applyTo(this);
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
            // Check map exits after movement, so the player can walk into the transition area.
            if (checkMapTransitions()) return;
            enemyM.update();
        }
    }



    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // the map is drawn under the inventory so the menu appears over the game
        if (gameState == playState || gameState == inventoryState) {
            tileM.draw(g2);
            enemyM.draw(g2);
            Player.draw(g2);
        }

        if (gameState == playState) {
            ui.drawPlayOverlay(g2);
        } else {
            ui.draw(g2);
        }

        g2.dispose();
    }


    // ===== MAP TRANSITIONS =====
    // The exits are on the left-center edge of Landfill and the right-center
    // edge of map01. Spawn points are one tile inside each exit so the player
    // does not immediately trigger the return trip upon arriving.
    private boolean checkMapTransitions() {
        int playerCenterX = Player.centerX();
        int playerCenterY = Player.centerY();
        int tile = tileSize;
        boolean inCenterLane = playerCenterY >= tile * 14
                && playerCenterY <= tile * 17;

        if ("/maps/Landfill.txt".equals(tileM.currentMapPath)) {
            boolean atLandfillLeftExit = playerCenterX <= tile * 2 && inCenterLane;

            if (atLandfillLeftExit) {
                tileM.loadMap("/maps/map01.txt");

                // Arrive one tile inside map01's right edge.
                Player.placeOnTile(this, tileM.worldCols - 3, 14);
                Player.updateCamera();
                clearAreaEnemies();
                return true;
            }
        } else if ("/maps/map01.txt".equals(tileM.currentMapPath)) {
            boolean atMap01RightExit = playerCenterX >= tile * (tileM.worldCols - 2)
                    && inCenterLane;

            if (atMap01RightExit) {
                tileM.loadMap("/maps/Landfill.txt");

                // Arrive one tile inside Landfill's left edge.
                Player.placeOnTile(this, 2, 15);
                Player.updateCamera();
                clearAreaEnemies();
                return true;
            }
        }

        return false;
    }

    // Enemies and projectiles belong to the area being left.
    private void clearAreaEnemies() {
        enemyM.enemies.clear();
        enemyM.projectiles.clear();
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

        // Show FPS toggle.
        if (ui.menuIndex == 0 && (left || right || confirm)) {
            ui.showFPS = !ui.showFPS;
        }
        // SFX toggle also mutes/unmutes background music, as requested.
        if (ui.menuIndex == 1 && (left || right || confirm)) {
            ui.sfxEnabled = !ui.sfxEnabled;
            music.setEnabled(ui.sfxEnabled);
        }
        // Shared sound volume. The same value can be used by future sound effects.
        if (ui.menuIndex == 2) {
            if (left)  ui.volume = Math.max(0, ui.volume - 1);
            if (right) ui.volume = Math.min(10, ui.volume + 1);
            if (left || right) music.setVolume(ui.volume / 10.0f);
        }
        if ((ui.menuIndex == 3 && confirm) || back) {
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
                if (item.healAmount > 0) {
                    if (playerHP >= playerMaxHP) {
                        ui.invMessage = "Your HP is already full.";
                        return;
                    }
                    int restored = Math.min(item.healAmount, playerMaxHP - playerHP);
                    playerHP += restored;
                    inventory.remove(ui.invIndex, 1);
                    ui.invMessage = "Restored " + restored + " HP.";
                } else {
                    inventory.remove(ui.invIndex, 1);
                    ui.invMessage = "Used " + item.name + ".";
                }
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


            } else if (ui.menuIndex == 3) {
                gameState = playState;
            } else {

            }
        }
    }
}