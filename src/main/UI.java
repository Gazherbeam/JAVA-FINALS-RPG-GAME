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
    public final int settingsOptionCount = 4;   // Show FPS, SFX, Volume, Back
    public boolean showFPS = false;
    public boolean sfxEnabled = true;           // Also controls background music
    public int volume = 5;                      // Shared sound volume: 0-10

    // ----- Inventory menu state (changed by GamePanel) -----
    public int invIndex = 0;          // which slot the cursor is on
    public int menuCategory = 0;      // Inventory is selected when the menu opens
    public boolean inventoryDetails = false;
    public boolean categoryDetails = false;
    private final String[] menuCategories = {"Inventory", "Spells", "Equipment", "Status", "System"};
    public String invMessage = "";    // short feedback line ("Used Potion.")
    private final Map<String, BufferedImage> iconCache = new HashMap<>();

    // ----- Loaded images -----
    BufferedImage btnNormal, btnFocus;
    BufferedImage panel, ribbon;
    BufferedImage slotNormal, slotSelected;
    BufferedImage toggleOn, toggleOff;
    BufferedImage barFrame, barTrough, barFill, barFillRed;

    // ----- Background -----
    BufferedImage bgImage;
    BufferedImage menuBackground;
    static final String BG_PATH = "/fantasy-backgrounds-free-v1.0.1/fantasy-backgrounds-free/320x160/harbor_day.png";
    static final int PAN_SPEED = 30;   // pixels per second
    static final int DIM_ALPHA = 110;   // 0 = no darkening, 255 = black. Lower is lighter.
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
        menuBackground = loadResource("/menu_background.png");

        loadFont("fonts/PressStart2P-Regular.ttf");
    }

    // ===== LOADING =====

    private BufferedImage load(String file) {
        return loadResource(BASE + file);
    }

    private BufferedImage loadResource(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage loadAbsolute(String path) {
        return loadResource(path);
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

        // Menus that open on top of the game show the frozen, darkened world.
        // The other screens (title, settings, credits) use the panning PNG.
        boolean overWorld = false;
        if (gp.gameState == gp.inventoryState) drawMenuBackdrop();
        else drawBackground();

        if (gp.gameState == gp.titleState)     drawTitle();
        if (gp.gameState == gp.settingsState)  drawSettings();
        if (gp.gameState == gp.creditsState)   drawCredits();
        if (gp.gameState == gp.inventoryState) drawInventory();
    }

    // Called on top of the map while playing.
    public void drawPlayOverlay(Graphics2D g2) {
        this.g2 = g2;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // HP bar, top left. Keep the existing HUD layout and add progression text below it.
        drawBar(12, 12, gp.playerMaxHP > 0 ? gp.playerHP / (double) gp.playerMaxHP : 0, barFillRed);
        // MP bar, top right.
        drawStatBar(
                "MP",
                gp.playerMP,
                gp.playerMaxMP,
                gp.screenWidth - 190,
                12,
                170,
                new Color(70, 190, 105)
        );

        g2.setFont(fontSmall);
        g2.setColor(Color.WHITE);
        if (gp.progression.level >= PlayerProgression.MAX_LEVEL) {
            g2.drawString("LV " + gp.progression.level + "   EXP MAX", 12, 62);
        } else {
            g2.drawString("LV " + gp.progression.level + "   EXP "
                    + gp.progression.experience + "/" + gp.progression.experienceToNext, 12, 62);
        }

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

        int panelW = 600, panelH = 520;
        int panelX = (gp.screenWidth - panelW) / 2, panelY = 145;
        nineSlice(panel, 11, 11, 11, 11, panelX, panelY, panelW, panelH);

        int rowX = panelX + 45, rowW = panelW - 90, rowH = 72;

        // Row 0: Show FPS toggle.
        int row0Y = panelY + 35;
        drawRow(rowX, row0Y, rowW, rowH, menuIndex == 0);
        drawLabel("Show FPS", rowX + 30, row0Y, rowH);
        drawToggle(showFPS, rowX + rowW - 30, row0Y, rowH);

        // Row 1: SFX toggle. This also mutes background music.
        int row1Y = row0Y + 90;
        drawRow(rowX, row1Y, rowW, rowH, menuIndex == 1);
        drawLabel("SFX", rowX + 30, row1Y, rowH);
        drawToggle(sfxEnabled, rowX + rowW - 30, row1Y, rowH);

        // Row 2: Shared music / sound-effect volume.
        int row2Y = row1Y + 90;
        drawRow(rowX, row2Y, rowW, rowH, menuIndex == 2);
        drawLabel("Volume", rowX + 30, row2Y, rowH);
        drawBar(rowX + rowW - 30 - barFrame.getWidth() * S,
                row2Y + (rowH - barFrame.getHeight() * S) / 2,
                volume / 10.0, barFill);

        // Row 3: Back.
        int btnW = 300, btnH = 72;
        drawButton("Back", (gp.screenWidth - btnW) / 2, row2Y + 95, btnW, btnH, menuIndex == 3);

        drawHint("A/D: change     Enter or J: toggle     Esc: back");
    }

    private void drawToggle(boolean enabled, int rightX, int rowY, int rowH) {
        BufferedImage image = enabled ? toggleOn : toggleOff;
        if (image != null) {
            int tw = image.getWidth() * S, th = image.getHeight() * S;
            g2.drawImage(image, rightX - tw, rowY + (rowH - th) / 2, tw, th, null);
        }
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

    // Octopath-inspired in-game menu: static illustrated centre, category rail, party panel.
    private void drawInventory() {
        if (inventoryDetails) { drawInventoryDetails(); return; }
        if (categoryDetails) { drawCategoryDetails(); return; }

        // Header
        g2.setFont(fontBig);
        g2.setColor(new Color(58, 39, 23));
        g2.drawString("MENU", 28, 43);
        g2.setFont(fontSmall);
        g2.drawString("ADVENTURE RECORD", 30, 62);

        // Left navigation panel
        int lx = 18, ly = 92, lw = 208, rowH = 58;
        drawOrnatePanel(lx, ly, lw, menuCategories.length * rowH + 18);
        for (int i = 0; i < menuCategories.length; i++) {
            int y = ly + 10 + i * rowH;
            if (i == menuCategory) {
                g2.setColor(new Color(92, 62, 34, 220));
                g2.fillRoundRect(lx + 8, y + 3, lw - 16, rowH - 6, 10, 10);
                g2.setColor(new Color(244, 210, 112));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(lx + 8, y + 3, lw - 16, rowH - 6, 10, 10);
            }
            g2.setFont(fontMid);
            g2.setColor(i == menuCategory ? new Color(255, 239, 190) : inkDark);
            g2.drawString((i == menuCategory ? "▶ " : "   ") + menuCategories[i], lx + 19, y + 36);
        }

        // Centre feature panel previews the selected category.
        int cx = 240, cy = 92, cw = 300, ch = 544;
        drawOrnatePanel(cx, cy, cw, ch);
        g2.setFont(fontBig);
        g2.setColor(inkDark);
        drawCentered(menuCategories[menuCategory].toUpperCase(), cy + 45, inkDark, cx + cw / 2);
        if (menuCategory == 0) {
            g2.setFont(fontMid);
            drawCentered("Your collected items", cy + 88, inkDark, cx + cw / 2);
            drawCentered(gp.inventory.getUsedSlots() + " / " + Inventory.SIZE + " slots occupied", cy + 120, inkDark, cx + cw / 2);
            drawCentered("Press ENTER to open", cy + ch - 35, inkDark, cx + cw / 2);
        } else {
            g2.setFont(fontMid);
            String description = categoryDescription(menuCategory);
            drawWrappedCentered(description, cx + 24, cy + 390, cw - 48, 24);
            g2.setFont(fontSmall);
            drawCentered("More features will be added here", cy + ch - 35, inkDark, cx + cw / 2);
        }

        // Right party/status panel
        int rx = 552, ry = 92, rw = 198, rh = 544;
        drawOrnatePanel(rx, ry, rw, rh);
        g2.setFont(fontMid);
        drawCentered("PARTY", ry + 38, inkDark, rx + rw / 2);
        g2.setColor(new Color(255,255,255,100));
        g2.drawLine(rx + 15, ry + 52, rx + rw - 15, ry + 52);
        // Character portrait/icon removed. Keep the party information text-only.
        g2.setFont(fontMid);
        drawCentered("Hero", ry + 105, inkDark, rx + rw / 2);
        g2.setFont(fontSmall);
        drawCentered("Lv. " + gp.progression.level, ry + 132, inkDark, rx + rw / 2);
        drawStatBar("HP", gp.playerHP, gp.playerMaxHP, rx + 18, ry + 175, rw - 36, new Color(93, 145, 76));
        drawStatBar("MP", gp.playerMP, gp.playerMaxMP, rx + 18, ry + 230, rw - 36, new Color(74, 133, 184));
        g2.setFont(fontSmall);
        drawCentered("EXP", ry + 300, inkDark, rx + rw / 2);
        drawCentered(gp.progression.level >= PlayerProgression.MAX_LEVEL ? "MAX" : gp.progression.experience + " / " + gp.progression.experienceToNext, ry + 325, inkDark, rx + rw / 2);

        drawHint("W/S: Select     ENTER: Confirm     ESC / I: Return to game");
    }

    private void drawInventoryDetails() {
        drawRibbon("INVENTORY", 24);
        int panelW = 690, panelH = 560;
        int panelX = (gp.screenWidth - panelW) / 2, panelY = 110;
        drawOrnatePanel(panelX, panelY, panelW, panelH);
        int cols = Inventory.COLS, rows = Inventory.ROWS;
        int slot = 76, gap = 10;
        int gridW = cols * slot + (cols - 1) * gap;
        int gridX = panelX + (panelW - gridW) / 2, gridY = panelY + 28;
        for (int i = 0; i < Inventory.SIZE; i++) {
            int x = gridX + (i % cols) * (slot + gap);
            int y = gridY + (i / cols) * (slot + gap);
            nineSlice(i == invIndex ? slotSelected : slotNormal, 8, 8, 8, 8, x, y, slot, slot);
            Item item = gp.inventory.getItem(i);
            if (item != null) drawItem(item, gp.inventory.getCount(i), x, y, slot);
        }
        int boxX = panelX + 30, boxW = panelW - 60;
        int boxY = gridY + rows * (slot + gap) + 8;
        int boxH = panelY + panelH - 22 - boxY;
        drawRow(boxX, boxY, boxW, boxH, false);
        Item sel = gp.inventory.getItem(invIndex);
        g2.setColor(inkDark);
        g2.setFont(fontMid);
        g2.drawString(sel == null ? "Empty slot" : sel.name, boxX + 18, boxY + 31);
        if (sel != null) {
            g2.setFont(fontSmall);
            g2.drawString("Category: " + sel.category.name().replace('_', ' '), boxX + 18, boxY + 52);
            drawWrapped(sel.description, boxX + 18, boxY + 72, boxW - 36, 16);
        }
        if (!invMessage.isEmpty()) { g2.setFont(fontSmall); drawCentered(invMessage, panelY + panelH + 20, inkLight); }
        drawHint("W/A/S/D: Move     ENTER: Use     ESC: Back to menu");
    }

    private String categoryDescription(int index) {
        switch (index) {
            case 0: return "View the items you have collected during your journey.";
            case 1: return "Review the spells and magic available to your character.";
            case 2: return "Manage your weapons, armor, and equipped gear.";
            case 3: return "Review your character's attributes and progression.";
            default: return "Adjust game options and access system functions.";
        }
    }

    private void drawCategoryDetails() {
        String category = menuCategories[menuCategory];
        // Display the actual spell list instead of the generic category screen.
        if (menuCategory == 1) {
            drawSpellDetails();
            return;
        }
        drawRibbon(category.toUpperCase(), 24);
        int panelW = 690, panelH = 560;
        int panelX = (gp.screenWidth - panelW) / 2, panelY = 110;
        drawOrnatePanel(panelX, panelY, panelW, panelH);
        g2.setFont(fontMid);
        drawCentered(category + " screen", panelY + 100, inkDark);
        g2.setFont(fontSmall);
        drawCentered("This menu destination is working.", panelY + 145, inkDark);
        drawCentered("Its contents will be implemented in a later step.", panelY + 175, inkDark);
        drawHint("ESC: Back to menu");
    }


    private void drawSpellDetails() {

        drawRibbon("SPELLS", 24);

        int panelW = 690;
        int panelH = 560;
        int panelX = (gp.screenWidth - panelW) / 2;
        int panelY = 110;

        drawOrnatePanel(panelX, panelY, panelW, panelH);

        g2.setFont(fontSmall);
        g2.setColor(inkDark);

        drawCentered(
                "Known spells and their quick-cast assignments",
                panelY + 35,
                inkDark
        );

        int rowY = panelY + 65;

        int rowHeight = 125;
        int rowX = panelX + 25;
        int rowWidth = panelW - 50;

        if (gp.spells.getSpells().isEmpty()) {

            g2.setFont(fontMid);
            drawCentered(
                    "No spells learned yet.",
                    panelY + 180,
                    inkDark
            );

            drawHint("ESC: Back to menu");
            return;
        }

        for (SpellList.Spell spell : gp.spells.getSpells()) {

            drawRow(
                    rowX,
                    rowY,
                    rowWidth,
                    rowHeight - 10,
                    false
            );

            // Spell name.
            g2.setFont(fontMid);
            g2.setColor(inkDark);

            g2.drawString(
                    spell.name,
                    rowX + 18,
                    rowY + 29
            );

            // Mana cost.
            g2.setFont(fontSmall);

            g2.drawString(
                    "Cost: " + spell.manaCost + " MP",
                    rowX + rowWidth - 150,
                    rowY + 29
            );

            // Find the quick-cast key assigned to this spell.
            String boundKey = "Not bound";

            for (int slot = 0; slot < gp.spells.getBindCount(); slot++) {

                SpellList.Spell boundSpell =
                        gp.spells.getBoundSpell(slot);

                if (boundSpell != null
                        && boundSpell.id.equals(spell.id)) {

                    boundKey = gp.spells.getBindName(slot);
                    break;
                }
            }

            // Description.
            g2.setFont(fontSmall);

            drawWrapped(
                    spell.description,
                    rowX + 18,
                    rowY + 55,
                    rowWidth - 36,
                    17
            );

            // Quick-cast assignment.
            g2.drawString(
                    "Quick Cast: " + boundKey,
                    rowX + 18,
                    rowY + 96
            );

            rowY += rowHeight;
        }

        drawHint("ESC: Back to menu");
    }

    private void drawOrnatePanel(int x, int y, int w, int h) {
        g2.setColor(new Color(235, 211, 164, 218));
        g2.fillRoundRect(x, y, w, h, 18, 18);
        g2.setColor(new Color(92, 61, 34, 230));
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x + 2, y + 2, w - 4, h - 4, 18, 18);
        g2.setColor(new Color(255, 245, 211, 150));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(x + 8, y + 8, w - 16, h - 16, 12, 12);
    }

    private void drawStatBar(String label, int value, int max, int x, int y, int w, Color fill) {
        g2.setFont(fontSmall); g2.setColor(inkDark);
        g2.drawString(label + "  " + value + "/" + max, x, y);
        g2.setColor(new Color(70, 52, 35)); g2.fillRoundRect(x, y + 8, w, 12, 6, 6);
        g2.setColor(fill);
        int amount = max <= 0 ? 0 : (int)(w * Math.max(0, Math.min(1, value / (double)max)));
        g2.fillRoundRect(x, y + 8, amount, 12, 6, 6);
    }

    private void drawWrappedCentered(String text, int x, int y, int width, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics(); String line = "";
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > width && !line.isEmpty()) {
                drawCentered(test = line, y, inkDark, x + width / 2); y += lineHeight; line = word;
            } else line = test;
        }
        if (!line.isEmpty()) drawCentered(line, y, inkDark, x + width / 2);
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
        int w = ribbon.getWidth() * S;
        int h = ribbon.getHeight() * S;
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
        drawCentered(text, ty, inkLight, x + w / 2);
    }

    private void drawRow(int x, int y, int w, int h, boolean selected) {
        nineSlice(selected ? slotSelected : slotNormal, 8, 8, 8, 8, x, y, w, h);
    }

    private void drawLabel(String text, int x, int rowY, int rowH) {
        g2.setFont(fontMid);
        g2.setColor(inkDark);
        g2.drawString(text, x, rowY + (rowH + g2.getFontMetrics().getAscent()) / 2 - 3);
    }

    private void drawBar(int x, int y, double fillRatio, BufferedImage fillArt) {
        g2.drawImage(barFrame, x, y, barFrame.getWidth() * S, barFrame.getHeight() * S, null);

        // The cavity inside the frame starts at (10, 5) in art pixels
        int cx = x + 10 * S, cy = y + 5 * S;
        int cw = barTrough.getWidth() * S, ch = barTrough.getHeight() * S;
        g2.drawImage(barTrough, cx, cy, cw, ch, null);

        if (fillArt != null && fillRatio > 0) {

            int fillW = (int) (cw * fillRatio);
            int srcW = (int) (fillArt.getWidth() * fillRatio);
            if (srcW > 0) {
                g2.drawImage(fillArt, cx, cy, cx + fillW, cy + ch,
                        0, 0, srcW, fillArt.getHeight(), null);
            }
        }
    }

    private void drawHint(String text) {
        g2.setFont(fontSmall);
        drawCentered(text, gp.screenHeight - 30, hintColor);
    }

    // ===== HELPERS =====

    // Centered on the whole screen.
    private void drawCentered(String text, int y, Color color) {
        int x = (gp.screenWidth - g2.getFontMetrics().stringWidth(text)) / 2;
        g2.setColor(color);
        g2.drawString(text, x, y);
    }

    // Centered at a specific x position.
    private void drawCentered(String text, int y, Color color, int centerX) {
        int x = centerX - g2.getFontMetrics().stringWidth(text) / 2;
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

    // Static illustrated parchment background for the in-game menu.
    private void drawMenuBackdrop() {
        if (menuBackground != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(menuBackground, 0, 0, gp.screenWidth, gp.screenHeight, null);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        } else {
            g2.setColor(new Color(188, 151, 101));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
    }

    // Draws the game world (frozen, because update() is paused) and darkens it slightly.
    private void drawWorldBackdrop() {
        gp.tileM.draw(g2);
        gp.enemyM.draw(g2);
        gp.Player.draw(g2);

        g2.setColor(new Color(0, 0, 0, DIM_ALPHA));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
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