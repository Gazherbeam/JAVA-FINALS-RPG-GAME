package main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import main.EquipmentList.Equipment;
import main.EquipmentList.Slot;
import main.EquipmentList.Stat;

/**
 * The Equipment screen opened from the in-game menu (Menu > Equipment).
 *
 *  - Slot list:  pick Weapon / Head / Body / Accessory.
 *  - Gear list:  pick one of the pieces you own for that slot (or take it off).
 *                The stat box previews what the change would do before you confirm.
 *
 * GamePanel sends the keys to handleInput(...); UI.drawCategoryDetails() calls draw(...).
 */
public class EquipmentMenu {

    private static final int ROW_H = 64;       // height of one list row
    private static final int PITCH = 72;       // distance from one row to the next
    private static final int VISIBLE = 5;      // rows shown at once in the gear list

    private final GamePanel gp;
    private final Slot[] slots = Slot.values();

    private boolean picking = false;           // false = slot list, true = gear list
    private int slotIndex = 0;
    private int pickIndex = 0;
    private final List<Equipment> choices = new ArrayList<>();   // gear list; index 0 is "Unequip" (null)
    private String message = "";

    private final Color emptyColor = new Color(120, 90, 60);
    private final Color goodColor  = new Color(40, 120, 40);
    private final Color badColor   = new Color(170, 40, 30);
    private final Color lightInk   = new Color(255, 239, 190);   // text on a selected (dark) row

    public EquipmentMenu(GamePanel gp) {
        this.gp = gp;
    }

    // ===== INPUT =====

    // Returns true when the player backs out of the equipment screen entirely.
    public boolean handleInput(boolean up, boolean down, boolean confirm, boolean back) {
        if (!picking) {
            if (up)   slotIndex = (slotIndex + slots.length - 1) % slots.length;
            if (down) slotIndex = (slotIndex + 1) % slots.length;
            if (up || down) message = "";

            if (confirm) {
                openGearList();
            }
            if (back) {
                message = "";
                return true;
            }
            return false;
        }

        int n = choices.size();
        if (up)   pickIndex = (pickIndex + n - 1) % n;
        if (down) pickIndex = (pickIndex + 1) % n;
        if (up || down) message = "";

        if (back) {
            picking = false;
            return false;
        }
        if (confirm) {
            applyChoice();
        }
        return false;
    }

    private void openGearList() {
        Slot slot = slots[slotIndex];
        choices.clear();
        choices.add(null);                                   // "Unequip"
        choices.addAll(gp.equipment.getOwned(slot));

        // Start the cursor on the piece that is worn now.
        pickIndex = 0;
        Equipment worn = gp.equipment.getEquipped(slot);
        if (worn != null) {
            int found = choices.indexOf(worn);
            if (found >= 0) pickIndex = found;
        } else if (choices.size() > 1) {
            pickIndex = 1;
        }

        message = "";
        picking = true;
    }

    private void applyChoice() {
        Slot slot = slots[slotIndex];
        Equipment chosen = choices.get(pickIndex);
        Equipment worn = gp.equipment.getEquipped(slot);

        if (chosen == null) {
            if (worn == null) {
                message = "Nothing is equipped there.";
                return;
            }
            gp.equipment.unequip(slot);
            message = "Took off " + worn.name + ".";
        } else if (gp.equipment.isEquipped(chosen)) {
            message = chosen.name + " is already equipped.";
            return;
        } else {
            gp.equipment.equip(chosen);
            message = "Equipped " + chosen.name + ".";
        }
        picking = false;   // back to the slot list so the new gear shows
    }

    // ===== DRAWING =====

    public void draw(UI ui) {
        Graphics2D g2 = ui.g2;

        int pw = 690, ph = 560;
        int px = (gp.screenWidth - pw) / 2, py = 110;

        ui.drawRibbon("EQUIPMENT", 24);
        ui.drawOrnatePanel(px, py, pw, ph);

        int top = py + 30;
        int leftX = px + 25, leftW = 300;
        int rightX = leftX + leftW + 25;
        int rightW = px + pw - 25 - rightX;

        Slot slot = slots[slotIndex];
        Equipment candidate = picking ? choices.get(pickIndex) : null;

        // ----- left column: slot list or gear list -----
        g2.setFont(ui.fontSmall);
        g2.setColor(ui.inkDark);
        g2.drawString(picking ? "CHOOSE " + slot.label.toUpperCase() : "EQUIPPED", leftX, top + 8);

        if (!picking) {
            drawSlotList(ui, leftX, top + 22, leftW);
        } else {
            drawGearList(ui, leftX, top + 22, leftW, top + 8);
        }

        // ----- right column: stats with a preview of the highlighted change -----
        g2.setFont(ui.fontSmall);
        g2.setColor(ui.inkDark);
        g2.drawString("STATS", rightX, top + 8);
        drawStats(ui, rightX, top + 22, rightW, slot, candidate);

        // ----- bottom: description of the highlighted piece -----
        int boxX = px + 25, boxW = pw - 50;
        int boxY = top + 22 + VISIBLE * PITCH + 10;
        int boxH = py + ph - 25 - boxY;
        drawBox(g2, boxX, boxY, boxW, boxH, false);

        Equipment shown = picking ? candidate : gp.equipment.getEquipped(slot);
        String title;
        String bonus;
        String description;
        if (shown != null) {
            title = shown.name;
            bonus = shown.bonusText();
            description = shown.description;
        } else if (picking) {
            title = "Unequip";
            bonus = "";
            description = "Take off the gear in this slot.";
        } else {
            title = "Empty slot";
            bonus = "";
            description = "Press ENTER to choose something to wear here.";
        }

        g2.setColor(ui.inkDark);
        g2.setFont(ui.fontMid);
        g2.drawString(title, boxX + 18, boxY + 31);
        g2.setFont(ui.fontSmall);
        if (!bonus.isEmpty()) g2.drawString(bonus, boxX + 18, boxY + 52);
        ui.drawWrapped(description, boxX + 18, boxY + 76, boxW - 36, 16);

        if (!message.isEmpty()) {
            g2.setFont(ui.fontSmall);
            ui.drawCentered(message, py + ph + 20, ui.inkLight);
        }

        ui.drawHint(picking
                ? "UP/DOWN: Move     ENTER: Equip     ESC: Back"
                : "UP/DOWN: Move     ENTER: Choose gear     ESC: Back to menu");
    }

    private void drawSlotList(UI ui, int x, int y0, int w) {
        Graphics2D g2 = ui.g2;

        for (int i = 0; i < slots.length; i++) {
            int y = y0 + i * PITCH;
            boolean selected = i == slotIndex;
            drawBox(g2, x, y, w, ROW_H, selected);

            g2.setFont(ui.fontSmall);
            g2.setColor(selected ? lightInk : ui.inkDark);
            g2.drawString(slots[i].label.toUpperCase(), x + 18, y + 25);

            Equipment worn = gp.equipment.getEquipped(slots[i]);
            Color nameColor = worn == null ? (selected ? new Color(205, 175, 135) : emptyColor)
                    : (selected ? lightInk : ui.inkDark);
            drawFitted(ui, worn == null ? "- Empty -" : worn.name, x + 18, y + 50, w - 36, nameColor);
        }
    }

    private void drawGearList(UI ui, int x, int y0, int w, int headerY) {
        Graphics2D g2 = ui.g2;

        int n = choices.size();
        int first = Math.max(0, Math.min(pickIndex - 2, n - VISIBLE));
        int shown = Math.min(VISIBLE, n);

        for (int r = 0; r < shown; r++) {
            int idx = first + r;
            Equipment piece = choices.get(idx);
            int y = y0 + r * PITCH;
            boolean selected = idx == pickIndex;
            drawBox(g2, x, y, w, ROW_H, selected);

            boolean worn = gp.equipment.isEquipped(piece);
            int nameWidth = w - 36 - (worn ? 30 : 0);
            drawFitted(ui, piece == null ? "- Unequip -" : piece.name, x + 18, y + 28, nameWidth,
                    selected ? lightInk : ui.inkDark);

            g2.setFont(ui.fontSmall);
            g2.setColor(piece == null ? (selected ? new Color(205, 175, 135) : emptyColor)
                    : (selected ? lightInk : ui.inkDark));
            g2.drawString(piece == null ? "Take off current gear" : piece.bonusText(), x + 18, y + 51);

            if (worn) {
                // Small gold "E" badge: this piece is worn right now.
                int bx = x + w - 18 - 22, by = y + 8;
                g2.setColor(new Color(47, 27, 16, 235));
                g2.fillRoundRect(bx, by, 22, 20, 6, 6);
                g2.setColor(new Color(244, 210, 112));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(bx, by, 22, 20, 6, 6);
                g2.setFont(ui.fontSmall);
                g2.drawString("E", bx + (22 - g2.getFontMetrics().stringWidth("E")) / 2, by + 15);
            }
        }

        // Scroll hints when the list is longer than what fits.
        g2.setFont(ui.fontSmall);
        g2.setColor(ui.inkDark);
        String arrows = (first > 0 ? "^" : " ") + " " + (first + VISIBLE < n ? "v" : " ");
        g2.drawString(arrows, x + w - g2.getFontMetrics().stringWidth(arrows), headerY);
    }

    private void drawStats(UI ui, int x, int y0, int w, Slot slot, Equipment candidate) {
        Graphics2D g2 = ui.g2;

        Stat[] stats = Stat.values();
        int boxH = stats.length * 46 + 30;
        drawBox(g2, x, y0, w, boxH, false);

        int currentRight = x + 175;
        int newRight = x + w - 24;

        for (int i = 0; i < stats.length; i++) {
            Stat stat = stats[i];
            int y = y0 + 52 + i * 46;

            double current = gp.equipment.getTotal(stat);
            double next = picking ? gp.equipment.getTotalWith(stat, slot, candidate) : current;
            boolean changed = Math.abs(next - current) > 0.0001;

            g2.setFont(ui.fontMid);
            g2.setColor(ui.inkDark);
            g2.drawString(stat.label, x + 24, y);
            drawRight(g2, format(stat, current), currentRight, y);

            if (changed) {
                g2.setFont(ui.fontSmall);
                g2.setColor(ui.inkDark);
                g2.drawString("->", currentRight + 20, y);

                g2.setFont(ui.fontMid);
                g2.setColor(next > current ? goodColor : badColor);
                drawRight(g2, format(stat, next), newRight, y);
            }
        }
    }

    // ----- small helpers -----

    // A rounded box in the same style as the main menu's category list.
    private void drawBox(Graphics2D g2, int x, int y, int w, int h, boolean selected) {
        if (selected) {
            g2.setColor(new Color(92, 62, 34, 235));
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(new Color(244, 210, 112));
        } else {
            g2.setColor(new Color(255, 245, 211, 110));
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(new Color(92, 61, 34, 200));
        }
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, w, h, 12, 12);
    }

    private String format(Stat stat, double value) {
        if (stat == Stat.SPEED) return String.format(Locale.US, "%.1f", value);
        return String.valueOf((int) Math.round(value));
    }

    private void drawRight(Graphics2D g2, String text, int rightEdge, int y) {
        g2.drawString(text, rightEdge - g2.getFontMetrics().stringWidth(text), y);
    }

    // Draws text in the medium font, or the small font if it would not fit the width.
    private void drawFitted(UI ui, String text, int x, int y, int maxWidth, Color color) {
        Graphics2D g2 = ui.g2;
        Font font = ui.fontMid;
        FontMetrics fm = g2.getFontMetrics(font);
        if (fm.stringWidth(text) > maxWidth) {
            font = ui.fontSmall;
        }
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }
}