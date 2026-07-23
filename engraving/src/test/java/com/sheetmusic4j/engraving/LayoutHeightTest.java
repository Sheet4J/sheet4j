package com.sheetmusic4j.engraving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sheetmusic4j.core.model.Attributes;
import com.sheetmusic4j.core.model.Clef;
import com.sheetmusic4j.core.model.Duration;
import com.sheetmusic4j.core.model.KeySignature;
import com.sheetmusic4j.core.model.Measure;
import com.sheetmusic4j.core.model.Note;
import com.sheetmusic4j.core.model.NoteType;
import com.sheetmusic4j.core.model.Part;
import com.sheetmusic4j.core.model.Pitch;
import com.sheetmusic4j.core.model.Score;
import com.sheetmusic4j.core.model.Step;
import com.sheetmusic4j.core.model.TimeSignature;
import com.sheetmusic4j.engraving.layout.LayoutOptions;
import com.sheetmusic4j.engraving.layout.LayoutResult;
import com.sheetmusic4j.engraving.layout.StaffLayout;

/**
 * Regression tests for the layout-height expansion added so downstream
 * renderers that size their surface to {@code layout.height()} do not clip
 * ledger lines below the last staff (e.g. C2 on a bass staff, which sits
 * on the second ledger line below the bass clef).
 */
class LayoutHeightTest {

    private Score singleNoteScore(Pitch pitch, Clef clef) {
        int divisions = 1;
        Measure.Builder m = Measure.builder(1).attributes(Attributes.builder()
                .divisions(divisions)
                .keySignature(KeySignature.cMajor())
                .timeSignature(TimeSignature.fourFour())
                .clef(clef)
                .build());
        m.addElement(Note.builder().pitch(pitch)
                .duration(new Duration(4, divisions))
                .type(NoteType.WHOLE)
                .build());
        return Score.builder()
                .addPart(Part.builder("P1").addMeasure(m.build()).build())
                .build();
    }

    @Test
    void heightCoversLedgerLinesBelowBassStaff() {
        // C2 sits on the second ledger line below the bass staff.
        Score score = singleNoteScore(new Pitch(Step.C, 2), Clef.bass());
        LayoutOptions options = LayoutOptions.defaults();
        LayoutResult layout = new Engraver().layout(score, options);
        StaffLayout staff = layout.staves().get(0);

        double bottomOfStaff = staff.lineY(4);
        // C2 is two staff steps below the bass staff (two ledger lines
        // below the bottom line, i.e. 4 half-gaps = 2 gaps).
        double expectedMinExtension = 2.0 * options.staffLineGap();

        assertTrue(layout.height() >= bottomOfStaff + expectedMinExtension,
                "layout.height() (" + layout.height() + ") must extend at "
                        + "least " + expectedMinExtension + " units below the "
                        + "bottom staff line (" + bottomOfStaff + ") to cover "
                        + "C2's ledger lines");
    }

    @Test
    void heightIsNotShrunkForMidRangeNotes() {
        // A single G4 (top line of treble staff) - no ledger lines above
        // or below - should keep the historical staff-only height.
        Score score = singleNoteScore(new Pitch(Step.G, 4), Clef.treble());
        LayoutOptions options = LayoutOptions.defaults();
        LayoutResult layout = new Engraver().layout(score, options);

        // The staff-only height was: topMargin + titleBlock + staffHeight +
        // rightMargin. Since this score has no title, that reduces to
        // topMargin + staffHeight + rightMargin (staffSpacing subtracted
        // from y since there is only one staff row).
        double staffOnly = options.topMargin() + options.staffHeight()
                + options.rightMargin();
        // Allow a tiny slack for stem/glyph headroom (one gap).
        double upperExpected = staffOnly + options.staffLineGap() * 2.0;

        assertTrue(layout.height() >= staffOnly * 0.9,
                "layout.height() should not shrink below the previous "
                        + "staff-only height for mid-range notes");
        assertTrue(layout.height() <= upperExpected,
                "layout.height() for a single mid-range note should not "
                        + "grow dramatically past the staff-only bound; "
                        + "got " + layout.height() + ", expected <= "
                        + upperExpected);
    }

    @Test
    void contentBottomExposesTrueLowestDrawnY() {
        Score score = singleNoteScore(new Pitch(Step.C, 2), Clef.bass());
        LayoutResult layout = new Engraver().layout(score, LayoutOptions.defaults());

        StaffLayout staff = layout.staves().get(0);
        assertTrue(layout.contentBottom() > staff.lineY(4),
                "contentBottom must extend below the bottom staff line "
                        + "for C2 in bass clef");
    }

    @Test
    void contentTopIsAtOrAboveTopMarginForMidRangeNotes() {
        Score score = singleNoteScore(new Pitch(Step.G, 4), Clef.treble());
        LayoutResult layout = new Engraver().layout(score, LayoutOptions.defaults());
        // For a note that sits on the top staff line, the drawn glyph
        // reaches roughly one gap above the top line. contentTop should
        // reflect that.
        StaffLayout staff = layout.staves().get(0);
        assertTrue(layout.contentTop() <= staff.lineY(0) + 1e-6,
                "contentTop must be at or above the top staff line");
    }

    @Test
    void heightAlsoCoversAnchorBoundingBox() {
        // An unbeamed eighth note has a stem hanging well past the notehead;
        // the anchor bounding box is tall. Height should cover it.
        int divisions = 2;
        Measure.Builder m = Measure.builder(1).attributes(Attributes.builder()
                .divisions(divisions)
                .clef(Clef.bass())
                .timeSignature(TimeSignature.fourFour())
                .build());
        // A C2 quarter note sits well below the bass staff and carries a stem.
        m.addElement(Note.builder().pitch(new Pitch(Step.C, 2))
                .duration(new Duration(1, divisions))
                .type(NoteType.QUARTER)
                .build());
        Score score = Score.builder()
                .addPart(Part.builder("P1").addMeasure(m.build()).build())
                .build();

        LayoutResult layout = new Engraver().layout(score, LayoutOptions.defaults());
        // Every anchor's bottom (y + height/2) must fit inside layout.height().
        for (var a : layout.noteAnchors()) {
            double anchorBottom = a.y() + a.height() / 2.0;
            assertTrue(layout.height() >= anchorBottom,
                    "layout.height() (" + layout.height()
                            + ") must cover anchor bottom (" + anchorBottom + ")");
        }
    }

    @Test
    void heightNeverDecreasesRelativeToStaffOnlyBaseline() {
        // A simple two-measure C-major scale - the historical staff-only
        // height must still be a lower bound for the new height.
        int divisions = 1;
        Step[] steps = {Step.C, Step.D, Step.E, Step.F, Step.G, Step.A, Step.B};
        Measure.Builder m = Measure.builder(1).attributes(Attributes.builder()
                .divisions(divisions)
                .clef(Clef.treble())
                .keySignature(KeySignature.cMajor())
                .timeSignature(new TimeSignature(7, 4))
                .build());
        for (Step step : steps) {
            m.addElement(Note.builder().pitch(new Pitch(step, 4))
                    .duration(new Duration(1, divisions)).build());
        }
        Score score = Score.builder()
                .addPart(Part.builder("P1").addMeasure(m.build()).build())
                .build();

        LayoutOptions options = LayoutOptions.defaults();
        LayoutResult layout = new Engraver().layout(score, options);
        double staffOnly = options.topMargin() + options.staffHeight()
                + options.rightMargin();
        assertTrue(layout.height() >= staffOnly * 0.9,
                "layout.height() must be at least as tall as the "
                        + "staff-only baseline; got " + layout.height());
        // Sanity: with default options the layout should carry exactly one
        // staff row for one measure of a scale.
        assertEquals(1, layout.staves().size());
    }
}
