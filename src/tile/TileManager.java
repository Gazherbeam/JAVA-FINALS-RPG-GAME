package tile;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

// Loads tilesets, reads the map from a text file, and draws the map.
public class TileManager {

    GamePanel gp;
    int[][] map;                       // map[row][col] = a number from map01.txt
    public int worldCols, worldRows;   // map size in tiles, detected from the map file

    // Nickname -> list of 16x16 tiles from that PNG.
    java.util.Map<String, java.util.List<BufferedImage>> tilesets = new java.util.HashMap<>();

    // Position in this array = number written in map01.txt.
    BufferedImage[] palette;

    public TileManager(GamePanel gp) {
        this.gp = gp;

        // ===== TILESET NICKNAMES =====
        // add("nickname", "path to the PNG")
        String folder = "/PostApocalypse AssetPack/Tiles/";
        add("BG green",      folder + "Background_Green_TileSet.png");
        add("BG dark green", folder + "Background_Dark-Green_TileSet.png");
        add("BG yellow",     folder + "Background_Bleak-Yellow_TileSet.png");
        add("brick wall",    folder + "Brick-Wall_TileSet.png");
        add("grass top",     folder + "Grass-On-Top_TileSet.png");

        // ===== PALETTE =====
        // tile("nickname", tile number from the TileViewer)
        palette = new BufferedImage[] {
                tile("BG green", 25),      // map number 0
                tile("brick wall", 5),     // map number 1
                tile("BG green", 52)       // map number 2
        };

        loadMap("/maps/map01.txt");
    }

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
            e.printStackTrace();
        }
        tilesets.put(nickname, list);
    }

    // Gets one tile by nickname and number. Example: tile("BG green", 25)
    private BufferedImage tile(String nickname, int number) {
        java.util.List<BufferedImage> list = tilesets.get(nickname);
        if (list == null) throw new RuntimeException("No tileset named '" + nickname + "'");
        return list.get(number);
    }

    // Reads map01.txt. The map can be any size.
    private void loadMap(String path) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)));

            java.util.List<String[]> lines = new java.util.ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim().split("\\s+"));
                }
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

    // Draws only the part of the world the camera can see.
    public void draw(Graphics2D g2) {
        for (int worldRow = 0; worldRow < worldRows; worldRow++) {
            for (int worldCol = 0; worldCol < worldCols; worldCol++) {

                int worldX = worldCol * gp.tileSize;
                int worldY = worldRow * gp.tileSize;

                int screenX = worldX - gp.Player.x + gp.Player.screenX;
                int screenY = worldY - gp.Player.y + gp.Player.screenY;

                if (screenX + gp.tileSize > 0 && screenX < gp.screenWidth &&
                        screenY + gp.tileSize > 0 && screenY < gp.screenHeight) {

                    BufferedImage img = palette[map[worldRow][worldCol]];
                    g2.drawImage(img, screenX, screenY, gp.tileSize, gp.tileSize, null);
                }
            }
        }
    }
}