package com.sheetmusic4j.fxdemo.reference;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Rasterizes pages of a PDF to {@link BufferedImage}s using Apache PDFBox if it
 * is available on the classpath.
 *
 * <p>PDFBox is invoked reflectively so this helper compiles and runs regardless
 * of whether PDFBox 2.x ({@code PDDocument.load}) or 3.x ({@code Loader.loadPDF})
 * is present - or whether it is present at all. When it cannot rasterize, the
 * methods return {@link Optional#empty()} (or {@link OptionalInt#empty()}) so
 * callers can skip gracefully instead of failing.
 *
 * <p>PDFBox is currently pulled in transitively via
 * {@code com.dlsc.pdfviewfx:pdfviewfx}; keeping the reflective loader lets
 * this class also degrade gracefully if that transitive path ever changes.
 */
public final class PdfRasterizer {

    private PdfRasterizer() {
    }

    /**
     * Return the total number of pages in the given PDF, or empty if PDFBox is
     * unavailable / the document cannot be loaded.
     *
     * @param pdf path to the PDF file
     * @return page count, or empty when the count could not be determined
     */
    public static OptionalInt pageCount(Path pdf) {
        Object document = null;
        Class<?> pdDocumentClass;
        try {
            document = loadDocument(pdf.toFile());
            pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            int count = (int) pdDocumentClass.getMethod("getNumberOfPages").invoke(document);
            return OptionalInt.of(count);
        } catch (Throwable t) {
            return OptionalInt.empty();
        } finally {
            closeQuietly(document);
        }
    }

    /**
     * Rasterize the first page of a PDF at the given DPI.
     *
     * <p>Kept as a thin convenience wrapper over
     * {@link #rasterizePage(Path, int, float)} for callers that only need
     * page 0.
     *
     * @param pdf path to the PDF file
     * @param dpi rasterization DPI
     * @return rasterized image, or empty when rasterization failed
     */
    public static Optional<BufferedImage> rasterizeFirstPage(Path pdf, float dpi) {
        return rasterizePage(pdf, 0, dpi);
    }

    /**
     * Rasterize a single page of a PDF at the given DPI.
     *
     * @param pdf       path to the PDF file
     * @param pageIndex zero-based page index
     * @param dpi       rasterization DPI
     * @return rasterized image, or empty when rasterization failed
     */
    public static Optional<BufferedImage> rasterizePage(Path pdf, int pageIndex, float dpi) {
        Object document = null;
        try {
            document = loadDocument(pdf.toFile());
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> rendererClass = Class.forName("org.apache.pdfbox.rendering.PDFRenderer");

            Object renderer = rendererClass.getConstructor(pdDocumentClass).newInstance(document);
            BufferedImage image = (BufferedImage) rendererClass
                    .getMethod("renderImageWithDPI", int.class, float.class)
                    .invoke(renderer, pageIndex, dpi);
            return Optional.ofNullable(image);
        } catch (Throwable t) {
            return Optional.empty();
        } finally {
            closeQuietly(document);
        }
    }

    /**
     * Rasterize every page of a PDF at the given DPI. The {@code PDDocument} is
     * opened once and reused for all pages.
     *
     * @param pdf path to the PDF file
     * @param dpi rasterization DPI
     * @return list of rasterized images (one per page, in order), or empty when
     *         rasterization failed
     */
    public static Optional<List<BufferedImage>> rasterizeAllPages(Path pdf, float dpi) {
        Object document = null;
        try {
            document = loadDocument(pdf.toFile());
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> rendererClass = Class.forName("org.apache.pdfbox.rendering.PDFRenderer");

            int pageCount = (int) pdDocumentClass.getMethod("getNumberOfPages").invoke(document);
            Object renderer = rendererClass.getConstructor(pdDocumentClass).newInstance(document);
            var renderMethod = rendererClass.getMethod("renderImageWithDPI", int.class, float.class);

            List<BufferedImage> pages = new ArrayList<>(pageCount);
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = (BufferedImage) renderMethod.invoke(renderer, i, dpi);
                if (image == null) {
                    return Optional.empty();
                }
                pages.add(image);
            }
            return Optional.of(pages);
        } catch (Throwable t) {
            return Optional.empty();
        } finally {
            closeQuietly(document);
        }
    }

    private static Object loadDocument(File file) throws Exception {
        try {
            // PDFBox 3.x
            Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
            return loaderClass.getMethod("loadPDF", File.class).invoke(null, file);
        } catch (ClassNotFoundException notV3) {
            // PDFBox 2.x
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            return pdDocumentClass.getMethod("load", File.class).invoke(null, file);
        }
    }

    private static void closeQuietly(Object document) {
        if (document == null) {
            return;
        }
        try {
            document.getClass().getMethod("close").invoke(document);
        } catch (Throwable ignored) {
            // best effort - nothing to do if PDFBox is not on classpath any more
        }
    }
}
