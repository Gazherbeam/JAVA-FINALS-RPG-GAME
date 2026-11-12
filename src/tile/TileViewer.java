package tile;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
// A small standalone tool (not part of the game).
// It displays the whole tileset with each 16x16 tile numbered,
// so you can find which number is grass, wall, path, etc.
public class TileViewer extends JPanel {
    BufferedImage sheet; //the fullest image
    int cols, rows, scale = 3; //how many 16x16 fit across and down, scale = zoom factor so the tiles are easier to see

    // Loads the tileset PNG and works out the size of the window area.
    public TileViewer(String path) throws Exception {
        sheet = ImageIO.read(getClass().getResourceAsStream(path)); // read the PNG for the res folder
        cols = sheet.getWidth() / 16; // tiles per row = image width / 16
        rows = sheet.getHeight() / 16; // tiles per column = image height / 16
        setPreferredSize(new Dimension(cols * 16 * scale, rows * 16 * scale));  // panel size after zoom
        setBackground(Color.DARK_GRAY); // background behind transparent tiles
    }

    // Swing calls this automatically whenever the panel needs to be drawn.
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);    // clear the panel first
        Graphics2D g2 = (Graphics2D) g;

        // Keeps pixel art sharp when zoomed instead of blurring it
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setFont(new Font("Monospaced", Font.BOLD, 10)); // font for the tile numbers

        // Go through every tile, row by row
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                // Screen position of this tile (zoomed)
                int x = c * 16 * scale, y = r * 16 * scale;
                // Cut one 16x16 tile out of the big image
                BufferedImage t = sheet.getSubimage(c * 16, r * 16, 16, 16);

                // Draw the tile zoomed up
                g2.drawImage(t, x, y, 16 * scale, 16 * scale, null);

                // Red outline so you can see where each tile starts and ends
                g2.setColor(new Color(255, 0, 0, 120));
                g2.drawRect(x, y, 16 * scale, 16 * scale);

                // Yellow tile number. This is the number you put in the palette.
                // Formula: row * tiles-per-row + column
                g2.setColor(Color.YELLOW);
                g2.drawString(String.valueOf(r * cols + c), x + 2, y + 11);
            }
    }

    // Runs when you right-click the class and choose "Run TileViewer.main()".
    // Opens a window showing the numbered tileset.
    public static void main(String[] args) throws Exception {
        // Change this path to view a different tileset
        JFrame f = new JFrame("Tile viewer");
        f.add(new JScrollPane(new TileViewer("/PostApocalypse AssetPack/Tiles/Background_Green_TileSet.png")));
        f.setSize(1200, 800);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}

