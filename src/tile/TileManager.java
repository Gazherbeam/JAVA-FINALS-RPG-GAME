package tile;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

// Loads the tileset, reads the map from a text file, and draws the map.
// GamePanel creates one of these and calls draw() every frame.
public class TileManager {

    GamePanel gp;                 // reference to GamePanel (for tileSize and screen size)
    BufferedImage[] tileImages;   // every 16x16 piece of the tileset, stored in one list
    int[][] map;                  // the map: map[row][col] = a number from map01.txt

    // Translates map file numbers into tileset numbers.
    // Position in this list = number in map01.txt, value = tile number from the TileViewer.
    // 0 in map01.txt -> draws tile 25, 1 -> tile 40, 2 -> tile 52.
    int[] palette = {25, 40, 52};

    // Runs once when the game starts.
    public TileManager(GamePanel gp) {
        this.gp = gp;
        loadTileset("/PostApocalypse AssetPack/Tiles/Background_Green_TileSet.png"); // cut up the tileset
        loadMap("/maps/map01.txt");                                                  // read the map layout
    }

    // Reads the tileset PNG and slices it into 16x16 tiles.
    // Tile numbers count left to right, then top to bottom (same as the TileViewer).
    private void loadTileset(String path) {
        try {
            BufferedImage sheet = ImageIO.read(getClass().getResourceAsStream(path)); // load the PNG
            int cols = sheet.getWidth() / 16;    // tiles per row
            int rows = sheet.getHeight() / 16;   // tiles per column
            tileImages = new BufferedImage[cols * rows];   // room for every tile
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    // Cut tile at (row r, column c) and store it at index r * cols + c
                    tileImages[r * cols + c] = sheet.getSubimage(c * 16, r * 16, 16, 16);
        } catch (IOException e) {
            e.printStackTrace();   // prints the error if the file can't be read
        }
    }

    // Reads map01.txt and fills the map array with its numbers.
    private void loadMap(String path) {
        map = new int[gp.maxScreenRow][gp.maxScreenCol];   // 16 rows x 16 columns
        try {
            // Open the text file so we can read it line by line
            BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)));
            for (int row = 0; row < gp.maxScreenRow; row++) {
                // Read one line, e.g. "1 1 1 0 0 2" and split it into separate numbers
                String[] nums = br.readLine().trim().split(" ");
                for (int col = 0; col < gp.maxScreenCol; col++) {
                    // Convert text "1" to the number 1 and store it
                    map[row][col] = Integer.parseInt(nums[col]);
                }
            }
            br.close();   // done with the file
        } catch (Exception e) {
            e.printStackTrace();   // prints the error if the file is missing or badly formatted
        }
    }

    // Called every frame by GamePanel. Draws the whole map on the screen.
    public void draw(Graphics2D g2) {
        for (int row = 0; row < gp.maxScreenRow; row++)
            for (int col = 0; col < gp.maxScreenCol; col++) {
                // map[row][col]   -> the number in map01.txt for this spot
                // palette[...]    -> converts it to a tileset tile number
                // tileImages[...] -> the actual 16x16 picture for that tile
                BufferedImage img = tileImages[palette[map[row][col]]];

                // Draw it at its grid position, scaled to tileSize (48x48)
                g2.drawImage(img, col * gp.tileSize, row * gp.tileSize, gp.tileSize, gp.tileSize, null);
            }
    }
}