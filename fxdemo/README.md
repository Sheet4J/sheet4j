#  

:: FX Demo

Standalone JavaFX testbed for the Sheetmusic4J engraving/rendering pipeline. Exposes the same `ScorePainter` used by the
on-screen viewer through a headless AWT surface, so its output can be compared to a trusted reference in tests.

## Local commands

Run the default test suite (no WebView, no network, no display needed):

    mvn -pl fxdemo -am test

Launch the interactive demo:

    mvn -pl fxdemo javafx:run

## How to add a fixture

1. Drop the `.musicxml` file into `src/test/resources/xmlsamples/`.
2. Drop the companion `.pdf` (the authored engraving, same base name)
   next to it.
3. Open the PDF once and note its page count.
4. Add an `Arguments.of(basename, xmlPath, expectedPageCount)` entry to
   `CompareFxViewWithReferenceTest.fixtures()`.
5. Run `mvn -pl fxdemo test`. On failure the diagnostic HTML report is written to
   `target/sheetmusic4j-diff/<fixture>/report.html`.

Fixtures without a sibling PDF are skipped automatically via JUnit
`Assumptions`; the reference comparison is PDF-driven so the sibling PDF is authoritative for both metadata (page count)
and pixel-level diagnostics (per-measure similarity after vertical page stitching).

## Tests

- `RenderingPipelineTest` — deterministic smoke test that the pipeline produces non-blank output for
  `c-major-scale.musicxml`.
- `CompareFxViewWithReferenceTest` — step-by-step diagnostic comparison against the sibling PDF. Rasterizes every page
  via `PdfRasterizer`
  (PDFBox), stitches them vertically via `ImageStack`, and feeds the result through the `DiagnosticComparator` pipeline.
  DPI is tunable via `-Dsheetmusic4j.compare.pdf.dpi=<float>` (default `150`).

## CI

`.github/workflows/ci.yml` runs `mvn verify` on JDK 26. It never opens a WebView; the reference PDFs are committed and
rasterized in-process via Apache PDFBox (pulled in transitively through
`com.dlsc.pdfviewfx:pdfviewfx`).
