package entity;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// A trail of fading, whitened copies of a sprite (the dash after-images).
//   add()   drops a ghost at a world position
//   tick()  ages them (call once per update; they keep fading after the dash ends)
//   draw()  draws them (call BEFORE drawing the real sprite so they sit behind it)
public class AfterImages {

    private static class Ghost {
        final BufferedImage img;
        final int x, y;      // world position of the sprite's top-left corner
        int age = 0;
        Ghost(BufferedImage img, int x, int y) { this.img = img; this.x = x; this.y = y; }
    }

    // update() runs on the game thread and draw() on the Swing thread, so use thread-safe collections
    private final List<Ghost> ghosts = new CopyOnWriteArrayList<>();
    private static final Map<BufferedImage, BufferedImage> WHITE_CACHE = new ConcurrentHashMap<>();

    private final int lifetime;        // ticks a ghost lasts
    private final float startAlpha;    // opacity of a brand new ghost (0..1)

    public AfterImages(int lifetime, float startAlpha) {
        this.lifetime = lifetime;
        this.startAlpha = startAlpha;
    }

    public void add(BufferedImage frame, int worldX, int worldY) {
        if (frame == null) return;
        ghosts.add(new Ghost(whiten(frame), worldX, worldY));
    }

    public void tick() {
        for (Ghost g : ghosts) g.age++;
        ghosts.removeIf(g -> g.age >= lifetime);
    }

    public void clear() { ghosts.clear(); }

    public void draw(Graphics2D g2, int camX, int camY, int size) {
        if (ghosts.isEmpty()) return;
        Composite old = g2.getComposite();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (Ghost g : ghosts) {
            float alpha = startAlpha * (1f - g.age / (float) lifetime);   // fades out over its life
            if (alpha <= 0f) continue;
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, alpha)));
            g2.drawImage(g.img, g.x - camX, g.y - camY, size, size, null);
        }
        g2.setComposite(old);
    }

    // Pushes every visible pixel most of the way to white (keeps the transparency).
    // Cached per source frame, so each frame is only converted once.
    private static BufferedImage whiten(BufferedImage src) {
        BufferedImage cached = WHITE_CACHE.get(src);
        if (cached != null) return cached;

        final double mix = 0.7;   // 0 = original colors, 1 = pure white
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < src.getHeight(); py++) {
            for (int px = 0; px < src.getWidth(); px++) {
                int argb = src.getRGB(px, py);
                int a = argb >>> 24;
                if (a == 0) continue;
                int r = (int) (((argb >> 16) & 255) * (1 - mix) + 255 * mix);
                int g = (int) (((argb >> 8) & 255) * (1 - mix) + 255 * mix);
                int b = (int) ((argb & 255) * (1 - mix) + 255 * mix);
                out.setRGB(px, py, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        WHITE_CACHE.put(src, out);
        return out;
    }
}
