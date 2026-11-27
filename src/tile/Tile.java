package tile;

import java.awt.image.BufferedImage;

// One kind of tile in the palette: its picture and whether it blocks movement.
public class Tile {

    public BufferedImage image;
    public boolean collision = false;   // true = solid (walls, water, fences...)

    public Tile() {}

    public Tile(BufferedImage image, boolean collision) {
        this.image = image;
        this.collision = collision;
    }
}
