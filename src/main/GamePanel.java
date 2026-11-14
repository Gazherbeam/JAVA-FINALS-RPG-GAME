package main;

import entity.player;
import tile.TileManager;

import java.awt.*;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {

    final int originalTileSize = 16;
    final int scale = 3;

    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 16;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    // ----- Game states -----
    public final int titleState = 0;
    public final int playState = 1;
    public final int settingsState = 2;
    public final int creditsState = 3;
    public int gameState = titleState;       // the game starts on the main menu

    int FPS = 60;
    int currentFPS = 0;                      // measured FPS, shown if enabled in Settings

    public KeyHandler keyH = new KeyHandler(this);
    public UI ui = new UI(this);
    Thread gameThread;
    public TileManager tileM = new TileManager(this);
    public player Player = new player(this, keyH);

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        long drawCount = 0;
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

            if (timer >= 1000000000) {
                currentFPS = (int) drawCount;
                drawCount = 0;
                timer = 0;
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

        if (gameState == playState) {
            tileM.draw(g2);
            Player.draw(g2);

            if (ui.showFPS) {
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 18));
                g2.drawString("FPS: " + currentFPS, 10, 24);
            }
        } else {
            ui.draw(g2);   // title, settings or credits
        }

        g2.dispose();
    }
}