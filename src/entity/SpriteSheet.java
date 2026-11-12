package entity;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class SpriteSheet {
    private final BufferedImage[][] frames; // [row][col]

    public SpriteSheet(String path, int cellW, int cellH) throws IOException {
        BufferedImage sheet = ImageIO.read(getClass().getResource(path));
        System.out.println("Sheet size: " + sheet.getWidth() + "x" + sheet.getHeight());
        int rows = sheet.getHeight() / cellH;
        int cols = sheet.getWidth() / cellW;
        frames = new BufferedImage[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                frames[r][c] = sheet.getSubimage(c * cellW, r * cellH, cellW, cellH);
    }

    public BufferedImage get(int row, int col) { return frames[row][col]; }
}