package com.sheetmusic4j.fxdemo.reference;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ImageStackTest {

    @Test
    void stacksSameSizedImagesWithGap() {
        BufferedImage a = solid(100, 50, Color.RED);
        BufferedImage b = solid(100, 50, Color.BLUE);

        BufferedImage stacked = ImageStack.stackVertically(List.of(a, b), 10, Color.WHITE);
        assertEquals(100, stacked.getWidth());
        assertEquals(50 + 10 + 50, stacked.getHeight());

        // First image occupies rows 0..49
        assertEquals(new Color(255, 0, 0).getRGB(), stacked.getRGB(0, 0) | 0xFF000000);
        // Gap row (50..59) is white
        assertEquals(Color.WHITE.getRGB(), stacked.getRGB(0, 55) | 0xFF000000);
        // Second image occupies rows 60..109
        assertEquals(new Color(0, 0, 255).getRGB(), stacked.getRGB(0, 60) | 0xFF000000);
    }

    @Test
    void padsHorizontallyForMixedWidths() {
        BufferedImage narrow = solid(40, 20, Color.RED);
        BufferedImage wide = solid(100, 30, Color.BLUE);

        BufferedImage stacked = ImageStack.stackVertically(List.of(narrow, wide), 0, Color.WHITE);
        assertEquals(100, stacked.getWidth());
        assertEquals(20 + 30, stacked.getHeight());

        // Narrow image is centered: x=30 -> 30..69 inclusive
        assertEquals(Color.WHITE.getRGB(), stacked.getRGB(0, 0) | 0xFF000000);
        assertEquals(new Color(255, 0, 0).getRGB(), stacked.getRGB(50, 10) | 0xFF000000);
        assertEquals(Color.WHITE.getRGB(), stacked.getRGB(95, 10) | 0xFF000000);
        // Wide image starts at y=20 and spans full width
        assertEquals(new Color(0, 0, 255).getRGB(), stacked.getRGB(0, 25) | 0xFF000000);
    }

    @Test
    void singleElementReturnedAsIs() {
        BufferedImage only = solid(20, 20, Color.RED);
        BufferedImage stacked = ImageStack.stackVertically(List.of(only), 5, Color.WHITE);
        assertSame(only, stacked);
    }

    @Test
    void emptyListRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ImageStack.stackVertically(List.of(), 0, Color.WHITE));
    }

    @Test
    void negativeGapClampedToZero() {
        BufferedImage a = solid(10, 10, Color.RED);
        BufferedImage b = solid(10, 10, Color.BLUE);
        BufferedImage stacked = ImageStack.stackVertically(List.of(a, b), -5, Color.WHITE);
        assertEquals(20, stacked.getHeight());
        assertNotSame(a, stacked);
    }

    private static BufferedImage solid(int w, int h, Color c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        try {
            g.setColor(c);
            g.fillRect(0, 0, w, h);
        } finally {
            g.dispose();
        }
        return img;
    }
}
