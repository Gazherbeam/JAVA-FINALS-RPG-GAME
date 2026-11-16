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

        // ===== TILESET NICKNAMES =====  add("nickname", "path to the PNG")
        String folder = "/PostApocalypse AssetPack/Tiles/";
        add("BG green",      folder + "Background_Green_TileSet.png");
        add("BG dark green", folder + "Background_Dark-Green_TileSet.png");
        add("BG yellow",     folder + "Background_Bleak-Yellow_TileSet.png");
        add("brick wall",    folder + "Brick-Wall_TileSet.png");
        add("grass top",     folder + "Grass_On-Top_TileSet.png");

        // ===== PALETTE =====
        // Position in this list = number you write in map01.txt.
        //   tile("nickname", number)   walkable tile
        //   solid("nickname", number)  solid tile: the player and enemies can't walk through it
        // Copy your own palette lines here if you changed them.
        palette = new Tile[] {
                tile("BG green", 25),      // 0  walkable
                solid("brick wall", 5),    // 1  SOLID (the wall border)
                tile("BG green", 52)       // 2  walkable
        };

        loadMap("/maps/village01.txt");
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
