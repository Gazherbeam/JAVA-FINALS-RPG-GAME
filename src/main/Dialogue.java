package main;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Handles the active conversation and draws the dialogue textbox.
 * Press E to advance; the textbox closes after the final line.
 */
public class Dialogue {

    private final GamePanel gp;
    private Conversation conversation;
    private int lineIndex;
    private boolean active;


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
        int boxH = 154;
        int boxY = gp.screenHeight - boxH - margin;
        int boxW = gp.screenWidth - margin * 2;

        g2.setColor(new Color(12, 16, 25, 235));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);
        g2.setColor(new Color(225, 190, 125));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 14, 14);
        g2.setColor(new Color(255, 235, 190));
        g2.drawRoundRect(boxX + 4, boxY + 4, boxW - 8, boxH - 8, 10, 10);

        g2.setFont(gp.ui.fontSpeaker);
        g2.setColor(new Color(255, 211, 130));
        g2.drawString(line.speaker, boxX + 20, boxY + 34);

        g2.setFont(gp.ui.fontDialogue);
        g2.setColor(Color.WHITE);
        drawWrappedText(g2, line.text, boxX + 20, boxY + 68, boxW - 40, 25);

        g2.setFont(gp.ui.fontSmall);
        g2.setColor(new Color(220, 220, 220));
        String hint = "E  Continue";
        int hintW = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, boxX + boxW - hintW - 20, boxY + boxH - 14);
    }

    private void drawWrappedText(Graphics2D g2, String text, int x, int y,
                                 int maxWidth, int lineHeight) {
        FontMetrics metrics = g2.getFontMetrics();
        StringBuilder current = new StringBuilder();
        int row = 0;

        for (String word : text.split("\\s+")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && current.length() > 0) {
                g2.drawString(current.toString(), x, y + row * lineHeight);
                current.setLength(0);
                if (++row == 3) return;
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        g2.drawString(current.toString(), x, y + row * lineHeight);
    }
}
