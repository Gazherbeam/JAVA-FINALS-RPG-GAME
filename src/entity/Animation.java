package entity;

import java.awt.image.BufferedImage;

public class Animation {
    private final BufferedImage[] frames;
    private final long frameTimeMs;
    private long lastSwitch = System.currentTimeMillis();
    private int index = 0;

    public Animation(BufferedImage[] frames, long frameTimeMs) {
        this.frames = frames;
        this.frameTimeMs = frameTimeMs;
    }

    public BufferedImage current() {
        long now = System.currentTimeMillis();
        if (now - lastSwitch >= frameTimeMs) {
            index = (index + 1) % frames.length;
            lastSwitch = now;
        }
        return frames[index];
    }

    public void reset() { index = 0; }
    public BufferedImage first() { return frames[0]; }
}