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
    int[][] map;                       // map[row][col] = palette number from the map file
    public int worldCols, worldRows;   // map size in tiles, detected from the map file
    public String currentMapPath = "/maps/Landfill.txt";

    // Nickname -> list of 16x16 tiles from that PNG.
    java.util.Map<String, java.util.List<BufferedImage>> tilesets = new java.util.HashMap<>();

    // Position in this array = number written in the map file.
    public Tile[] palette;

    // Burning barrels (Landfill map)
    BufferedImage[] barrelFrames = new BufferedImage[4];
    java.util.List<int[]> barrels = new java.util.ArrayList<>();
    boolean barrelsAreSolid = true;
    final String BARREL_MAP = "/maps/Landfill.txt";

    // Props (houses from spr_old_building_*.png + pieces from ruined1.png)
    static class Prop {
        BufferedImage img;
        int col, row;               // top-left corner, in tiles
        boolean[][] solidCells;     // [row][col] in 16x16 pieces of the picture
        boolean townOnly;
        boolean swampOnly;
    }
    java.util.List<Prop> props = new java.util.ArrayList<>();
    final String TOWN_MAP = "/maps/RuinedTown.txt";
    final String SWAMP_MAP = "/maps/TheSwamp.txt";
    BufferedImage ruinedSheet;

    public TileManager(GamePanel gp) {
        this.gp = gp;

        // ===== TILESET NICKNAMES =====
        String folder = "/PostApocalypse AssetPack/Tiles/";
        add("BG green",        folder + "Background_Green_TileSet.png");
        add("BG dark green",   folder + "Background_Dark-Green_TileSet.png");
        add("BG yellow",       folder + "Background_Bleak-Yellow_TileSet.png");
        add("brick wall",      folder + "Brick-Wall_TileSet.png");
        add("garbage",         folder + "Garbage_TileSet.png");
        add("wire fence",      folder + "Wire-Fence/Wire-Fence_TileSet.png");
        add("iron fence",      folder + "Iron-Fence_TileSet.png");
        add("props",           folder + "props.png");
        add("roof",            folder + "Roof_TileSet.png");

        // ===== PALETTE =====
        palette = new Tile[] {
                tile("BG yellow", 52),       // 0: smooth dirt ground
                tile("BG yellow", 3),        // 1: cracked dirt
                tile("BG yellow", 27),       // 2: dirt with subtle texture
                solid("brick wall", 1),      // 3: broken wall top
                solid("brick wall", 2),      // 4: broken wall top
                solid("brick wall", 3),      // 5: broken wall top
                solid("brick wall", 4),      // 6: mossy wall top
                solid("brick wall", 9),      // 7: wall body (second row)
                solid("brick wall", 6),      // 8: left side wall
                solid("brick wall", 11),     // 9: right side wall
                solid("brick wall", 16),     // 10: bottom wall
                solid("wire fence", 6),      // 11: fence (plain)
                solid("wire fence", 14),     // 12: fence with post
                solid("wire fence", 13),     // 13: fence with trash stuck in it
                solid("wire fence", 0),      // 14: fence with a bag stuck in it

                // ----- BIG PILE pieces -----
                tile("garbage", 0),          // 15
                tile("garbage", 1),          // 16
                tile("garbage", 2),          // 17
                tile("garbage", 3),          // 18
                tile("garbage", 4),          // 19
                tile("garbage", 16),         // 20
                tile("garbage", 8),          // 21
                tile("garbage", 20),         // 22
                tile("garbage", 12),         // 23
                tile("garbage", 24),         // 24
                tile("garbage", 25),         // 25
                tile("garbage", 26),         // 26
                tile("garbage", 27),         // 27
                tile("garbage", 28),         // 28
                tile("garbage", 9),          // 29
                tile("garbage", 11),         // 30
                tile("garbage", 17),         // 31
                tile("garbage", 18),         // 32
                tile("garbage", 10),         // 33
                tile("garbage", 19),         // 34

                // ----- SMALL PILE pieces -----
                tile("garbage", 5),          // 35
                tile("garbage", 6),          // 36
                tile("garbage", 7),          // 37
                tile("garbage", 13),         // 38
                tile("garbage", 14),         // 39
                tile("garbage", 15),         // 40
                tile("garbage", 21),         // 41
                tile("garbage", 22),         // 42
                tile("garbage", 23)          // 43
        };

        // Fix transparent/black edges on fence and garbage pieces
        BufferedImage ground = palette[0].image;
        for (int i = 11; i < palette.length; i++) {
            palette[i] = new Tile(onTop(palette[i].image, ground), palette[i].collision);
        }
        Color SKY = new Color(196, 178, 60);
        BufferedImage skyTile = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = skyTile.createGraphics();
        sg.setColor(SKY);
        sg.fillRect(0, 0, 16, 16);
        sg.dispose();
        for (int i = 3; i <= 6; i++) {
            palette[i] = new Tile(onTop(palette[i].image, skyTile), palette[i].collision);
        }

        // ===== RUINED TOWN GROUND, DIRT PATHS & SPARSE GREEN TILES (44-63) =====
        ruinedSheet = findImage("ruined1.png");
        palette = java.util.Arrays.copyOf(palette, 81);

        // Smooth dirt sub-tiles from ruined1.png (avoiding dark hole edges)
        BufferedImage[] smoothDirt = new BufferedImage[4];
        if (ruinedSheet != null && ruinedSheet.getWidth() >= 256 && ruinedSheet.getHeight() >= 256) {
            smoothDirt[0] = ruinedSheet.getSubimage(4 * 32, 6 * 32, 16, 16);
            smoothDirt[1] = ruinedSheet.getSubimage(4 * 32 + 16, 6 * 32, 16, 16);
            smoothDirt[2] = ruinedSheet.getSubimage(5 * 32, 6 * 32, 16, 16);
            smoothDirt[3] = ruinedSheet.getSubimage(4 * 32, 7 * 32, 16, 16);
        } else {
            smoothDirt[0] = palette[0].image;
            smoothDirt[1] = palette[1].image;
            smoothDirt[2] = palette[2].image;
            smoothDirt[3] = palette[0].image;
        }

        // 44-47: smooth ruined1 dirt; 48-51: smooth BG yellow dirt
        palette[44] = new Tile(smoothDirt[0], false);
        palette[45] = new Tile(smoothDirt[1], false);
        palette[46] = new Tile(smoothDirt[2], false);
        palette[47] = new Tile(smoothDirt[3], false);
        palette[48] = tile("BG yellow", 52);
        palette[49] = tile("BG yellow", 3);
        palette[50] = tile("BG yellow", 27);
        palette[51] = tile("BG yellow", 52);

        // 52-59: use the brick-like tiles from palette[48..51] (BG yellow: 52, 3, 27, 52) for pathways & bridge deck
        for (int i = 0; i < 8; i++) {
            palette[52 + i] = palette[48 + (i % 4)];
        }

        // 60-63: sparse green grass patches
        palette[60] = tile("BG green", 0);
        palette[61] = tile("BG green", 1);
        palette[62] = tile("BG green", 8);
        palette[63] = tile("BG green", 9);

        // 64-67: dark-green ground & subtle variation
        palette[64] = tile("BG dark green", 14);
        palette[65] = tile("BG dark green", 15);
        palette[66] = tile("BG dark green", 38);
        palette[67] = tile("BG dark green", 39);

        // 68-71: solid brick & iron-fence barriers
        palette[68] = new Tile(onTop(image("brick wall", 1), skyTile), true);
        palette[69] = new Tile(onTop(image("brick wall", 9), ground), true);
        palette[70] = new Tile(onTop(image("brick wall", 4), skyTile), true);
        palette[71] = new Tile(onTop(image("iron fence", 0), palette[64].image), true);

        // ===== SWAMP, TOXIC & WATER _2 PNGs (72-80) =====
        BufferedImage swampFill = findImage("swamp_fill_2.png");
        BufferedImage swampTopA = findImage("swamp_top_a_2.png");
        BufferedImage swampTopB = findImage("swamp_top_b_2.png");
        BufferedImage toxicFill = findImage("toxic_fill_2.png");
        BufferedImage toxicTopA = findImage("toxic_top_a_2.png");
        BufferedImage toxicTopB = findImage("toxic_top_b_2.png");
        BufferedImage waterFill = findImage("water_fill_2.png");
        BufferedImage waterTopA = findImage("water_top_a_2.png");
        BufferedImage waterTopB = findImage("water_top_b_2.png");

        palette[72] = new Tile(swampFill != null ? swampFill : palette[64].image, false);
        palette[73] = new Tile(swampTopA != null ? onTop(swampTopA, palette[64].image) : palette[64].image, false);
        palette[74] = new Tile(swampTopB != null ? onTop(swampTopB, palette[64].image) : palette[65].image, false);

        // Contaminated green water (75-77)
        palette[75] = new Tile(toxicFill != null ? toxicFill : palette[66].image, false);
        palette[76] = new Tile(toxicTopA != null ? onTop(toxicTopA, palette[66].image) : palette[66].image, false);
        palette[77] = new Tile(toxicTopB != null ? onTop(toxicTopB, palette[66].image) : palette[67].image, false);

        // Cleaner blue water (78-80)
        palette[78] = new Tile(waterFill != null ? waterFill : palette[0].image, true);
        palette[79] = new Tile(waterTopA != null ? onTop(waterTopA, palette[78].image) : palette[78].image, true);
        palette[80] = new Tile(waterTopB != null ? onTop(waterTopB, palette[78].image) : palette[78].image, true);

        // Load TheSwamp.txt by default
        loadMap(TOWN_MAP);

        // ===== BURNING BARRELS (only active on BARREL_MAP) =====
        java.util.List<BufferedImage> barrelTiles = tilesets.get("props");
        if (barrelTiles != null && barrelTiles.size() > 19) {
            for (int i = 0; i < 4; i++) {
                barrelFrames[i] = stack(barrelTiles.get(i), barrelTiles.get(16 + i));
            }
            addBarrel(13, 12);
            addBarrel(18, 12);
            addBarrel(6, 3);
            addBarrel(20, 10);
            addBarrel(9, 17);
            addBarrel(21, 22);
        }

        // ===== RUINED TOWN HOUSES (townOnly) =====
        addHouse("spr_old_building_1.png", 3, 2);
        addHouse("spr_old_building_2.png", 14, 2);
        addHouse("spr_old_building_3.png", 29, 2);

        addHouse("spr_old_building_4.png", 3, 15);
        addHouse("spr_old_building_5.png", 15, 15);
        addHouse("spr_old_building_6.png", 28, 15);

        addHouse("spr_old_building_7.png", 3, 29);
        addHouse("spr_old_building_1.png", 15, 29);
        addHouse("spr_old_building_2.png", 28, 29);

        // Solid props cut from ruined1.png (32x32 cell grid) - RuinedTown
        addPiece(16, 5, 2, 2, 36, 3, true, true, false);
        addPiece(18, 5, 2, 2, 1, 22, true, true, false);
        addPiece(16, 5, 2, 2, 36, 22, true, true, false);

        addPiece(16, 8, 3, 5, 9, 14, true, true, false);
        addPiece(16, 8, 3, 5, 23, 14, true, true, false);
        addPiece(16, 8, 3, 5, 9, 28, true, true, false);
        addPiece(16, 8, 3, 5, 23, 28, true, true, false);

        addPiece(13, 6, 1, 2, 23, 7, true, true, false);
        addPiece(13, 8, 1, 1, 8, 21, true, true, false);
        addPiece(13, 8, 1, 1, 22, 21, true, true, false);
        addPiece(13, 9, 1, 2, 37, 19, true, true, false);
        addPiece(10, 10, 2, 1, 18, 23, true, true, false);

        // Walkable details & sparse weeds from ruined1.png - RuinedTown
        addPiece(12, 8, 1, 1, 7, 22, false, true, false);
        int[][] weeds = {
                {22, 1}, {35, 1}, {1, 10}, {15, 10}, {28, 10},
                {1, 23}, {14, 23}, {27, 23}, {37, 23}, {15, 37}
        };
        int[][] weedCells = {{12, 10}, {13, 12}, {12, 12}, {14, 11}};
        for (int i = 0; i < weeds.length; i++) {
            int[] wc = weedCells[i % weedCells.length];
            addPiece(wc[0], wc[1], 1, 1, weeds[i][0], weeds[i][1], false, true, false);
        }

        // ===== SWAMP BANK PROPS (swampOnly) =====
        addPiece(16, 5, 2, 2, 28, 15, true, false, true);   // rock crag near upper bank
        addPiece(18, 5, 2, 2, 6, 23, true, false, true);    // rock crag on lower-left bank
        addPiece(13, 6, 1, 2, 34, 16, true, false, true);   // barrel + crate on right bank
        addPiece(13, 8, 1, 1, 10, 22, true, false, true);   // tire near lower-left bank
        addPiece(13, 8, 1, 1, 33, 21, true, false, true);   // tire near right bank
        addPiece(13, 9, 1, 2, 36, 20, true, false, true);   // trash bin + small rock on right bank
        addPiece(16, 8, 3, 5, 35, 6, true, false, true);    // dead tree on upper-right bank
        addPiece(16, 8, 3, 5, 5, 27, true, false, true);    // dead tree on lower-left bank

        // Walkable swamp weeds & embers
        addPiece(12, 8, 1, 1, 8, 17, false, false, true);
        int[][] swampWeeds = {
                {7, 4}, {11, 6}, {27, 5}, {33, 12}, {9, 24}, {26, 22}, {34, 24}
        };
        for (int i = 0; i < swampWeeds.length; i++) {
            int[] wc = weedCells[i % weedCells.length];
            addPiece(wc[0], wc[1], 1, 1, swampWeeds[i][0], swampWeeds[i][1], false, false, true);
        }

        props.sort((a, b) -> Integer.compare(a.row * 16 + a.img.getHeight(), b.row * 16 + b.img.getHeight()));
    }

    private BufferedImage stack(BufferedImage top, BufferedImage bottom) {
        BufferedImage out = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(top, 0, 0, null);
        g.drawImage(bottom, 0, 16, null);
        g.dispose();
        return out;
    }

    private void addBarrel(int col, int row) {
        if (!isGround(col, row) || !isGround(col, row + 1)) {
            return;
        }
        barrels.add(new int[]{col, row, barrels.size()});
    }

    private boolean isGround(int col, int row) {
        if (map == null || col < 0 || row < 0 || col >= worldCols || row >= worldRows) return false;
        int n = map[row][col];
        return n >= 0 && n <= 2;
    }

    private boolean barrelsOn() {
        return currentMapPath != null && currentMapPath.equals(BARREL_MAP);
    }

    private java.util.Map<String, java.nio.file.Path> pngIndex;

    private BufferedImage findImage(String fileName) {
        String[] folders = {
                "/PostApocalypse AssetPack/Tiles/",
                "/PostApocalypse AssetPack/Tiles/Buildings/",
                "/PostApocalypse AssetPack/"
        };
        for (String dir : folders) {
            try {
                InputStream in = getClass().getResourceAsStream(dir + fileName);
                if (in != null) {
                    BufferedImage img = ImageIO.read(in);
                    in.close();
                    System.out.println("Found " + fileName + " in " + dir);
                    return img;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            if (pngIndex == null) {
                pngIndex = new java.util.HashMap<>();
                java.net.URL root = getClass().getResource("/");
                if (root != null && "file".equals(root.getProtocol())) {
                    java.nio.file.Path start = java.nio.file.Paths.get(root.toURI());
                    try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(start)) {
                        walk.filter(p -> p.toString().toLowerCase().endsWith(".png"))
                                .forEach(p -> pngIndex.putIfAbsent(p.getFileName().toString().toLowerCase(), p));
                    }
                }
            }
            java.nio.file.Path hit = pngIndex.get(fileName.toLowerCase());
            if (hit != null) {
                System.out.println("Found " + fileName + " at " + hit);
                return ImageIO.read(hit.toFile());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("COULD NOT FIND " + fileName + " in the project's resources.");
        return null;
    }

    private void addHouse(String fileName, int col, int row) {
        BufferedImage img = findImage(fileName);
        if (img == null) {
            img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = img.createGraphics();
            pg.setColor(new Color(90, 90, 90));
            pg.fillRect(8, 8, 112, 112);
            pg.setColor(Color.RED);
            pg.drawRect(8, 8, 111, 111);
            pg.setColor(Color.WHITE);
            pg.setFont(new Font("Arial", Font.PLAIN, 9));
            pg.drawString("MISSING:", 14, 50);
            pg.drawString(fileName.replace("spr_old_", ""), 14, 64);
            pg.dispose();
        }
        makeProp(img, col, row, true, true, false);
    }

    private void addPiece(int cx, int cy, int cw, int ch, int col, int row, boolean solid, boolean townOnly, boolean swampOnly) {
        if (ruinedSheet == null) return;
        int x = cx * 32, y = cy * 32;
        int w = Math.min(cw * 32, ruinedSheet.getWidth() - x);
        int h = Math.min(ch * 32, ruinedSheet.getHeight() - y);
        if (w <= 0 || h <= 0) return;
        makeProp(ruinedSheet.getSubimage(x, y, w, h), col, row, solid, townOnly, swampOnly);
    }

    private void makeProp(BufferedImage img, int col, int row, boolean solid, boolean townOnly, boolean swampOnly) {
        Prop p = new Prop();
        p.img = img;
        p.col = col;
        p.row = row;
        p.townOnly = townOnly;
        p.swampOnly = swampOnly;
        int cw = (img.getWidth() + 15) / 16, ch = (img.getHeight() + 15) / 16;
        p.solidCells = new boolean[ch][cw];
        if (solid) {
            for (int r = 0; r < ch; r++) {
                for (int c = 0; c < cw; c++) {
                    int opaque = 0;
                    for (int y = r * 16; y < Math.min(img.getHeight(), r * 16 + 16); y++)
                        for (int x = c * 16; x < Math.min(img.getWidth(), c * 16 + 16); x++)
                            if (((img.getRGB(x, y) >>> 24) & 0xFF) > 40) opaque++;
                    p.solidCells[r][c] = opaque > 128;
                }
            }
        }
        props.add(p);
    }

    private boolean isPropActive(Prop p) {
        if (currentMapPath == null) return false;
        if (p.townOnly) return currentMapPath.endsWith("RuinedTown.txt");
        if (p.swampOnly) return currentMapPath.endsWith("TheSwamp.txt");
        return false;
    }

    private boolean anyPropsActive() {
        return currentMapPath != null &&
                (currentMapPath.endsWith("RuinedTown.txt") || currentMapPath.endsWith("TheSwamp.txt"));
    }

    private BufferedImage onTop(BufferedImage top, BufferedImage under) {
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int p = top.getRGB(x, y);
                int u = under.getRGB(x, y);
                int a = (p >>> 24) & 0xFF;
                int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;

                boolean nearBlack = (a == 255 && r + g + b <= 15);
                boolean nearWhite = (a == 255 && r >= 242 && g >= 242 && b >= 242);

                if (a == 0 || nearBlack || nearWhite) {
                    out.setRGB(x, y, u);
                } else if (a < 255) {
                    int ur = (u >> 16) & 0xFF, ug = (u >> 8) & 0xFF, ub = u & 0xFF;
                    int nr = (r * a + ur * (255 - a)) / 255;
                    int ng = (g * a + ug * (255 - a)) / 255;
                    int nb = (b * a + ub * (255 - a)) / 255;
                    out.setRGB(x, y, (255 << 24) | (nr << 16) | (ng << 8) | nb);
                } else {
                    out.setRGB(x, y, p);
                }
            }
        }
        return out;
    }

    // ===== LOADING =====

    private void add(String nickname, String path) {
        java.util.List<BufferedImage> list = new java.util.ArrayList<>();
        try {
            InputStream in = getClass().getResourceAsStream(path);
            if (in == null) {
                System.out.println("Could not find tileset '" + nickname + "' at " + path);
                tilesets.put(nickname, list);
                return;
            }
            BufferedImage sheet = ImageIO.read(in);
            in.close();
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
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("Tileset '" + nickname + "' is missing or empty");
        }
        int safeIndex = Math.max(0, Math.min(number, list.size() - 1));
        return list.get(safeIndex);
    }

    private Tile tile(String nickname, int number)  { return new Tile(image(nickname, number), false); }
    private Tile solid(String nickname, int number) { return new Tile(image(nickname, number), true); }

    public void loadMap(String path) {
        try {
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) throw new FileNotFoundException("Map resource not found: " + path);
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            java.util.List<String[]> lines = new java.util.ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line.trim().split("\\s+"));
            }
            br.close();

            if (lines.isEmpty()) throw new IOException("Map is empty: " + path);
            worldRows = lines.size();
            worldCols = lines.get(0).length;
            map = new int[worldRows][worldCols];
            for (int row = 0; row < worldRows; row++) {
                if (lines.get(row).length != worldCols)
                    throw new IOException("Map rows have different widths: " + path + " (row " + row + ")");
                for (int col = 0; col < worldCols; col++)
                    map[row][col] = Integer.parseInt(lines.get(row)[col]);
            }
            currentMapPath = path;
            System.out.println("Loaded map " + path + " (" + worldCols + " x " + worldRows + " tiles)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== COLLISION =====

    public int worldWidthPx()  { return worldCols * gp.tileSize; }
    public int worldHeightPx() { return worldRows * gp.tileSize; }

    public boolean isSolidTile(int col, int row) {
        if (map == null || col < 0 || row < 0 || col >= worldCols || row >= worldRows) return true;

        if (barrelsAreSolid && barrelsOn()) {
            for (int[] b : barrels) {
                if (b[0] == col && b[1] + 1 == row) return true;
            }
        }

        if (anyPropsActive()) {
            for (Prop p : props) {
                if (!isPropActive(p)) continue;
                int r = row - p.row, c = col - p.col;
                if (r >= 0 && r < p.solidCells.length && c >= 0 && c < p.solidCells[0].length
                        && p.solidCells[r][c]) return true;
            }
        }

        int n = map[row][col];
        if (n < 0 || n >= palette.length) return false;
        return palette[n].collision;
    }

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
                if (n < 0 || n >= palette.length) continue;
                g2.drawImage(palette[n].image, col * ts - camX, row * ts - camY, ts, ts, null);
            }
        }

        if (anyPropsActive()) {
            for (Prop p : props) {
                if (!isPropActive(p)) continue;
                int sx = p.col * ts - camX;
                int sy = p.row * ts - camY;
                int dw = p.img.getWidth() * ts / 16;
                int dh = p.img.getHeight() * ts / 16;
                if (sx + dw < 0 || sx > gp.screenWidth || sy + dh < 0 || sy > gp.screenHeight) continue;
                g2.drawImage(p.img, sx, sy, dw, dh, null);
            }
        }

        if (barrelsOn() && barrelFrames[0] != null) {
            long tick = System.currentTimeMillis() / 150;
            for (int[] b : barrels) {
                int sx = b[0] * ts - camX;
                int sy = b[1] * ts - camY;
                int dw = ts;
                int dh = ts * 2;
                if (sx + dw < 0 || sx > gp.screenWidth || sy + dh < 0 || sy > gp.screenHeight) continue;
                int frame = (int) ((tick + b[2]) % 4);
                g2.drawImage(barrelFrames[frame], sx, sy, dw, dh, null);
            }
        }
    }
}
