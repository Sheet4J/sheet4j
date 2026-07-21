package com.sheetmusic4j.engraving;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.sheetmusic4j.core.model.Attributes;
import com.sheetmusic4j.core.model.Clef;
import com.sheetmusic4j.core.model.Direction;
import com.sheetmusic4j.core.model.DirectionType;
import com.sheetmusic4j.core.model.Duration;
import com.sheetmusic4j.core.model.Harmony;
import com.sheetmusic4j.core.model.HarmonyKind;
import com.sheetmusic4j.core.model.KeySignature;
import com.sheetmusic4j.core.model.Measure;
import com.sheetmusic4j.core.model.MusicElement;
import com.sheetmusic4j.core.model.Note;
import com.sheetmusic4j.core.model.NoteType;
import com.sheetmusic4j.core.model.Part;
import com.sheetmusic4j.core.model.Pitch;
import com.sheetmusic4j.core.model.Placement;
import com.sheetmusic4j.core.model.Score;
import com.sheetmusic4j.core.model.Step;
import com.sheetmusic4j.core.model.TimeSignature;

class EngraverHarmonyTest {

    private static final int DIVISIONS = 1;

    private static Measure.Builder measureWithAttributes(int number) {
        return Measure.builder(number).attributes(Attributes.builder()
                .divisions(DIVISIONS)
                .keySignature(KeySignature.cMajor())
                .timeSignature(TimeSignature.fourFour())
                .clef(Clef.treble())
                .build());
    }

    private static Note quarter(Step step) {
        return Note.builder()
                .pitch(new Pitch(step, 4))
                .duration(new Duration(1, DIVISIONS))
                .type(NoteType.QUARTER)
                .build();
    }

    private static Harmony bMaj7() {
        return new Harmony(new Harmony.Root(Step.B, 0), HarmonyKind.MAJOR_SEVENTH,
                Optional.empty(), Optional.of("Maj7"));
    }

    private static Score scoreWith(MusicElement... elements) {
        Measure.Builder mb = measureWithAttributes(1);
        for (MusicElement el : elements) {
            mb.addElement(el);
        }
        return Score.builder()
                .addPart(Part.builder("P1").addMeasure(mb.build()).build())
                .build();
    }

    private static List<TextPlacement> textsOf(LayoutResult layout, MarkingCategory category) {
        return layout.texts().stream()
                .filter(t -> t.category() == category)
                .toList();
    }

    @Test
    void harmonyEmitsTextAboveStaff() {
        Score score = scoreWith(bMaj7(), quarter(Step.C));

        LayoutOptions options = LayoutOptions.defaults();
        LayoutResult layout = new Engraver().layout(score, options);

        List<TextPlacement> chordSymbols = textsOf(layout, MarkingCategory.CHORD_SYMBOL);
        assertEquals(1, chordSymbols.size());
        TextPlacement placement = chordSymbols.get(0);
        assertEquals("BMaj7", placement.text());
        assertTrue(!placement.boxed(),
                "chord symbol placement must not be boxed");

        double staffY = layout.staves().get(0).y();
        assertTrue(placement.y() < staffY,
                "chord symbol y (" + placement.y() + ") must sit above staff y (" + staffY + ")");
    }

    @Test
    void harmonyAnchorsToNextNoteX() {
        Score score = scoreWith(bMaj7(), quarter(Step.C), quarter(Step.D));

        LayoutResult layout = new Engraver().layout(score, LayoutOptions.defaults());
        TextPlacement placement = textsOf(layout, MarkingCategory.CHORD_SYMBOL).get(0);

        double firstNoteX = layout.staves().get(0).glyphs().stream()
                .filter(g -> g.glyph() == Glyph.NOTEHEAD_BLACK)
                .mapToDouble(GlyphPlacement::x)
                .findFirst().orElseThrow();
        assertEquals(firstNoteX, placement.x(), 1e-6,
                "chord symbol must anchor to the x of the next timed element");
    }

    @Test
    void harmonyBoostsRowAboveReserve() {
        LayoutOptions options = LayoutOptions.defaults();
        Score without = scoreWith(quarter(Step.C));
        Score with = scoreWith(bMaj7(), quarter(Step.C));

        double yWithout = new Engraver().layout(without, options).staves().get(0).y();
        double yWith = new Engraver().layout(with, options).staves().get(0).y();
        assertTrue(yWith > yWithout,
                "harmony must push the first staff down (with=" + yWith
                        + ", without=" + yWithout + ")");
    }

    @Test
    void harmonyAndDirectionAboveStack() {
        Direction words = new Direction(
                new DirectionType.Words("Andantino", false, true), Placement.ABOVE);
        Score score = scoreWith(words, bMaj7(), quarter(Step.C));

        LayoutResult layout = new Engraver().layout(score, LayoutOptions.defaults());
        TextPlacement chord = textsOf(layout, MarkingCategory.CHORD_SYMBOL).get(0);
        TextPlacement direction = textsOf(layout, MarkingCategory.DIRECTION).get(0);
        assertNotEquals(chord.y(), direction.y(),
                "chord symbol and direction must not sit at the same y");
    }

    @Test
    void harmonyUsesChordSymbolCategoryAndLeftAlign() {
        Score score = scoreWith(bMaj7(), quarter(Step.C));

        LayoutResult layout = new Engraver().layout(score, LayoutOptions.defaults());
        TextPlacement placement = textsOf(layout, MarkingCategory.CHORD_SYMBOL).get(0);
        assertEquals(MarkingCategory.CHORD_SYMBOL, placement.category());
        assertEquals(TextPlacement.Align.LEFT, placement.align());
    }

    @Test
    void harmonyRendersSlashChord() {
        Harmony slash = new Harmony(
                new Harmony.Root(Step.G, 0),
                HarmonyKind.DOMINANT_SEVENTH,
                Optional.of(new Harmony.Bass(Step.D, 0)),
                Optional.empty());
        Score score = scoreWith(slash, quarter(Step.C));

        LayoutResult layout = new Engraver().layout(score, LayoutOptions.defaults());
        TextPlacement placement = textsOf(layout, MarkingCategory.CHORD_SYMBOL).get(0);
        assertEquals("G7/D", placement.text());
    }
}
