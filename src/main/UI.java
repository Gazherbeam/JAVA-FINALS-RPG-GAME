package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

// Draws the menu screens using the wood-themed Pixel RPG UI pack.
// GamePanel calls draw() when the game isn't in playState.
public class UI {

    GamePanel gp;
    Graphics2D g2;

    // ===== WHERE THE ASSETS ARE (inside res). Change this if you put the folder elsewhere. =====
    static final String BASE = "/pixel-rpg-ui-free-sample/";

    // The art is drawn at 1x (tiny), so everything is zoomed by this factor.
    static final int S = 3;

    // ===== MENU BACKGROUND =====
    static final String BG_PATH = "/fantasy-backgrounds-free-v1.0.1/fantasy-backgrounds-free/320x160/harbor_day.png/";  // change to your PNG's path inside res
    static final double PAN_SPEED = 40;                      // pixels per second. Higher = faster
    BufferedImage bgImage;

    // ----- Menu state (used by KeyHandler) -----
    public int menuIndex = 0;
    public final String[] titleOptions = {"Start Game", "Settings", "Credits", "Quit"};
    public final int settingsOptionCount = 3;   // Show FPS, Volume, Back
    public boolean showFPS = false;
    public int volume = 5;                      // 0-10, not connected to sound yet

    // ----- Loaded images -----
    BufferedImage btnNormal, btnFocus;
    BufferedImage panel, ribbon;
    BufferedImage slotNormal, slotSelected;
    BufferedImage toggleOn, toggleOff;
    BufferedImage barFrame, barTrough, barFill;

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


        bgImage = loadAbsolute(BG_PATH);

        loadFont("fonts/PressStart2P-Regular.ttf");
    }

    // ===== LOADING =====

    private BufferedImage loadAbsolute(String path) {      // <-- add this whole method
        try (InputStream in = getClass().getResourceAsStream(path)) {
            return ImageIO.read(in);
        } catch (Exception e) {
            System.out.println("UI: could not load background " + path);
            return null;
        }
    }


    private BufferedImage load(String file) {
        try (InputStream in = getClass().getResourceAsStream(BASE + file)) {
            return ImageIO.read(in);
        } catch (Exception e) {
            System.out.println("UI: could not load " + BASE + file);
            return null;
        }
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

    // ===== MAIN ENTRY =====

    public void draw(Graphics2D g2) {
        this.g2 = g2;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // keeps pixel art sharp
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);               // crisp pixel font

        drawBackground();

        if (gp.gameState == gp.titleState)    drawTitle();
        if (gp.gameState == gp.settingsState) drawSettings();
        if (gp.gameState == gp.creditsState)  drawCredits();
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
        drawVolumeBar(rowX + rowW - 30 - barFrame.getWidth() * S,
                row1Y + (rowH - barFrame.getHeight() * S) / 2);

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

    // ===== PIECES =====

    // Red ribbon with a title. The forked ends stay whole, the middle stretches.
    private void drawRibbon(String text, int y) {
        int w = 540, h = ribbon.getHeight() * S;
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
        drawCentered(text, ty, inkLight);
    }

    // Slot frame behind a settings row. Gold when selected.
    private void drawRow(int x, int y, int w, int h, boolean selected) {
        nineSlice(selected ? slotSelected : slotNormal, 8, 8, 8, 8, x, y, w, h);
    }

    private void drawLabel(String text, int x, int rowY, int rowH) {
        g2.setFont(fontMid);
        g2.setColor(inkDark);
        g2.drawString(text, x, rowY + (rowH + g2.getFontMetrics().getAscent()) / 2 - 3);
    }

    // Bar frame + trough + gold fill. volume is 0-10.
    private void drawVolumeBar(int x, int y) {
        g2.drawImage(barFrame, x, y, barFrame.getWidth() * S, barFrame.getHeight() * S, null);

        // The cavity inside the frame starts at (10, 5) in art pixels
        int cx = x + 10 * S, cy = y + 5 * S;
        int cw = barTrough.getWidth() * S, ch = barTrough.getHeight() * S;
        g2.drawImage(barTrough, cx, cy, cw, ch, null);

        int fillArt = barFill.getWidth() * volume / 10;   // how many art pixels of fill to show
        if (fillArt > 0) {
            g2.drawImage(barFill, cx, cy, cx + fillArt * S, cy + ch,
                    0, 0, fillArt, barFill.getHeight(), null);
        }
    }

    private void drawHint(String text) {
        g2.setFont(fontSmall);
        drawCentered(text, gp.screenHeight - 30, hintColor);
    }

    // ===== HELPERS =====

    private void drawCentered(String text, int y, Color color) {
        int x = (gp.screenWidth - g2.getFontMetrics().stringWidth(text)) / 2;
        g2.setColor(color);
        g2.drawString(text, x, y);
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
