package com.sheetmusic4j.fxdemo.reference;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class PdfRasterizerTest {

    private static final Path SAMPLES_DIR =
            Paths.get("src", "test", "resources", "xmlsamples");

    private static final float DPI = 72f;

    @Test
    void rasterizesSinglePagePdf() {
        Path pdf = SAMPLES_DIR.resolve("Echigo-Jishi.pdf");
        Assumptions.assumeTrue(Files.isRegularFile(pdf),
                "sample PDF missing: " + pdf.toAbsolutePath());

        OptionalInt count = PdfRasterizer.pageCount(pdf);
        Assumptions.assumeTrue(count.isPresent(), "PDFBox unavailable; skipping.");
        assertEquals(1, count.getAsInt(), "Echigo-Jishi.pdf should have 1 page");

        Optional<List<BufferedImage>> pages = PdfRasterizer.rasterizeAllPages(pdf, DPI);
        assertTrue(pages.isPresent(), "rasterizeAllPages should return a non-empty Optional");
        assertEquals(1, pages.get().size(), "one image expected for a 1-page PDF");
        assertPositiveDims(pages.get().get(0));
    }

    @Test
    void rasterizesMultiPagePdf() {
        Path pdf = SAMPLES_DIR.resolve("Dichterliebe01.pdf");
        Assumptions.assumeTrue(Files.isRegularFile(pdf),
                "sample PDF missing: " + pdf.toAbsolutePath());

        OptionalInt count = PdfRasterizer.pageCount(pdf);
        Assumptions.assumeTrue(count.isPresent(), "PDFBox unavailable; skipping.");
        assertTrue(count.getAsInt() >= 2, "Dichterliebe01.pdf should have multiple pages, got " + count.getAsInt());

        Optional<List<BufferedImage>> pages = PdfRasterizer.rasterizeAllPages(pdf, DPI);
        assertTrue(pages.isPresent(), "rasterizeAllPages should succeed on Dichterliebe01.pdf");
        assertEquals(count.getAsInt(), pages.get().size(),
                "pageCount and rasterizeAllPages should agree");
        for (int i = 0; i < pages.get().size(); i++) {
            assertNotNull(pages.get().get(i), "page " + i + " should not be null");
            assertPositiveDims(pages.get().get(i));
        }
    }

    @Test
    void rasterizeFirstPageDelegatesToRasterizePage() {
        Path pdf = SAMPLES_DIR.resolve("Echigo-Jishi.pdf");
        Assumptions.assumeTrue(Files.isRegularFile(pdf));

        Optional<BufferedImage> first = PdfRasterizer.rasterizeFirstPage(pdf, DPI);
        Assumptions.assumeTrue(first.isPresent(), "PDFBox unavailable; skipping.");

        Optional<BufferedImage> page0 = PdfRasterizer.rasterizePage(pdf, 0, DPI);
        assertTrue(page0.isPresent());
        assertEquals(first.get().getWidth(), page0.get().getWidth());
        assertEquals(first.get().getHeight(), page0.get().getHeight());
    }

    private static void assertPositiveDims(BufferedImage image) {
        assertTrue(image.getWidth() > 0, "image width must be > 0");
        assertTrue(image.getHeight() > 0, "image height must be > 0");
    }
}
