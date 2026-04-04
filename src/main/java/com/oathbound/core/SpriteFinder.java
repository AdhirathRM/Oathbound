package com.oathbound.core;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class SpriteFinder extends JPanel {
    private BufferedImage spriteSheet;
    private static final int TILE_SIZE = 16;

    public SpriteFinder() {
        try {
            // Make sure this path matches where you put Tilesetv3.png!
            InputStream is = getClass().getResourceAsStream("/sprites/Tilesetv3.png");
            if (is != null) {
                spriteSheet = ImageIO.read(is);
                setPreferredSize(new Dimension(spriteSheet.getWidth(), spriteSheet.getHeight()));
            } else {
                System.out.println("Could not find image!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // This listens for your mouse clicks
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // The magic math: Divide pixel position by tile size to get the grid column/row
                int col = e.getX() / TILE_SIZE;
                int row = e.getY() / TILE_SIZE;
                
                System.out.println("You clicked: Column " + col + ", Row " + row);
                System.out.println("Code: getTile(" + col + ", " + row + ");\n");
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (spriteSheet != null) {
            g.drawImage(spriteSheet, 0, 0, null);

            // Draw a helpful grid over the image
            g.setColor(new Color(255, 255, 255, 100)); // Semi-transparent white
            for (int x = 0; x < getWidth(); x += TILE_SIZE) {
                g.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += TILE_SIZE) {
                g.drawLine(0, y, getWidth(), y);
            }
        }
    }

    // A main method so you can run this file directly!
    public static void main(String[] args) {
        JFrame frame = new JFrame("Click a tile to get its code!");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new JScrollPane(new SpriteFinder())); // ScrollPane in case the image is huge
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}