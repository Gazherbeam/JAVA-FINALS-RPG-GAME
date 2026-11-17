package entity;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

// The visual for the big swing: all nine 48x48 frames (0 to 8), played in order, once.
// It is always drawn on the player's body and follows the player. The art faces RIGHT,
// so it is rotated for the other directions.
public class SlashEffect {

    // =====================================================================
    // ===== EDIT ME ========================================================
    // =====================================================================

    private static final String SHEET = "/particles/original_slash_sprites_48x48.png";
    private static final int  CELL        = 48;    // size of one frame in the PNG
    private static final int  FRAME_COUNT = 9;     // frames 0 to 8
    private static final long FRAME_MS    = 60;    // time per frame (lower = faster)
    private static final int  SCALE       = 3;     // base size: 48 px -> 144 px on screen

    // true  = the crescent turns to match the facing direction
    // false = always drawn the same way
    private static final boolean ROTATE_WITH_FACING = true;

    // The spot inside a 48x48 frame that is pinned to the player.
    // 24, 24 = the middle of the frame. (The visible crescent's middle is about 23, 20.)
    private static final double PIVOT_X = 24, PIVOT_Y = 24;

    // One line per facing direction:
    //   offX, offY = move the effect (screen pixels) from the anchor point. 0, 0 = dead center.
    //   along      = stretch ALONG the facing direction (1.0 = normal, 1.5 = 50% longer)
    //   across     = stretch ACROSS the facing direction, i.e. thickness (1.0 = normal)
    // The anchor point is the middle of the player's body (ANCHOR_Y_SHIFT moves it up/down).
    //
    //                                          offX  offY  along across
    private static final Look LOOK_UP    = new Look(0,   0,  1.0,  1.4);
    private static final Look LOOK_DOWN  = new Look(0,   0,  1.0,  1.4);
    private static final Look LOOK_LEFT  = new Look(0,   0,  1.4,  1.0);
    private static final Look LOOK_RIGHT = new Look(0,   0,  1.4,  1.0);

    // The player's body center ("hitbox center") is down at the feet. This lifts the anchor
    // to the middle of the visible body. Set to 0 to anchor on the feet box instead.
    private static final int ANCHOR_Y_SHIFT = -14;

    // =====================================================================
    // ===== Code below does not need editing ===============================
    // =====================================================================

    private static final class Look {
        final int offX, offY;
        final double along, across;
        Look(int offX, int offY, double along, double across) {
            this.offX = offX; this.offY = offY; this.along = along; this.across = across;
        }
    }

    private static final BufferedImage[] FRAMES = load();

    private final Animation anim = FRAMES == null ? null : new Animation(FRAMES, FRAME_MS, false);
    private boolean active = false;
    private int fallbackTicks = 0;               // used only when the sheet is missing
    private int centerOffsetX, centerOffsetY;    // extra offset set from code (see setCenterOffset)
    private int scale = SCALE;

    private static BufferedImage[] load() {
        try {
            URL url = SlashEffect.class.getResource(SHEET);
            if (url == null) throw new IOException("Missing resource " + SHEET);
            BufferedImage sheet = ImageIO.read(url);
            int cols = sheet.getWidth() / CELL;
            BufferedImage[] frames = new BufferedImage[FRAME_COUNT];
            for (int i = 0; i < FRAME_COUNT; i++)
                frames[i] = sheet.getSubimage((i % cols) * CELL, (i / cols) * CELL, CELL, CELL);
            return frames;                       // frame 0 through 8, left-to-right, top-to-bottom
        } catch (IOException | RuntimeException e) {
            System.err.println("SlashEffect: using fallback arc (" + e.getMessage() + ")");
            return null;
        }
    }

    public void start() {
        active = true;
        fallbackTicks = 0;
        if (anim != null) anim.reset();
    }

    // Call once per update (the effect keeps playing a moment after the dash ends)
    public void tick() {
        if (!active) return;
        if (anim != null) {
            anim.tick();
            if (anim.isFinished()) active = false;
        } else if (++fallbackTicks > 16) {
            active = false;
        }
    }

    public boolean isActive() { return active; }

    // Optional extra offset from code, on top of the per-direction offsets above.
    public void setCenterOffset(int x, int y) {
        centerOffsetX = x;
        centerOffsetY = y;
    }

    // Optional size multiplier from code (3 = 144 px frame).
    public void setScale(int scale) {
        if (scale < 1) throw new IllegalArgumentException("Scale must be at least 1");
        this.scale = scale;
    }

    private static Look lookFor(String direction) {
        switch (direction) {
            case "up":    return LOOK_UP;
            case "left":  return LOOK_LEFT;
            case "right": return LOOK_RIGHT;
            default:      return LOOK_DOWN;
        }
    }

    // cx, cy = the player's body center ON SCREEN
    public void draw(Graphics2D g2, int cx, int cy, String direction) {
        if (!active) return;
        double angle;
        switch (direction) {
            case "up":    angle = -90; break;
            case "left":  angle = 180; break;
            case "right": angle = 0;   break;
            default:      angle = 90;  break;
        }
        if (!ROTATE_WITH_FACING) angle = 0;

        Look look = lookFor(direction);
        int px = cx + look.offX + centerOffsetX;
        int py = cy + ANCHOR_Y_SHIFT + look.offY + centerOffsetY;

        if (anim == null) {                      // fallback: a plain white ring
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(6));
            g2.drawArc(px - 60, py - 60, 120, 120, 0, 360);
            g2.setStroke(new BasicStroke(1));
            return;
        }
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Order matters: the stretch is applied BEFORE the rotation, so "along" always follows
        // the facing direction and "across" is always the thickness.
        AffineTransform t = new AffineTransform();
        t.translate(px, py);
        t.rotate(Math.toRadians(angle));
        t.scale(scale * look.along, scale * look.across);
        t.translate(-PIVOT_X, -PIVOT_Y);
        g2.drawImage(anim.current(), t, null);
    }
}