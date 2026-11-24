package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Handles the active conversation and draws the dialogue textbox.
 * Press E to advance; the textbox closes after the final line.
 */
public class Dialogue {

    private final GamePanel gp;
    private Conversation conversation;
    private int lineIndex;
    private boolean active;

    private final Font speakerFont = new Font("Arial", Font.BOLD, 17);
    private final Font dialogueFont = new Font("Arial", Font.PLAIN, 16);

    public Dialogue(GamePanel gp) {
        this.gp = gp;
    }

    public boolean isActive() {
        return active;
    }

    public void start(Conversation conversation) {
        if (conversation == null || conversation.size() == 0) return;
        this.conversation = conversation;
        this.lineIndex = 0;
        this.active = true;
    }

    public void advance() {
        if (!active || conversation == null) return;
        lineIndex++;
        if (lineIndex >= conversation.size()) {
            active = false;
            conversation = null;
            lineIndex = 0;
        }
    }

    public void draw(Graphics2D g2) {
        if (!active || conversation == null) return;

        Conversation.Line line = conversation.getLine(lineIndex);
        int margin = 22;
        int boxX = margin;
        int boxY = gp.screenHeight - 116;
        int boxW = gp.screenWidth - margin * 2;
        int boxH = 94;

        Object oldAntialias = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Dark, layered textbox with a warm border.
        g2.setColor(new Color(12, 16, 25, 235));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);
        g2.setColor(new Color(225, 190, 125));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 14, 14);
        g2.setColor(new Color(255, 235, 190));
        g2.drawRoundRect(boxX + 4, boxY + 4, boxW - 8, boxH - 8, 10, 10);

        g2.setFont(speakerFont);
        g2.setColor(new Color(255, 211, 130));
        g2.drawString(line.speaker, boxX + 17, boxY + 25);

        g2.setFont(dialogueFont);
        g2.setColor(Color.WHITE);
        drawWrappedText(g2, line.text, boxX + 18, boxY + 51, boxW - 36, 20);

        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("E  Continue", boxX + boxW - 92, boxY + boxH - 12);

        if (oldAntialias != null) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAntialias);
        }
    }

    private void drawWrappedText(Graphics2D g2, String text, int x, int y,
                                 int maxWidth, int lineHeight) {
        FontMetrics metrics = g2.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        int row = 0;

        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && current.length() > 0) {
                g2.drawString(current.toString(), x, y + row * lineHeight);
                current.setLength(0);
                current.append(word);
                row++;
            } else {
                if (current.length() > 0) current.append(' ');
                current.append(word);
            }
        }
        if (current.length() > 0 && row < 2) {
            g2.drawString(current.toString(), x, y + row * lineHeight);
        }
    }
}
