package com.sheetmusic4j.fxdemo.reference;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Composites a list of PDF page images into a single tall {@link BufferedImage}
 * so that the existing single-image {@link DiagnosticComparator} pipeline can
 * consume multi-page references transparently.
 */
public final class ImageStack {

    private ImageStack() {
    }

    /**
     * Stack {@code pages} vertically on a common canvas.
     *
     * <p>The canvas width is {@code max(page.width)} and the canvas height is
     * {@code sum(page.height) + gapPx * (n - 1)}. Each page is drawn centered
     * horizontally; any padding is filled with {@code background}.
     *
     * @param pages      list of page images, top-to-bottom (must not be empty)
     * @param gapPx      vertical gap in pixels inserted between adjacent pages
     * @param background background color used to fill the canvas / padding
     * @return the stitched image
     * @throws IllegalArgumentException if {@code pages} is empty
     */
    public static BufferedImage stackVertically(List<BufferedImage> pages, int gapPx, Color background) {
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("pages must not be empty");
        }
        if (pages.size() == 1) {
            return pages.get(0);
        }
        int gap = Math.max(0, gapPx);
        int width = 0;
        int height = 0;
        for (BufferedImage page : pages) {
            width = Math.max(width, page.getWidth());
            height += page.getHeight();
        }
        height += gap * (pages.size() - 1);

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(background);
            g.fillRect(0, 0, width, height);

            int y = 0;
            for (BufferedImage page : pages) {
                int x = (width - page.getWidth()) / 2;
                g.drawImage(page, x, y, null);
                y += page.getHeight() + gap;
            }
        } finally {
            g.dispose();
        }
        return canvas;
    }
}
