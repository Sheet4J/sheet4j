package com.sheetmusic4j.engraving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sheetmusic4j.core.model.Attributes;
import com.sheetmusic4j.core.model.Clef;
import com.sheetmusic4j.core.model.Duration;
import com.sheetmusic4j.core.model.KeySignature;
import com.sheetmusic4j.core.model.Measure;
import com.sheetmusic4j.core.model.Note;
import com.sheetmusic4j.core.model.Part;
import com.sheetmusic4j.core.model.Pitch;
import com.sheetmusic4j.core.model.Score;
import com.sheetmusic4j.core.model.Step;
import com.sheetmusic4j.core.model.TimeSignature;
import com.sheetmusic4j.engraving.layout.LayoutOptions;
import com.sheetmusic4j.engraving.layout.LayoutResult;
import com.sheetmusic4j.engraving.layout.MeasureLayout;
import com.sheetmusic4j.engraving.layout.NoteAnchor;
import com.sheetmusic4j.engraving.layout.StaffLayout;

class NoteAnchorTest {

    private Note[] notes;

    private Score twoMeasureScale() {
        int divisions = 1;
        Step[] first = {Step.C, Step.D, Step.E, Step.F};
        Step[] second = {Step.G, Step.A, Step.B, Step.C};
        notes = new Note[8];

        Measure.Builder m1 = Measure.builder(1).attributes(Attributes.builder()
                .divisions(divisions)
                .keySignature(KeySignature.cMajor())
                .timeSignature(TimeSignature.fourFour())
                .clef(Clef.treble())
                .build());
        for (int i = 0; i < first.length; i++) {
            notes[i] = Note.builder().pitch(new Pitch(first[i], 4))
                    .duration(new Duration(1, divisions)).build();
            m1.addElement(notes[i]);
        }
        Measure.Builder m2 = Measure.builder(2);
        for (int i = 0; i < second.length; i++) {
            int octave = i == second.length - 1 ? 5 : 4;
            notes[first.length + i] = Note.builder().pitch(new Pitch(second[i], octave))
                    .duration(new Duration(1, divisions)).build();
            m2.addElement(notes[first.length + i]);
        }

        Part part = Part.builder("P1").name("Piano")
                .addMeasure(m1.build())
                .addMeasure(m2.build())
                .build();
        return Score.builder().addPart(part).build();
    }

    @Test
    void emitsOneAnchorPerNote() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        assertEquals(8, layout.noteAnchors().size(),
                "should have one anchor per note (no chords)");
        for (int i = 0; i < 8; i++) {
            assertSame(notes[i], layout.noteAnchors().get(i).elementRef(),
                    "anchor element ref must be identity-equal to the source Note");
        }
    }

    @Test
    void anchorsAreSortedByOnset() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        double prev = -1.0;
        for (NoteAnchor a : layout.noteAnchors()) {
            assertTrue(a.onsetQuarters() >= prev,
                    "anchors must be non-decreasing by onset");
            prev = a.onsetQuarters();
        }
    }

    @Test
    void anchorTimesAreConsistentWithMeasure() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        List<NoteAnchor> anchors = layout.noteAnchors();
        // 4/4, 1 quarter per note => onsets 0, 1, 2, 3, 4, 5, 6, 7
        for (int i = 0; i < 8; i++) {
            assertEquals((double) i, anchors.get(i).onsetQuarters(), 1e-9,
                    "onset " + i);
            assertEquals(1.0, anchors.get(i).durationQuarters(), 1e-9,
                    "duration " + i);
        }
    }

    @Test
    void anchorXMatchesNoteheadGlyphX() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        StaffLayout staff = layout.staves().get(0);
        // Collect notehead xs in order (they are added right after any
        // accidental; scan sequentially and take the notehead-shaped ones).
        int found = 0;
        for (var g : staff.glyphs()) {
            if (g.glyph().name().startsWith("NOTEHEAD_")) {
                assertEquals(g.x(), layout.noteAnchors().get(found).x(), 1e-9,
                        "anchor x must equal notehead glyph x for note " + found);
                found++;
            }
        }
        assertEquals(8, found);
    }

    @Test
    void totalDurationQuartersMatchesScoreLength() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        assertEquals(8.0, layout.totalDurationQuarters(), 1e-9);
    }

    @Test
    void xAtTimeIsMonotonic() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        double prev = Double.NEGATIVE_INFINITY;
        for (double t = 0; t <= 8; t += 0.25) {
            double x = layout.xAtTime(t);
            assertTrue(x >= prev, "xAtTime must be monotonic at t=" + t);
            prev = x;
        }
    }

    @Test
    void xAtTimeMatchesMeasureBoundaries() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        StaffLayout staff = layout.staves().get(0);
        List<MeasureLayout> measures = staff.measures();
        // t=0 lands inside measure 1
        assertEquals(measures.get(0).x(), layout.xAtTime(0.0), 1e-9);
        // t=4 is exactly the end of measure 1 / start of measure 2
        assertEquals(measures.get(0).right(), layout.xAtTime(4.0), 1e-6);
    }

    @Test
    void measureLayoutCarriesTimeInfo() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        StaffLayout staff = layout.staves().get(0);
        MeasureLayout m1 = staff.measures().get(0);
        MeasureLayout m2 = staff.measures().get(1);
        assertEquals(0.0, m1.startQuarters(), 1e-9);
        assertEquals(4.0, m1.endQuarters(), 1e-9);
        assertEquals(4.0, m2.startQuarters(), 1e-9);
        assertEquals(8.0, m2.endQuarters(), 1e-9);
    }

    @Test
    void anchorsCarryPartAndStaffIndex() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        for (NoteAnchor a : layout.noteAnchors()) {
            assertEquals(0, a.partIndex(), "single-part score has partIndex=0");
            assertEquals(0, a.staffIndex(), "single-staff part has staffIndex=0");
        }
    }

    @Test
    void anchorBoundingBoxIsNonEmpty() {
        LayoutResult layout = new Engraver().layout(twoMeasureScale(), LayoutOptions.defaults());
        for (NoteAnchor a : layout.noteAnchors()) {
            assertTrue(a.width() > 0, "anchor width must be positive");
            assertTrue(a.height() > 0, "anchor height must be positive");
            assertNotNull(a.elementRef());
        }
        assertFalse(layout.noteAnchors().isEmpty());
    }
}
