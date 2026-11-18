package entity;

import java.awt.image.BufferedImage;

public class Animation {
    private final BufferedImage[] frames;
    private final long frameTimeMs;
    private final boolean loop;          // true = repeats, false = plays once
    private long lastSwitch = System.currentTimeMillis();
    private int index = 0;
    private boolean finished = false;

    public Animation(BufferedImage[] frames, long frameTimeMs) {
        this(frames, frameTimeMs, true);
    }

    public Animation(BufferedImage[] frames, long frameTimeMs, boolean loop) {
        this.frames = frames;
        this.frameTimeMs = frameTimeMs;
        this.loop = loop;
    }

    // Call this ONCE per game update (from player.update()). It moves to the next frame when it's time.
    public void tick() {
        if (finished) return;
        long now = System.currentTimeMillis();
        if (now - lastSwitch >= frameTimeMs) {
            lastSwitch = now;
            if (index == frames.length - 1) {
                if (loop) index = 0;
                else finished = true;     // the last frame has been shown for its full time
            } else {
                index++;
            }
        }
    }

    // Only returns the current frame. It no longer changes anything.
    public BufferedImage current() { return frames[index]; }

    public boolean isFinished() { return finished; }

    public void reset() {
        index = 0;
        finished = false;
        lastSwitch = System.currentTimeMillis();
    }

    public BufferedImage first() { return frames[0]; }
}