package main;

import entity.EnemyManager;
import entity.player;
import tile.TileManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JPanel;


public class GamePanel extends JPanel implements Runnable {

    // ===== SCREEN SETTING
    // S (unchanged) =====
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

    private int manaRegenTicks = 0;

    // ===== PROGRESSION =====
    public final PlayerProgression progression = new PlayerProgression();

    // ===== MUSIC TRACKS (the files live in res/Soundtrack/) =====
    private static final String MENU_MUSIC = "MainMenu.wav";
    private static final String DEFAULT_MUSIC = "Battle01.wav";   // temporary default theme

    // ===== SYSTEM =====
    public TileManager tileM = new TileManager(this);
    public UI ui = new UI(this);
    public final Music music = new Music();
    public final SoundEffect soundEffects = new SoundEffect();
    // Compatibility alias for code that uses the singular field name.
    public final SoundEffect soundEffect = soundEffects;
    public Inventory inventory = new Inventory();
    public final Quest quest = new Quest(this);
    public final SpellManager spells = new SpellManager(this);
    public final EquipmentManager equipment = new EquipmentManager(this);
    public final EquipmentMenu equipmentMenu = new EquipmentMenu(this);
    public KeyHandler keyH = new KeyHandler();
    public player Player = new player(this, keyH);
    public EnemyManager enemyM = new EnemyManager(this);
    public final Dialogue dialogue = new Dialogue(this);
    Thread gameThread;

    public GamePanel() {
        Player.slashFx.setScale(4); // COPILOT EDIT
        music.setEnabled(ui.sfxEnabled);
        music.setVolume(ui.volume / 10.0f);
        soundEffects.setEnabled(ui.sfxEnabled);
        soundEffects.setVolume(ui.volume / 10.0f);
        // The game opens on the title screen, so it starts with the main menu music.
        music.play(MENU_MUSIC);
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.setFocusable(true);
        installKeyboardBindings();

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

            // Update spell cooldowns.
            spells.update();

            // Check map exits after movement, so the player can walk into the transition area.
            if (checkMapTransitions()) return;
            enemyM.update();
        }
    }



    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        // the map is drawn under the inventory so the menu appears over the game
        if (gameState == playState || gameState == inventoryState) {
            tileM.draw(g2);
            enemyM.draw(g2);
            drawTestSign(g2);
            Player.draw(g2);
        }

        if (gameState == playState) {
            ui.drawPlayOverlay(g2);
            dialogue.draw(g2);
        } else {
            ui.draw(g2);
        }

        g2.dispose();
    }


    // Temporary dialogue-test sign in RuinedTown, two tiles east of the player start.
    private void drawTestSign(Graphics2D g2) {
        if (!"/maps/RuinedTown.txt".equals(tileM.currentMapPath)) return;

        int worldX = 12 * tileSize;
        int worldY = 10 * tileSize;
        int screenX = worldX - Player.camX;
        int screenY = worldY - Player.camY;

        g2.setColor(new Color(75, 45, 25));
        g2.fillRect(screenX + 21, screenY + 30, 6, 28);
        g2.setColor(new Color(115, 75, 38));
        g2.fillRect(screenX + 3, screenY + 3, 42, 31);
        g2.setColor(new Color(210, 175, 105));
        g2.drawRect(screenX + 3, screenY + 3, 42, 31);
        g2.setColor(Color.WHITE);
        g2.setFont(ui.fontTiny);
        int readW = g2.getFontMetrics().stringWidth("READ");
        g2.drawString("READ", screenX + (tileSize - readW) / 2, screenY + 23);

        int dx = Player.centerX() - (worldX + tileSize / 2);
        int dy = Player.centerY() - (worldY + tileSize / 2);
        if (dx * dx + dy * dy <= (tileSize * 2) * (tileSize * 2)) {
            g2.setFont(ui.fontSmall);
            String prompt = "E: Read sign";
            int promptW = g2.getFontMetrics().stringWidth(prompt);
            g2.drawString(prompt, screenX + (tileSize - promptW) / 2, screenY - 7);
        }
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
        enemyM.magicProjectiles.clear();
    }

    // ===== KEYBOARD INPUT =====
    // Window-level bindings receive input even when the panel itself is not the
    // focused component. Arrow keys navigate menus and move the player in playState.
    private void installKeyboardBindings() {
        InputMap input = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = getActionMap();

        bindKey(input, actions, KeyEvent.VK_UP, "up", true);
        bindKey(input, actions, KeyEvent.VK_DOWN, "down", true);
        bindKey(input, actions, KeyEvent.VK_LEFT, "left", true);
        bindKey(input, actions, KeyEvent.VK_RIGHT, "right", true);
        bindKey(input, actions, KeyEvent.VK_Z, "attack", true);
        bindKey(input, actions, KeyEvent.VK_X, "spell1", true);
        bindKey(input, actions, KeyEvent.VK_C, "spell2", true);
        bindKey(input, actions, KeyEvent.VK_V, "spell3", true);

        bindMenuKey(input, actions, KeyEvent.VK_ENTER, "confirm");
        bindMenuKey(input, actions, KeyEvent.VK_ESCAPE, "back");
        bindMenuKey(input, actions, KeyEvent.VK_I, "inventory");
        bindMenuKey(input, actions, KeyEvent.VK_H, "testDamage");
        bindMenuKey(input, actions, KeyEvent.VK_G, "testHeal");
        bindMenuKey(input, actions, KeyEvent.VK_J, "confirmJ");
        bindMenuKey(input, actions, KeyEvent.VK_E, "interact");
    }

    private void bindKey(InputMap input, ActionMap actions, int keyCode,
                         String name, boolean routeToMenus) {
        String pressed = name + "Pressed";
        String released = name + "Released";
        input.put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0, false), pressed);
        input.put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0, true), released);

        actions.put(pressed, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (gameState == playState) {
                    keyH.keyPressed(keyCode);
                } else if (routeToMenus) {
                    handleMenuKey(keyCode);
                }
            }
        });
        actions.put(released, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                keyH.keyReleased(keyCode);
            }
        });
    }

    private void bindMenuKey(InputMap input, ActionMap actions, int keyCode, String name) {
        String actionName = "menu_" + name;
        input.put(javax.swing.KeyStroke.getKeyStroke(keyCode, 0, false), actionName);
        actions.put(actionName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                handleMenuKey(keyCode);
            }
        });
    }

    private void handleMenuKey(int keyCode) {
        boolean up = keyCode == KeyEvent.VK_UP;
        boolean down = keyCode == KeyEvent.VK_DOWN;
        boolean left = keyCode == KeyEvent.VK_LEFT;
        boolean right = keyCode == KeyEvent.VK_RIGHT;
        boolean confirm = keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_J;
        boolean back = keyCode == KeyEvent.VK_ESCAPE;

        if (gameState == titleState) {
            handleTitle(up, down, confirm);
        } else if (gameState == settingsState) {
            handleSettings(up, down, left, right, confirm, back);
        } else if (gameState == creditsState) {
            handleCredits(confirm, back);
        } else if (gameState == playState) {
            handlePlay(keyCode, back);
        } else if (gameState == battleState) {
            handleBattle(up, down, left, right, confirm, back);
        } else if (gameState == inventoryState) {
            handleInventory(keyCode, up, down, left, right, confirm, back);
        }
    }

    private void handleTitle(boolean up, boolean down, boolean confirm) {
        int n = ui.titleOptions.length;
        if (up)   ui.menuIndex = (ui.menuIndex + n - 1) % n;
        if (down) ui.menuIndex = (ui.menuIndex + 1) % n;

        if (confirm) {
            switch (ui.menuIndex) {
                case 0: gameState = playState; music.play(DEFAULT_MUSIC); break;
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
            soundEffects.setEnabled(ui.sfxEnabled);
        }
        // Shared sound volume. The same value can be used by future sound effects.
        if (ui.menuIndex == 2) {
            if (left)  ui.volume = Math.max(0, ui.volume - 1);
            if (right) ui.volume = Math.min(10, ui.volume + 1);
            if (left || right) {
                music.setVolume(ui.volume / 10.0f);
                soundEffects.setVolume(ui.volume / 10.0f);
            }
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
        if (keyCode == KeyEvent.VK_E) {
            if (dialogue.isActive()) {
                dialogue.advance();
            } else if ("/maps/RuinedTown.txt".equals(tileM.currentMapPath)) {
                int signX = 12 * tileSize + tileSize / 2;
                int signY = 10 * tileSize + tileSize / 2;
                int dx = Player.centerX() - signX;
                int dy = Player.centerY() - signY;
                if (dx * dx + dy * dy <= (tileSize * 2) * (tileSize * 2)) {
                    dialogue.start(Conversation.testSign());
                }
            }
            return;
        }
        if (dialogue.isActive()) return;
        if (back) {
            gameState = titleState;
            ui.menuIndex = 0;
            music.play(MENU_MUSIC);
        }
        // TEMPORARY: H = lose 10 HP, G = gain 10 HP, so you can watch the bar move
        if (keyCode == KeyEvent.VK_H) playerHP = Math.max(0, playerHP - 10);
        if (keyCode == KeyEvent.VK_G) playerHP = Math.min(playerMaxHP, playerHP + 10);

        // I opens the inventory
        if (keyCode == KeyEvent.VK_I) {
            ui.invMessage = "";
            ui.inventoryDetails = false;
            ui.categoryDetails = false;
            ui.menuCategory = 0;
            gameState = inventoryState;
        }
        // TEMPORARY: press B to open the battle screen so you can see it
    }


    private void handleInventory(int keyCode, boolean up, boolean down, boolean left, boolean right,
                                 boolean confirm, boolean back) {
        if (ui.categoryDetails) {
            // The Equipment screen handles its own keys; it tells us when to close.
            if (ui.menuCategory == 2) {
                if (equipmentMenu.handleInput(up, down, confirm, back || keyCode == KeyEvent.VK_I)) {
                    ui.categoryDetails = false;
                }
                return;
            }
            if (back || keyCode == KeyEvent.VK_I) {
                ui.categoryDetails = false;
            }
            return;
        }

        if (!ui.inventoryDetails) {
            if (up) ui.menuCategory = (ui.menuCategory + 4) % 5;
            if (down) ui.menuCategory = (ui.menuCategory + 1) % 5;
            if (confirm) {
                if (ui.menuCategory == 0) {
                    ui.inventoryDetails = true;
                    ui.invIndex = 0;
                    ui.invMessage = "";
                } else {
                    ui.categoryDetails = true;
                }
            }
            if (back || keyCode == KeyEvent.VK_I) {
                gameState = playState;
                ui.invMessage = "";
            }
            return;
        }

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
            ui.inventoryDetails = false;
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