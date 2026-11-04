import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RpgSwingGame extends JPanel {
    private static final int VIEW_WIDTH = 960;
    private static final int VIEW_HEIGHT = 640;
    private static final int WORLD_WIDTH = 1600;
    private static final int WORLD_HEIGHT = 1000;
    private static final int TILE_SIZE = 48;
    private static final double PLAYER_SPEED = 230.0;

    private final BufferedImage playerSprite;
    private final BufferedImage[] swingFrames = new BufferedImage[8];
    private final List<Obstacle> obstacles = createObstacles();
    private final Timer timer;

    private double playerX = 440;
    private double playerY = 500;
    private double dummyX = 900;
    private double dummyY = 500;
    private double swingTime;
    private double swingCooldown;
    private double dummyFlashTime;
    private int dummyHealth = 3;
    private int hits;
    private boolean swingActive;
    private boolean swingResolved;
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private Facing facing = Facing.RIGHT;
    private long previousTick;

    public RpgSwingGame() {
        setPreferredSize(new Dimension(VIEW_WIDTH, VIEW_HEIGHT));
        setFocusable(true);
        setBackground(new Color(36, 68, 51));
        playerSprite = loadImage("Player Idle.png", false);
        for (int i = 0; i < swingFrames.length; i++) {
            swingFrames[i] = loadImage("SwingAttack" + (i + 1) + ".jpeg", true);
        }
        installControls();
        previousTick = System.nanoTime();
        timer = new Timer(16, this::updateGame);
        timer.start();
    }

    private BufferedImage loadImage(String fileName, boolean removeJpegBackground) {
        for (Path folder : assetFolders()) {
            Path file = folder.resolve(fileName);
            if (Files.isRegularFile(file)) {
                try {
                    BufferedImage image = ImageIO.read(file.toFile());
                    if (image != null) {
                        return removeJpegBackground ? removeDarkBackground(image) : image;
                    }
                } catch (IOException ignored) {
                    // The game can still run with its built-in pixel-art fallback.
                }
            }
        }
        return null;
    }

    private List<Path> assetFolders() {
        List<Path> folders = new ArrayList<>();
        String configured = System.getProperty("rpg.assets");
        if (configured != null && !configured.isBlank()) {
            folders.add(Path.of(configured));
        }
        folders.add(Path.of("Pixel Art"));
        folders.add(Path.of("..", "Pixel Art"));
        folders.add(Path.of(System.getProperty("user.home"), "Downloads", "Pixel Art"));
        return folders;
    }

    private BufferedImage removeDarkBackground(BufferedImage source) {
        BufferedImage cleaned = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int red = (argb >> 16) & 0xff;
                int green = (argb >> 8) & 0xff;
                int blue = argb & 0xff;
                int brightness = Math.max(red, Math.max(green, blue));
                int alpha = brightness < 34 ? 0 : 255;
                cleaned.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return cleaned;
    }

    private void installControls() {
        InputMap input = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = getActionMap();
        bind(input, actions, "pressed UP", "up-on", () -> up = true);
        bind(input, actions, "released UP", "up-off", () -> up = false);
        bind(input, actions, "pressed W", "w-on", () -> up = true);
        bind(input, actions, "released W", "w-off", () -> up = false);
        bind(input, actions, "pressed DOWN", "down-on", () -> down = true);
        bind(input, actions, "released DOWN", "down-off", () -> down = false);
        bind(input, actions, "pressed S", "s-on", () -> down = true);
        bind(input, actions, "released S", "s-off", () -> down = false);
        bind(input, actions, "pressed LEFT", "left-on", () -> left = true);
        bind(input, actions, "released LEFT", "left-off", () -> left = false);
        bind(input, actions, "pressed A", "a-on", () -> left = true);
        bind(input, actions, "released A", "a-off", () -> left = false);
        bind(input, actions, "pressed RIGHT", "right-on", () -> right = true);
        bind(input, actions, "released RIGHT", "right-off", () -> right = false);
        bind(input, actions, "pressed D", "d-on", () -> right = true);
        bind(input, actions, "released D", "d-off", () -> right = false);
        bind(input, actions, "pressed SPACE", "swing-space", this::startSwing);
        bind(input, actions, "pressed J", "swing-j", this::startSwing);
        bind(input, actions, "pressed E", "swing-e", this::startSwing);
    }

    private void bind(InputMap input, ActionMap actions, String key, String name, Runnable task) {
        input.put(KeyStroke.getKeyStroke(key), name);
        actions.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                task.run();
            }
        });
    }

    private void startSwing() {
        if (swingCooldown <= 0) {
            swingActive = true;
            swingResolved = false;
            swingTime = 0;
            swingCooldown = 0.42;
        }
    }

    private void updateGame(ActionEvent event) {
        long now = System.nanoTime();
        double delta = Math.min((now - previousTick) / 1_000_000_000.0, 0.05);
        previousTick = now;

        double dx = (right ? 1 : 0) - (left ? 1 : 0);
        double dy = (down ? 1 : 0) - (up ? 1 : 0);
        if (dx != 0 || dy != 0) {
            double length = Math.hypot(dx, dy);
            dx /= length;
            dy /= length;
            updateFacing(dx, dy);
            movePlayer(dx * PLAYER_SPEED * delta, dy * PLAYER_SPEED * delta);
        }

        if (swingCooldown > 0) swingCooldown -= delta;
        if (dummyFlashTime > 0) dummyFlashTime -= delta;
        if (swingActive) {
            swingTime += delta;
            if (!swingResolved && swingTime >= 0.13) {
                swingResolved = true;
                checkSwingHit();
            }
            if (swingTime >= 0.42) swingActive = false;
        }
        repaint();
    }

    private void updateFacing(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            facing = dx < 0 ? Facing.LEFT : Facing.RIGHT;
        } else {
            facing = dy < 0 ? Facing.UP : Facing.DOWN;
        }
    }

    private void movePlayer(double dx, double dy) {
        double nextX = clamp(playerX + dx, 20, WORLD_WIDTH - 20);
        if (!collides(nextX, playerY)) playerX = nextX;
        double nextY = clamp(playerY + dy, 20, WORLD_HEIGHT - 20);
        if (!collides(playerX, nextY)) playerY = nextY;
    }

    private boolean collides(double x, double y) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.bounds().contains(x, y)) return true;
        }
        return false;
    }

    private void checkSwingHit() {
        double offsetX = dummyX - playerX;
        double offsetY = dummyY - playerY;
        double distance = Math.hypot(offsetX, offsetY);
        double forward = switch (facing) {
            case RIGHT -> offsetX;
            case LEFT -> -offsetX;
            case DOWN -> offsetY;
            case UP -> -offsetY;
        };
        if (distance < 116 && forward > 0 && dummyHealth > 0) {
            dummyHealth--;
            hits++;
            dummyFlashTime = 0.18;
            if (dummyHealth == 0) {
                dummyHealth = 3;
                dummyFlashTime = 0.45;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double cameraX = clamp(playerX - getWidth() / 2.0, 0, WORLD_WIDTH - getWidth());
        double cameraY = clamp(playerY - getHeight() / 2.0, 0, WORLD_HEIGHT - getHeight());
        g.translate(-cameraX, -cameraY);
        drawGround(g, cameraX, cameraY);
        drawObstacles(g);
        drawDummy(g);
        drawPlayer(g);
        g.translate(cameraX, cameraY);
        drawHud(g);
        g.dispose();
    }

    private void drawGround(Graphics2D g, double cameraX, double cameraY) {
        int firstX = Math.max(0, (int) cameraX / TILE_SIZE);
        int firstY = Math.max(0, (int) cameraY / TILE_SIZE);
        int lastX = Math.min(WORLD_WIDTH / TILE_SIZE, (int) (cameraX + getWidth()) / TILE_SIZE + 1);
        int lastY = Math.min(WORLD_HEIGHT / TILE_SIZE, (int) (cameraY + getHeight()) / TILE_SIZE + 1);
        for (int tileY = firstY; tileY <= lastY; tileY++) {
            for (int tileX = firstX; tileX <= lastX; tileX++) {
                int pattern = Math.floorMod(tileX * 13 + tileY * 7, 5);
                Color grass = switch (pattern) {
                    case 0 -> new Color(66, 126, 72);
                    case 1 -> new Color(71, 133, 76);
                    case 2 -> new Color(76, 137, 79);
                    case 3 -> new Color(69, 128, 72);
                    default -> new Color(73, 132, 75);
                };
                int x = tileX * TILE_SIZE;
                int y = tileY * TILE_SIZE;
                g.setColor(grass);
                g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g.setColor(new Color(255, 255, 255, 14));
                g.fillRect(x + 7 + pattern * 4, y + 9, 3, 3);
                g.fillRect(x + 28, y + 30 + pattern % 3, 2, 2);
            }
        }
        g.setColor(new Color(42, 91, 56));
        g.setStroke(new BasicStroke(5));
        g.drawRect(2, 2, WORLD_WIDTH - 4, WORLD_HEIGHT - 4);
    }

    private void drawObstacles(Graphics2D g) {
        for (Obstacle obstacle : obstacles) {
            int x = obstacle.x;
            int y = obstacle.y;
            g.setColor(new Color(34, 64, 42, 100));
            g.fillOval(x - 5, y + 21, 76, 25);
            g.setColor(new Color(105, 66, 43));
            g.fillRect(x + 27, y + 17, 18, 30);
            g.setColor(new Color(91, 52, 37));
            g.fillRect(x + 31, y + 19, 5, 24);
            g.setColor(new Color(31, 92, 54));
            g.fillOval(x + 2, y - 11, 66, 49);
            g.setColor(new Color(43, 119, 62));
            g.fillOval(x + 11, y - 19, 49, 44);
            g.setColor(new Color(63, 143, 69));
            g.fillOval(x + 20, y - 14, 27, 24);
            g.setColor(new Color(105, 173, 83, 180));
            g.fillRect(x + 25, y - 9, 6, 4);
        }
    }

    private void drawDummy(Graphics2D g) {
        int x = (int) dummyX;
        int y = (int) dummyY;
        g.setColor(new Color(41, 63, 43, 120));
        g.fillOval(x - 30, y + 20, 64, 20);
        g.setColor(new Color(102, 67, 45));
        g.fillRect(x - 5, y - 8, 10, 39);
        g.setColor(new Color(139, 94, 58));
        g.fillRoundRect(x - 21, y - 31, 42, 33, 5, 5);
        g.setColor(new Color(177, 127, 76));
        g.fillRect(x - 16, y - 25, 32, 5);
        g.setColor(new Color(90, 59, 39));
        g.fillRect(x - 26, y - 19, 52, 5);
        g.setColor(new Color(75, 52, 39));
        g.fillRect(x - 25, y - 20, 5, 15);
        g.fillRect(x + 20, y - 20, 5, 15);
        g.setColor(new Color(193, 172, 117));
        g.fillRect(x - 18, y - 14, 36, 3);
        if (dummyFlashTime > 0) {
            g.setColor(new Color(255, 235, 146, 180));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x - 26, y - 35, 52, 48);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        g.setColor(new Color(32, 43, 34));
        g.drawString("TRAINING DUMMY", x - 53, y - 45);
        g.setColor(new Color(48, 37, 32));
        g.fillRoundRect(x - 24, y - 41, 48, 5, 4, 4);
        g.setColor(new Color(224, 112, 78));
        g.fillRoundRect(x - 24, y - 41, 48 * dummyHealth / 3, 5, 4, 4);
    }

    private void drawPlayer(Graphics2D g) {
        int x = (int) playerX;
        int y = (int) playerY;
        g.setColor(new Color(31, 56, 38, 110));
        g.fillOval(x - 18, y + 13, 36, 14);
        if (playerSprite != null) {
            int width = 48;
            int height = 76;
            AffineTransform transform = new AffineTransform();
            transform.translate(x, y + 16);
            if (facing == Facing.LEFT) {
                transform.scale(-1, 1);
            }
            transform.translate(-width / 2.0, -height);
            transform.scale((double) width / playerSprite.getWidth(), (double) height / playerSprite.getHeight());
            g.drawImage(playerSprite, transform, null);
        } else {
            g.setColor(new Color(41, 48, 94));
            g.fillRoundRect(x - 16, y - 25, 32, 39, 8, 8);
            g.setColor(new Color(231, 183, 132));
            g.fillOval(x - 12, y - 39, 24, 23);
            g.setColor(new Color(104, 57, 48));
            g.fillRect(x - 13, y - 39, 26, 7);
        }
        if (swingActive) drawSwing(g, x, y);
    }

    private void drawSwing(Graphics2D g, int x, int y) {
        int frame = Math.min(7, (int) (swingTime / 0.42 * swingFrames.length));
        BufferedImage image = swingFrames[frame];
        if (image != null) {
            int offsetX = facing == Facing.RIGHT ? 32 : facing == Facing.LEFT ? -32 : 0;
            int offsetY = facing == Facing.DOWN ? 22 : facing == Facing.UP ? -49 : -15;
            int width = 112;
            int height = 112;
            AffineTransform transform = new AffineTransform();
            transform.translate(x + offsetX, y + offsetY);
            double angle = switch (facing) {
                case RIGHT -> 0;
                case DOWN -> Math.PI / 2;
                case LEFT -> Math.PI;
                case UP -> -Math.PI / 2;
            };
            transform.rotate(angle);
            transform.translate(-width / 2.0, -height / 2.0);
            transform.scale((double) width / image.getWidth(), (double) height / image.getHeight());
            g.drawImage(image, transform, null);
        } else {
            g.setColor(new Color(240, 232, 173, 200));
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int direction = facing == Facing.LEFT ? -1 : 1;
            g.drawArc(x - 44 + (direction < 0 ? -22 : 0), y - 55, 88, 82,
                    facing == Facing.LEFT ? 215 : 10, facing == Facing.LEFT ? 130 : 130);
        }
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(23, 35, 31, 220));
        g.fillRoundRect(18, 16, 344, 76, 16, 16);
        g.setColor(new Color(234, 226, 190));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("GREENWOOD TRAINING GROUNDS", 34, 43);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString("Hits: " + hits + "     Dummy: " + dummyHealth + "/3", 34, 69);

        g.setColor(new Color(23, 35, 31, 210));
        g.fillRoundRect(18, getHeight() - 54, 564, 36, 12, 12);
        g.setColor(new Color(239, 236, 213));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString("Move: WASD / Arrows      Swing: E / SPACE / J      Face the dummy and get close!", 32, getHeight() - 31);
    }

    private static List<Obstacle> createObstacles() {
        List<Obstacle> result = new ArrayList<>();
        result.add(new Obstacle(190, 210, 62, 42));
        result.add(new Obstacle(360, 330, 62, 42));
        result.add(new Obstacle(620, 170, 62, 42));
        result.add(new Obstacle(710, 640, 62, 42));
        result.add(new Obstacle(1110, 280, 62, 42));
        result.add(new Obstacle(1280, 540, 62, 42));
        result.add(new Obstacle(1390, 170, 62, 42));
        return result;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Obstacle(int x, int y, int width, int height) {
        java.awt.Rectangle bounds() {
            return new java.awt.Rectangle(x + 8, y + 7, width - 16, height + 12);
        }
    }

    private enum Facing { UP, DOWN, LEFT, RIGHT }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Greenwood - Pixel RPG Practice");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(false);
            window.add(new RpgSwingGame());
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}