package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UI {

    GamePanel gp;
    Graphics2D g2;

    // ===== WHERE THE ASSETS ARE (inside res). Change this if you put the folder elsewhere. =====
    static final String BASE = "/pixel-rpg-ui-free-sample/";

    // The art is drawn at 1x (tiny), so everything is zoomed by this factor.
    static final int S = 3;

    public int menuIndex = 0;
    public final String[] titleOptions = {"Start Game", "Settings", "Credits", "Quit"};
    public final int settingsOptionCount = 3;   // Show FPS, Volume, Back
    public boolean showFPS = false;
    public int volume = 5;                      // 0-10, not connected to sound yet

    // ----- Battle placeholder data (replace with real battle data later) -----
    public final String[] battleOptions = {"Fight", "Item", "Swap", "Run"};
    public String battleEnemyName = "Enemy";
    public String battlePlayerName = "Hero";
    public int enemyHP = 100, enemyMaxHP = 100;
    public String battleMessage = "What will you do?";

    // ----- Inventory menu state (changed by GamePanel) -----
    public int invIndex = 0;          // which slot the cursor is on
    public String invMessage = "";    // short feedback line ("Used Potion.")
    private final Map<String, BufferedImage> iconCache = new HashMap<>();

    // ----- Loaded images -----
    BufferedImage btnNormal, btnFocus;
    BufferedImage panel, ribbon;
    BufferedImage slotNormal, slotSelected;
    BufferedImage toggleOn, toggleOff;

    // ----- Fonts -----
    Font fontBig, fontMid, fontSmall;

    // ----- Colors -----
    final Color background = new Color(47, 27, 16);     // dark brown
    final Color inkDark    = new Color(59, 36, 18);     // text on the cream panel
    final Color inkLight   = new Color(250, 240, 220);  // text on buttons and ribbon
    final Color hintColor  = new Color(190, 160, 120);

    public UI(GamePanel gp) {
        this.gp = gp;

        btnNormal    = load("ui/wood/1x/controls/btn_wide_normal.png");
        btnFocus     = load("ui/wood/1x/controls/btn_wide_focus.png");
        panel        = load("ui/wood/1x/panels/panel_window.png");
        ribbon       = load("ui/wood/1x/panels/ribbon_long.png");
        slotNormal   = load("ui/wood/1x/misc/slot_normal.png");
        slotSelected = load("ui/wood/1x/misc/slot_selected.png");
        toggleOn     = load("ui/wood/1x/controls/toggle_on_normal.png");
        toggleOff    = load("ui/wood/1x/controls/toggle_off_normal.png");
        barFrame     = load("ui/wood/1x/bars/bar_frame.png");
        barTrough    = load("ui/wood/1x/bars/bar_trough.png");
        barFill      = load("ui/wood/1x/bars/bar_fill_gold.png");
        barFillRed   = load("ui/wood/1x/bars/bar_fill_red.png");


        bgImage = loadAbsolute(BG_PATH);

        loadFont("fonts/PressStart2P-Regular.ttf");
    }

    // ===== LOADING =====

        try (InputStream in = getClass().getResourceAsStream(path)) {
            return null;
        }
            return ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    // Item icons live in res/items/. Each one is loaded once, then reused.
    private BufferedImage getIcon(String file) {
        if (file == null) return null;
        if (!iconCache.containsKey(file)) {
            iconCache.put(file, loadResource("/items/" + file));
        }
        return iconCache.get(file);
    }

    private void loadFont(String file) {
        try (InputStream in = getClass().getResourceAsStream(BASE + file)) {
            Font base = Font.createFont(Font.TRUETYPE_FONT, in);
            fontBig   = base.deriveFont(Font.PLAIN, 24f);
            fontMid   = base.deriveFont(Font.PLAIN, 18f);
            fontSmall = base.deriveFont(Font.PLAIN, 10f);
        } catch (Exception e) {
            System.out.println("UI: could not load font " + BASE + file + ", using Arial");
            fontBig   = new Font("Arial", Font.BOLD, 28);
            fontMid   = new Font("Arial", Font.BOLD, 22);
            fontSmall = new Font("Arial", Font.PLAIN, 14);
        }
    }


    // ===== MAIN ENTRIES =====

    // Called for every state except playState.
    public void draw(Graphics2D g2) {
        this.g2 = g2;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // keeps pixel art sharp
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);               // crisp pixel font


        if (gp.gameState == gp.titleState)     drawTitle();
        if (gp.gameState == gp.settingsState)  drawSettings();
        if (gp.gameState == gp.creditsState)   drawCredits();
        if (gp.gameState == gp.battleState)    drawBattle();
        if (gp.gameState == gp.inventoryState) drawInventory();
    }

    // Called on top of the map while playing.
    public void drawPlayOverlay(Graphics2D g2) {
        this.g2 = g2;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // HP bar, top left. Bar only, no number.
        drawBar(12, 12, gp.playerMaxHP > 0 ? gp.playerHP / (double) gp.playerMaxHP : 0, barFillRed);

        if (showFPS) {
            g2.setFont(fontSmall);
            g2.setColor(Color.WHITE);
            g2.drawString("FPS: " + gp.currentFPS, 12, 85);
        }

        g2.setFont(fontSmall);
        g2.setColor(Color.WHITE);
        g2.drawString("I: Inventory", 10, gp.screenHeight - 12);
    }

    // ===== SCREENS =====

    private void drawTitle() {
        drawRibbon("2D ADVENTURE", 90);

        int panelW = 450, panelH = 470;
        int panelX = (gp.screenWidth - panelW) / 2, panelY = 190;
        nineSlice(panel, 11, 11, 11, 11, panelX, panelY, panelW, panelH);

        int btnW = 330, btnH = 78;
        int btnX = (gp.screenWidth - btnW) / 2;
        for (int i = 0; i < titleOptions.length; i++) {
            drawButton(titleOptions[i], btnX, panelY + 45 + i * 100, btnW, btnH, i == menuIndex);
        }

        drawHint("W/S: move     Enter or J: select");
    }

    private void drawSettings() {
        drawRibbon("SETTINGS", 100);

        int panelW = 600, panelH = 420;
        int panelX = (gp.screenWidth - panelW) / 2, panelY = 190;
        nineSlice(panel, 11, 11, 11, 11, panelX, panelY, panelW, panelH);

        int rowX = panelX + 45, rowW = panelW - 90, rowH = 84;

        // Row 0: Show FPS (toggle switch)
        int row0Y = panelY + 50;
        drawRow(rowX, row0Y, rowW, rowH, menuIndex == 0);
        drawLabel("Show FPS", rowX + 30, row0Y, rowH);
        BufferedImage t = showFPS ? toggleOn : toggleOff;
        if (t != null) {
            int tw = t.getWidth() * S, th = t.getHeight() * S;
            g2.drawImage(t, rowX + rowW - 30 - tw, row0Y + (rowH - th) / 2, tw, th, null);
        }

        // Row 1: Volume (bar)
        int row1Y = row0Y + 105;
        drawRow(rowX, row1Y, rowW, rowH, menuIndex == 1);
        drawLabel("Volume", rowX + 30, row1Y, rowH);

        // Row 2: Back (button)
        int btnW = 300, btnH = 78;
        drawButton("Back", (gp.screenWidth - btnW) / 2, row1Y + 120, btnW, btnH, menuIndex == 2);

        drawHint("A/D: change     Enter or J: select     Esc: back");
    }

    private void drawCredits() {
        drawRibbon("CREDITS", 100);

        int panelW = 600, panelH = 420;
        int panelX = (gp.screenWidth - panelW) / 2, panelY = 190;
        nineSlice(panel, 11, 11, 11, 11, panelX, panelY, panelW, panelH);

        // Edit these lines with your own names
        g2.setFont(fontMid);
        drawCentered("Programming", panelY + 80,  inkDark);
        drawCentered("(your name)", panelY + 115, inkDark);
        drawCentered("Art", panelY + 180, inkDark);
        drawCentered("Post Apocalypse Asset Pack", panelY + 215, inkDark);
        drawCentered("Pixel RPG UI", panelY + 280, inkDark);
        drawCentered("StationSupply", panelY + 315, inkDark);

        drawButton("Back", (gp.screenWidth - 300) / 2, panelY + panelH + 25, 300, 78, true);
        drawHint("Enter, J or Esc: back");
    }

    // Placeholder battle layout. Reads the dummy fields above; swap them for your battle data later.
    private void drawBattle() {
        // Enemy info (top left) and player info (middle right)
        drawInfoBox(40, 40, 400, 110, battleEnemyName, enemyHP, enemyMaxHP);
        drawInfoBox(gp.screenWidth - 440, 250, 400, 110, battlePlayerName, gp.playerHP, gp.playerMaxHP);

        // Bottom panel: message + 2x2 command buttons
        int panelX = 24, panelY = 400, panelW = gp.screenWidth - 48, panelH = 320;
        nineSlice(panel, 11, 11, 11, 11, panelX, panelY, panelW, panelH);

        g2.setFont(fontMid);
        g2.setColor(inkDark);
        g2.drawString(battleMessage, panelX + 30, panelY + 60);

        int btnW = 320, btnH = 78;
        for (int i = 0; i < battleOptions.length; i++) {
            int col = i % 2, row = i / 2;
            int bx = panelX + 30 + col * (btnW + 20);
            int by = panelY + 100 + row * 100;
            drawButton(battleOptions[i], bx, by, btnW, btnH, i == menuIndex);
        }

        drawHint("W/A/S/D: move     Enter or J: select     Esc: leave");
    }

    // Item grid on top, description box underneath. Reads gp.inventory, never changes it.
    private void drawInventory() {
        drawRibbon("INVENTORY", 30);

        int panelW = 640, panelH = 520;
        int panelX = (gp.screenWidth - panelW) / 2, panelY = 120;
        nineSlice(panel, 11, 11, 11, 11, panelX, panelY, panelW, panelH);

        int cols = Inventory.COLS, rows = Inventory.ROWS;
        int slot = 80, gap = 12;
        int gridW = cols * slot + (cols - 1) * gap;
        int gridX = panelX + (panelW - gridW) / 2, gridY = panelY + 30;

        // Slots
        for (int i = 0; i < Inventory.SIZE; i++) {
            int x = gridX + (i % cols) * (slot + gap);
            int y = gridY + (i / cols) * (slot + gap);
            nineSlice(i == invIndex ? slotSelected : slotNormal, 8, 8, 8, 8, x, y, slot, slot);

            Item item = gp.inventory.getItem(i);
            if (item != null) {
                drawItem(item, gp.inventory.getCount(i), x, y, slot);
            }
        }

        // Description box for the highlighted slot
        int boxX = panelX + 30, boxW = panelW - 60;
        int boxY = gridY + rows * (slot + gap) - gap + 20;
        int boxH = panelY + panelH - 20 - boxY;
        drawRow(boxX, boxY, boxW, boxH, false);

        Item sel = gp.inventory.getItem(invIndex);
        g2.setColor(inkDark);
        if (sel == null) {
            g2.setFont(fontMid);
            g2.drawString("Empty", boxX + 20, boxY + 40);
        } else {
            g2.setFont(fontMid);
            g2.drawString(sel.name, boxX + 20, boxY + 35);
            g2.setFont(fontSmall);
            drawWrapped(sel.description, boxX + 20, boxY + 60, boxW - 40, 16);
        }

        // Feedback line + controls
        if (!invMessage.isEmpty()) {
            g2.setFont(fontSmall);
            drawCentered(invMessage, panelY + panelH + 30, inkLight);
        }
        drawHint("W/A/S/D: move     Enter or J: use     Esc or I: close");
    }

    // One item inside a slot: icon (or first letter if there's no icon) + stack count.
    private void drawItem(Item item, int count, int x, int y, int size) {
        BufferedImage icon = getIcon(item.iconFile);
        if (icon != null) {
            g2.drawImage(icon, x + 10, y + 10, size - 20, size - 20, null);
        } else {
            g2.setFont(fontBig);
            drawCentered(item.name.substring(0, 1).toUpperCase(), y + size / 2 + 9, inkDark, x + size / 2);
        }

        if (count > 1) {
            g2.setFont(fontSmall);
            String c = "x" + count;
            g2.setColor(inkDark);
            g2.drawString(c, x + size - 8 - g2.getFontMetrics().stringWidth(c), y + size - 8);
        }
    }

    // ===== PIECES =====

    // Red ribbon with a title. The forked ends stay whole, the middle stretches.
    private void drawRibbon(String text, int y) {
        int x = (gp.screenWidth - w) / 2;
        nineSlice(ribbon, 15, 0, 15, 0, x, y, w, h);
        g2.setFont(fontBig);
        drawCentered(text, y + (h + g2.getFontMetrics().getAscent()) / 2 - 3, inkLight);
    }

    // Wooden button. The selected one uses the "focus" version (blue outline).
    private void drawButton(String text, int x, int y, int w, int h, boolean selected) {
        BufferedImage img = selected ? btnFocus : btnNormal;
        nineSlice(img, 9, 9, 9, 10, x, y, w, h);   // state files have 1px padding, so margins are +1
        g2.setFont(fontMid);
        int ty = y + (h + g2.getFontMetrics().getAscent()) / 2 - 6;
    }

    private void drawRow(int x, int y, int w, int h, boolean selected) {
        nineSlice(selected ? slotSelected : slotNormal, 8, 8, 8, 8, x, y, w, h);
    }

    private void drawLabel(String text, int x, int rowY, int rowH) {
        g2.setFont(fontMid);
        g2.setColor(inkDark);
        g2.drawString(text, x, rowY + (rowH + g2.getFontMetrics().getAscent()) / 2 - 3);
    }

        g2.drawImage(barFrame, x, y, barFrame.getWidth() * S, barFrame.getHeight() * S, null);

        // The cavity inside the frame starts at (10, 5) in art pixels
        int cx = x + 10 * S, cy = y + 5 * S;
        int cw = barTrough.getWidth() * S, ch = barTrough.getHeight() * S;
        g2.drawImage(barTrough, cx, cy, cw, ch, null);

        if (fillArt > 0) {
        }
    }

    private void drawHint(String text) {
        g2.setFont(fontSmall);
        drawCentered(text, gp.screenHeight - 30, hintColor);
    }

    // ===== HELPERS =====

    // Centered on the whole screen.
    private void drawCentered(String text, int y, Color color) {
        g2.setColor(color);
        g2.drawString(text, x, y);
    }

    // Word-wraps text inside maxWidth using the current font.
    private void drawWrapped(String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        String line = "";
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
                g2.drawString(line, x, y);
                y += lineHeight;
                line = word;
            } else {
                line = test;
            }
        }
        if (!line.isEmpty()) g2.drawString(line, x, y);
    }

    // Draws an image as a 9-slice: corners keep their size (zoomed by S), edges and centre stretch.
    // l, t, r, b are the margins in ART pixels. Use 0 for t and b to make a horizontal 3-slice.
    private void nineSlice(BufferedImage img, int l, int t, int r, int b,
                           int dx, int dy, int dw, int dh) {
        if (img == null) return;
        int sw = img.getWidth(), sh = img.getHeight();

        // Source columns / rows
        int[] sx = {0, l, sw - r, sw};
        int[] sy = {0, t, sh - b, sh};

        // Destination columns / rows (margins are zoomed by S, the middle takes what's left)
        int[] ddx = {dx, dx + l * S, dx + dw - r * S, dx + dw};
        int[] ddy = {dy, dy + t * S, dy + dh - b * S, dy + dh};

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (sx[col + 1] <= sx[col] || sy[row + 1] <= sy[row]) continue;   // empty piece
                g2.drawImage(img,
                        ddx[col], ddy[row], ddx[col + 1], ddy[row + 1],
                        sx[col], sy[row], sx[col + 1], sy[row + 1], null);
            }
        }
    }
// Draws the background image, slowly panning left and right.
    private void drawBackground() {
        if (bgImage == null) {                       // no image found: plain color fallback
            g2.setColor(background);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            return;
        }

        // Scale the image to cover the screen height, and make it at least 25% wider
        // than the screen so there is always room to pan.
        double scale = Math.max((double) gp.screenHeight / bgImage.getHeight(),
                gp.screenWidth * 1.25 / bgImage.getWidth());
        int w = (int) (bgImage.getWidth() * scale);
        int h = (int) (bgImage.getHeight() * scale);

        // How far the image can slide (the part that sticks out past the screen)
        double travel = w - gp.screenWidth;

        // Ping-pong: moves 0 -> travel, then travel -> 0, forever, at PAN_SPEED.
        // Based on the clock, so the speed doesn't depend on the frame rate.
        double seconds = System.nanoTime() / 1_000_000_000.0;
        double t = (seconds * PAN_SPEED / travel) % 2.0;   // 0..2
        if (t > 1.0) t = 2.0 - t;                           // fold into 0..1..0
        int offsetX = (int) (t * travel);

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);   // smooth scaling for the background
        g2.drawImage(bgImage, -offsetX, (gp.screenHeight - h) / 2, w, h, null);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // back to crisp for the UI

        // Slight dark overlay so the menu stays readable. Lower 90 for a brighter background.
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
    }
}