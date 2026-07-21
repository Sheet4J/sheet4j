package com.sheetmusic4j.fxviewer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sheetmusic4j.engraving.Glyph;
import com.sheetmusic4j.engraving.GlyphPlacement;
import com.sheetmusic4j.engraving.LayoutResult;
import com.sheetmusic4j.engraving.MarkingCategory;
import com.sheetmusic4j.engraving.StaffLayout;
import com.sheetmusic4j.engraving.SystemLayout;
import com.sheetmusic4j.engraving.TextPlacement;

class ScorePainterTextVisibilityTest {

    private static LayoutResult buildLayout() {
        StaffLayout staff = new StaffLayout(0, 100, 500, 10.0,
                List.of(), List.of(), List.of(), List.of());
        SystemLayout system = new SystemLayout(0, 100, 500, List.of(staff));
        List<TextPlacement> texts = List.of(
                new TextPlacement("Title", 250, 40, 24, TextPlacement.Align.CENTER,
                        MarkingCategory.TITLE),
                new TextPlacement("Composer", 480, 70, 12, TextPlacement.Align.RIGHT,
                        MarkingCategory.CREATOR),
                new TextPlacement("La", 100, 180, 14, TextPlacement.Align.CENTER,
                        MarkingCategory.LYRIC));
        return new LayoutResult(List.of(system), texts, 500, 200);
    }

    @Test
    void showsAllTextByDefault() {
        RecordingSurface surface = new RecordingSurface();
        new ScorePainter().paint(surface, buildLayout(), 500, 200);
        assertEquals(List.of("Title", "Composer", "La"), surface.textsDrawn);
    }

    @Test
    void hidingLyricCategorySkipsLyricText() {
        RecordingSurface surface = new RecordingSurface();
        ScorePainter painter = new ScorePainter();
        painter.setHiddenCategories(EnumSet.of(MarkingCategory.LYRIC));
        painter.paint(surface, buildLayout(), 500, 200);
        assertEquals(List.of("Title", "Composer"), surface.textsDrawn);
    }

    @Test
    void hidingCreatorCategorySkipsCreatorText() {
        RecordingSurface surface = new RecordingSurface();
        ScorePainter painter = new ScorePainter();
        painter.setHiddenCategories(EnumSet.of(MarkingCategory.CREATOR));
        painter.paint(surface, buildLayout(), 500, 200);
        assertEquals(List.of("Title", "La"), surface.textsDrawn);
        }

        @Test
        void hidingTitleAndSubtitleShowsCreators() {
        RecordingSurface surface = new RecordingSurface();
        ScorePainter painter = new ScorePainter();
        painter.setHiddenCategories(EnumSet.of(
                MarkingCategory.TITLE, MarkingCategory.SUBTITLE));
        painter.paint(surface, buildLayout(), 500, 200);
        assertEquals(List.of("Composer", "La"), surface.textsDrawn);
        }

    @Test
    void hiddenCategoryStillDrawsStaves() {
        RecordingSurface surface = new RecordingSurface();
        ScorePainter painter = new ScorePainter();
        painter.setHiddenCategories(EnumSet.allOf(MarkingCategory.class));
        painter.paint(surface, buildLayout(), 500, 200);
        assertTrue(surface.textsDrawn.isEmpty(), "no text should be drawn");
        assertTrue(surface.strokeLineCount > 0,
                "staff lines must still be drawn even when all text is hidden");
    }

    @Test
    void hidingDynamicCategorySkipsDynamicGlyph() {
        List<GlyphPlacement> glyphs = List.of(
                new GlyphPlacement(100, 105, Glyph.NOTEHEAD_BLACK, 4, MarkingCategory.NOTE),
                new GlyphPlacement(120, 160, Glyph.DYNAMIC_F, 12, MarkingCategory.DYNAMIC));
        StaffLayout staff = new StaffLayout(0, 100, 500, 10.0,
                List.of(), glyphs, List.of(), List.of());
        SystemLayout system = new SystemLayout(0, 100, 500, List.of(staff));
        LayoutResult layout = new LayoutResult(List.of(system), List.of(), 500, 200);

        RecordingSurface surface = new RecordingSurface();
        ScorePainter painter = new ScorePainter();
        painter.setHiddenCategories(EnumSet.of(MarkingCategory.DYNAMIC));
        painter.paint(surface, layout, 500, 200);
        assertTrue(surface.strokeLineCount > 0, "staff lines must still be drawn");
        assertFalse(surface.dynamicGlyphDrawn,
                "dynamic glyph must be skipped when its category is hidden");
        assertTrue(surface.noteGlyphDrawn,
                "note glyph must still be drawn when only dynamics are hidden");
    }

    @Test
    void showsAllGlyphsByDefault() {
        List<GlyphPlacement> glyphs = List.of(
                new GlyphPlacement(100, 105, Glyph.NOTEHEAD_BLACK, 4, MarkingCategory.NOTE),
                new GlyphPlacement(120, 160, Glyph.DYNAMIC_F, 12, MarkingCategory.DYNAMIC));
        StaffLayout staff = new StaffLayout(0, 100, 500, 10.0,
                List.of(), glyphs, List.of(), List.of());
        SystemLayout system = new SystemLayout(0, 100, 500, List.of(staff));
        LayoutResult layout = new LayoutResult(List.of(system), List.of(), 500, 200);

        RecordingSurface surface = new RecordingSurface();
        new ScorePainter().paint(surface, layout, 500, 200);
        assertTrue(surface.noteGlyphDrawn);
        assertTrue(surface.dynamicGlyphDrawn);
    }

    @Test
    void gettersReflectHiddenCategories() {
        ScorePainter painter = new ScorePainter();
        painter.setHiddenCategories(EnumSet.of(MarkingCategory.CREATOR));
        assertEquals(EnumSet.of(MarkingCategory.CREATOR), painter.getHiddenCategories());
        painter.setHiddenCategories(EnumSet.noneOf(MarkingCategory.class));
        assertTrue(painter.getHiddenCategories().isEmpty());
    }

    /** Minimal {@link RenderSurface} that records drawn text and counts strokes. */
    private static final class RecordingSurface implements RenderSurface {
        final List<String> textsDrawn = new ArrayList<>();
        int strokeLineCount;
        boolean noteGlyphDrawn;
        boolean dynamicGlyphDrawn;

        @Override public void setStroke(RenderColor color) { }
        @Override public void setFill(RenderColor color) { }
        @Override public void setLineWidth(double width) { }
        @Override public void fillRect(double x, double y, double w, double h) { }
        @Override public void strokeLine(double x1, double y1, double x2, double y2) {
            strokeLineCount++;
        }
        @Override public void fillOval(double x, double y, double w, double h) {
            // Notehead primitive fallback draws a filled oval.
            noteGlyphDrawn = true;
        }
        @Override public void strokeOval(double x, double y, double w, double h) { }
        @Override public void strokeText(String text, double x, double y) {
            textsDrawn.add(text);
            // Dynamic primitive fallback strokes the mark's letters.
            if (text != null && (text.equals("f") || text.equals("p") || text.equals("ff")
                    || text.equals("mf") || text.equals("mp") || text.equals("pp")
                    || text.equals("fff") || text.equals("ppp") || text.equals("sf")
                    || text.equals("sfz") || text.equals("fz") || text.equals("fp")
                    || text.equals("rf") || text.equals("rfz") || text.equals("n"))) {
                dynamicGlyphDrawn = true;
            }
        }
        @Override public void drawText(String text, double x, double y, double fontSize) {
            textsDrawn.add(text);
        }
    }
}
