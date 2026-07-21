id: 77142dad-5e2c-4b28-89ac-90d8b20ef9ab
sessionId: 68953cdd-ec39-4071-a87d-f55fa5afc0cd
date: '2026-07-21T06:12:31.301Z'
label: Switch reference comparison from OSMD-generated PNGs to sibling PDFs
---
# Switch reference comparison from OSMD-generated PNGs to sibling PDFs

## Goal

Retire the OSMD-in-WebView PNG generation pipeline that
`CompareFxViewWithReferenceTest` depends on. Use the `.pdf` files already
committed next to most `xmlsamples/*.musicxml` fixtures as the source of
truth for visual comparison, rasterizing them at test time via
`PdfRasterizer` (PDFBox). Also expose PDF metadata (page count) as a
first-class assertable test value.

## Design summary

For each fixture:

1. Resolve `xxx.pdf` next to `xxx.musicxml` via `PdfSibling.existingPathFor`.
   If absent, skip the invocation.
2. Read `pageCount` via `PdfRasterizer.pageCount(pdf)`; skip if PDFBox
   is unavailable.
3. Assert `pageCount == expectedPages` (from the fixture table below).
4. Rasterize every page via `PdfRasterizer.rasterizeAllPages(pdf, dpi)`
   and stitch vertically via `ImageStack.stackVertically(...)`.
5. Feed the stitched reference through the existing
   `DiagnosticComparator` + `DiffReportWriter` pipeline.

DPI is configurable via `-Dsheetmusic4j.compare.pdf.dpi=<float>`,
default `150`. PDFBox is inherited transitively via
`com.dlsc.pdfviewfx:pdfviewfx:3.4.2`.

## Fixture page counts (captured from the committed PDFs)

| basename            | expected pages |
|---------------------|----------------|
| ActorPreludeSample  | 4  |
| BeetAnGeSample      | 1  |
| BrahWiMeSample      | 1  |
| BrookeWestSample    | 1  |
| DebuMandSample      | 1  |
| Dichterliebe01      | 2  |
| Echigo-Jishi        | 1  |
| FaurReveSample      | 1  |
| MahlFaGe4Sample     | 1  |
| MozaChloSample      | 1  |
| MozaVeilSample      | 1  |
| SchbAvMaSample      | 1  |

Fixtures dropped from the reference comparison (no sibling PDF):
`c-major-scale` (still covered by `RenderingPipelineTest`),
`Saltarello`, `Telemann`, `MozartPianoSonata`, `MozartTrio`.

## Outcome

- `WebViewReferenceRenderer`, `ReferenceCache`,
  `GenerateReferenceImagesTest`, the `refresh-references` Maven profile
  and the `reference-generation` JUnit tag are all removed.
- `PdfRasterizer` is promoted to `src/main/.../fxdemo/reference/` and
  gains `pageCount`, `rasterizePage`, and `rasterizeAllPages`.
- New `ImageStack` helper composites multi-page PDFs into a single
  reference image consumed unchanged by `DiagnosticComparator`.
- `DiffReportWriter.write(...)` now takes the PDF page count and shows
  it in the reference pane caption.
- `SheetDemoApp`'s Diff tab was rewired to rasterize the sibling PDF
  in-process; its button was renamed to "Compare against PDF" and is
  disabled when no sibling PDF exists.

## Note

This plan supersedes the OSMD-reference premise of the earlier
`Sheet4j--incrementally-close-FX-OSMD` plan (already in `done/`).
