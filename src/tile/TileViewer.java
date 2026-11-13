package tile;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Standalone tool (not part of the game).
// Shows any tileset PNG with every tile numbered, so you can pick numbers for your palette.
public class TileViewer extends JPanel {

    // Folder to scan for PNGs. Change this if your assets are somewhere else.
    // Relative to the project folder (where "res" lives).
    static final String START_FOLDER = "res/PostApocalypse AssetPack/Tiles";

    BufferedImage sheet;        // the tileset currently being shown
    int tileSize = 16;          // size of one tile in the PNG (pixels)
    int scale = 3;              // zoom factor
    int cols, rows;             // how many tiles across and down
    int hoverTile = -1;         // tile number under the mouse

    JLabel info = new JLabel(" ");   // status text at the bottom

    public TileViewer() {
        setBackground(Color.DARK_GRAY);

        // Show the tile number under the mouse in the status label
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                if (sheet == null) return;
                int c = e.getX() / (tileSize * scale);
                int r = e.getY() / (tileSize * scale);
                if (c >= 0 && c < cols && r >= 0 && r < rows) {
                    hoverTile = r * cols + c;
                    info.setText("Tile " + hoverTile + "   (row " + r + ", column " + c + ")");
                } else {
                    hoverTile = -1;
                    info.setText(" ");
                }
            }
        });
    }

    // Loads a PNG file and refreshes the display.
    public void loadImage(File file) {
        try {
            sheet = ImageIO.read(file);
            updateSize();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not read: " + file.getName());
        }
    }

    // Recalculates the grid and panel size (called after loading or changing tile size / zoom).
    public void updateSize() {
        if (sheet == null) return;
        cols = sheet.getWidth() / tileSize;
        rows = sheet.getHeight() / tileSize;
        setPreferredSize(new Dimension(cols * tileSize * scale, rows * tileSize * scale));
        revalidate();
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sheet == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * tileSize * scale, y = r * tileSize * scale;

                // Draw one tile, zoomed
                BufferedImage t = sheet.getSubimage(c * tileSize, r * tileSize, tileSize, tileSize);
                g2.drawImage(t, x, y, tileSize * scale, tileSize * scale, null);

                // Red outline
                g2.setColor(new Color(255, 0, 0, 120));
                g2.drawRect(x, y, tileSize * scale, tileSize * scale);

                // Yellow number with a dark shadow so it's readable on any tile
                String num = String.valueOf(r * cols + c);
                g2.setColor(Color.BLACK);
                g2.drawString(num, x + 3, y + 12);
                g2.setColor(Color.YELLOW);
                g2.drawString(num, x + 2, y + 11);
            }
        }
    }

    // Finds every PNG inside a folder (and its subfolders).
    static void findPngs(File folder, List<File> result) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) findPngs(f, result);
            else if (f.getName().toLowerCase().endsWith(".png")) result.add(f);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TileViewer viewer = new TileViewer();

            // ----- Top bar: dropdown, browse button, tile size, zoom -----
            JComboBox<File> dropdown = new JComboBox<>();
            dropdown.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                                                                        int index, boolean sel, boolean focus) {
                    // Show just the file name instead of the full path
                    return super.getListCellRendererComponent(list,
                            value == null ? "" : ((File) value).getName(), index, sel, focus);
                }
            });

            // Fill the dropdown with the PNGs from START_FOLDER
            List<File> pngs = new ArrayList<>();
            findPngs(new File(START_FOLDER), pngs);
            for (File f : pngs) dropdown.addItem(f);

            // Picking something in the dropdown loads that PNG
            dropdown.addActionListener(e -> {
                File f = (File) dropdown.getSelectedItem();
                if (f != null) viewer.loadImage(f);
            });

            // Browse button: pick any PNG from anywhere on your computer
            JButton browse = new JButton("Browse...");
            browse.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser(new File(START_FOLDER).exists()
                        ? new File(START_FOLDER) : new File("."));
                chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG images", "png"));
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    viewer.loadImage(chooser.getSelectedFile());
                }
            });

            // Tile size selector (16 normally, 32 for the character sheet)
            JComboBox<Integer> sizeBox = new JComboBox<>(new Integer[]{8, 16, 32, 48, 64});
            sizeBox.setSelectedItem(16);
            sizeBox.addActionListener(e -> {
                viewer.tileSize = (Integer) sizeBox.getSelectedItem();
                viewer.updateSize();
            });

            // Zoom selector
            JComboBox<Integer> zoomBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
            zoomBox.setSelectedItem(3);
            zoomBox.addActionListener(e -> {
                viewer.scale = (Integer) zoomBox.getSelectedItem();
                viewer.updateSize();
            });

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            top.add(new JLabel("Tileset:"));
            top.add(dropdown);
            top.add(browse);
            top.add(new JLabel("Tile size:"));
            top.add(sizeBox);
            top.add(new JLabel("Zoom:"));
            top.add(zoomBox);

            // ----- Window -----
            JFrame f = new JFrame("Tile viewer");
            f.setLayout(new BorderLayout());
            f.add(top, BorderLayout.NORTH);
            f.add(new JScrollPane(viewer), BorderLayout.CENTER);
            f.add(viewer.info, BorderLayout.SOUTH);
            f.setSize(1200, 800);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true);

            // Show the first PNG automatically
            if (dropdown.getItemCount() > 0) dropdown.setSelectedIndex(0);
        });
    }
}