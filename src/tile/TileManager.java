package tile;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

// Loads tilesets, reads the map from a text file, draws the visible part of the map,
// and answers "is this area solid?" for collision (used by the player, enemies, projectiles).
public class TileManager {

    GamePanel gp;
    int[][] map;                       // map[row][col] = palette number from map01.txt
    public int worldCols, worldRows;   // map size in tiles, detected from the map file

    // Nickname -> list of 16x16 tiles from that PNG.
    java.util.Map<String, java.util.List<BufferedImage>> tilesets = new java.util.HashMap<>();

    // Position in this array = number written in map01.txt.
    public Tile[] palette;

    public TileManager(GamePanel gp) {
        this.gp = gp;

    // ===== TILESET NICKNAMES =====
        String folder = "/PostApocalypse AssetPack/Tiles/";
        add("BG green",       folder + "Background_Green_TileSet.png");
        add("BG yellow",       folder + "Background_Bleak-Yellow_TileSet.png");
        add("brick wall",      folder + "Brick-Wall_TileSet.png");
        add("garbage",         folder + "Garbage_TileSet.png");
        add("wire fence",      folder + "Wire-Fence/Wire-Fence_TileSet.png");


    // ===== PALETTE =====
        palette = new Tile[] {
                tile("BG green", 25),       // 0: walkable
                solid("brick wall", 5),     // 1: solid wall border
                tile("BG green", 52),       // 2: walkable

                tile("BG yellow", 52),       // 3: plain dirt ground
                tile("BG yellow", 3),        // 4: cracked dirt
                tile("BG yellow", 27),       // 5: dirt with yellow bits
                solid("brick wall", 1),      // 6: broken wall top
                solid("brick wall", 2),      // 7: broken wall top
                solid("brick wall", 3),      // 8: broken wall top
                solid("brick wall", 4),      // 9: mossy wall top
                solid("brick wall", 9),      // 10: wall body (second row)
                solid("brick wall", 6),      // 11: left side wall
                solid("brick wall", 11),     // 12: right side wall
                solid("brick wall", 16),     // 13: bottom wall
                solid("wire fence", 6),      // 14: fence (plain)
                solid("wire fence", 14),     // 15: fence with post
                solid("wire fence", 13),     // 16: fence with trash stuck in it
                solid("wire fence", 0),      // 17: fence with a bag stuck in it

                // ----- BIG PILE pieces -----
                tile("garbage", 0),          // 18: top-left corner
                tile("garbage", 1),          // 19: top edge
                tile("garbage", 2),          // 20: top edge
                tile("garbage", 3),          // 21: top edge
                tile("garbage", 4),          // 22: top-right corner
                tile("garbage", 16),         // 23: left edge
                tile("garbage", 8),          // 24: left edge with red barrel
                tile("garbage", 20),         // 25: right edge
                tile("garbage", 12),         // 26: right edge
                tile("garbage", 24),         // 27: bottom-left corner
                tile("garbage", 25),         // 28: bottom edge, dumpster (left half)
                tile("garbage", 26),         // 29: bottom edge, dumpster (right half)
                tile("garbage", 27),         // 30: bottom edge
                tile("garbage", 28),         // 31: bottom-right corner
                tile("garbage", 9),          // 32: inside filling
                tile("garbage", 11),         // 33: inside filling
                tile("garbage", 17),         // 34: inside filling (dark bags)
                tile("garbage", 18),         // 35: inside filling (red)
                tile("garbage", 10),         // 36: inside filling (brown box)
                tile("garbage", 19),         // 37: inside filling (tire, not used yet)

                // ----- SMALL PILE pieces -----
                tile("garbage", 5),          // 38: top-left corner
                tile("garbage", 6),          // 39: top edge
                tile("garbage", 7),          // 40: top-right corner
                tile("garbage", 13),         // 41: left edge
                tile("garbage", 14),         // 42: middle
                tile("garbage", 15),         // 43: right edge with red barrel
                tile("garbage", 21),         // 44: bottom-left corner
                tile("garbage", 22),         // 45: bottom edge
                tile("garbage", 23)          // 46: bottom-right corner
        };

        // ################################################################
        // ##### ADDED (NEW CODE #1): FIX THE BLACK EDGES              #####
        // ##### This block is INSIDE the constructor, after the       #####
        // ##### palette and BEFORE loadMap(...).                      #####
        // ################################################################
        BufferedImage ground = palette[3].image;
        for (int i = 14; i < palette.length; i++) {      // fence and garbage pieces
            palette[i].image = onTop(palette[i].image, ground);
        }
        Color SKY = new Color(196, 178, 60);             // dusty yellow (use Color.BLACK for black)
        BufferedImage skyTile = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = skyTile.createGraphics();
        sg.setColor(SKY);
        sg.fillRect(0, 0, 16, 16);
        sg.dispose();
        for (int i = 6; i <= 9; i++) {                   // the broken wall tiles on the top row
            palette[i].image = onTop(palette[i].image, skyTile);
        }

        loadMap("/maps/Landfill.txt"); // Loads the maps
    }

    private BufferedImage onTop(BufferedImage foreground, BufferedImage background) {
        BufferedImage result = new BufferedImage(
                background.getWidth(), background.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(background, 0, 0, null);
        g2.drawImage(foreground, 0, 0, null);
        g2.dispose();
        return result;
    }

    // ===== LOADING =====

    // Loads a PNG, slices it into 16x16 tiles and stores it under a nickname.
    private void add(String nickname, String path) {
        java.util.List<BufferedImage> list = new java.util.ArrayList<>();
        try {
            BufferedImage sheet = ImageIO.read(getClass().getResourceAsStream(path));
            int cols = sheet.getWidth() / 16;
            int rows = sheet.getHeight() / 16;
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    list.add(sheet.getSubimage(c * 16, r * 16, 16, 16));
        } catch (Exception e) {
            System.out.println("Could not load tileset '" + nickname + "' from " + path);
        }
        tilesets.put(nickname, list);
    }

    private BufferedImage image(String nickname, int number) {
        java.util.List<BufferedImage> list = tilesets.get(nickname);
        if (list == null) throw new RuntimeException("No tileset named '" + nickname + "'");
        return list.get(number);
    }

    private Tile tile(String nickname, int number)  { return new Tile(image(nickname, number), false); }
    private Tile solid(String nickname, int number) { return new Tile(image(nickname, number), true); }

    // Reads map01.txt. The map can be any size.
    private void loadMap(String path) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)));
            java.util.List<String[]> lines = new java.util.ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line.trim().split("\\s+"));
            }
            br.close();

            worldRows = lines.size();
            worldCols = lines.get(0).length;
            map = new int[worldRows][worldCols];
            for (int row = 0; row < worldRows; row++)
                for (int col = 0; col < worldCols; col++)
                    map[row][col] = Integer.parseInt(lines.get(row)[col]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== COLLISION =====

    public int worldWidthPx()  { return worldCols * gp.tileSize; }
    public int worldHeightPx() { return worldRows * gp.tileSize; }

    // Is this one tile solid? Anything outside the map counts as solid.
    public boolean isSolidTile(int col, int row) {
        if (map == null || col < 0 || row < 0 || col >= worldCols || row >= worldRows) return true;
        int n = map[row][col];
        if (n < 0 || n >= palette.length) return false;
        return palette[n].collision;
    }

    // Is any part of this rectangle (in world pixels) touching a solid tile?
    public boolean isSolid(int px, int py, int w, int h) {
        if (px < 0 || py < 0) return true;
        int ts = gp.tileSize;
        int c1 = px / ts, c2 = (px + w - 1) / ts;
        int r1 = py / ts, r2 = (py + h - 1) / ts;
        for (int r = r1; r <= r2; r++)
            for (int c = c1; c <= c2; c++)
                if (isSolidTile(c, r)) return true;
        return false;
    }

    // ===== DRAWING =====

    // Draws only the tiles the camera can see.
    public void draw(Graphics2D g2) {
        if (map == null) return;
        int ts = gp.tileSize;
        int camX = gp.Player.camX, camY = gp.Player.camY;

        int startCol = Math.max(0, camX / ts);
        int endCol   = Math.min(worldCols - 1, (camX + gp.screenWidth) / ts);
        int startRow = Math.max(0, camY / ts);
        int endRow   = Math.min(worldRows - 1, (camY + gp.screenHeight) / ts);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int n = map[row][col];
                if (n < 0 || n >= palette.length) continue;   // -1 = draw nothing
                g2.drawImage(palette[n].image, col * ts - camX, row * ts - camY, ts, ts, null);
            }
        }
    }
}
