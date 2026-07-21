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

class EngraverSystemBreakTest {

    private static Score twentyMeasureScore() {
        int divisions = 1;
        Part.Builder part = Part.builder("P1");
        for (int i = 0; i < 20; i++) {
            Measure.Builder m = Measure.builder(i + 1);
            if (i == 0) {
                m.attributes(Attributes.builder()
                        .divisions(divisions)
                        .keySignature(KeySignature.cMajor())
                        .timeSignature(TimeSignature.fourFour())
                        .clef(Clef.treble())
                        .build());
            }
            m.addElement(Note.builder().pitch(new Pitch(Step.C, 4)).duration(new Duration(4, divisions))
                    .type(NoteType.WHOLE).build());
            part.addMeasure(m.build());
        }
        return Score.builder().addPart(part.build()).build();
    }

    @Test
    void narrowSystemProducesMultipleRows() {
        LayoutOptions defaults = LayoutOptions.defaults();
        LayoutOptions narrow = new LayoutOptions(
                defaults.staffLineGap(),
                defaults.staffSpacing(),
                600, // narrow enough that 20 measures cannot fit on one row
                defaults.leftMargin(),
                defaults.rightMargin(),
                defaults.topMargin(),
                defaults.measureMinWidth(),
                defaults.fontSize());

        LayoutResult layout = new Engraver().layout(twentyMeasureScore(), narrow);

        assertTrue(layout.systems().size() > 1,
                "expected multiple systems, got " + layout.systems().size());
        long distinctY = layout.staves().stream()
                .map(StaffLayout::y)
                .distinct()
                .count();
        assertTrue(distinctY > 1, "expected staves at multiple y positions, got " + distinctY);
    }

    @Test
    void newSystemRepeatsClef() {
        LayoutOptions defaults = LayoutOptions.defaults();
        LayoutOptions narrow = new LayoutOptions(
                defaults.staffLineGap(),
                defaults.staffSpacing(),
                600,
                defaults.leftMargin(),
                defaults.rightMargin(),
                defaults.topMargin(),
                defaults.measureMinWidth(),
                defaults.fontSize());

        LayoutResult layout = new Engraver().layout(twentyMeasureScore(), narrow);

        // Every system's staff should emit its clef at the leftmost measure.
        for (SystemLayout system : layout.systems()) {
            for (StaffLayout staff : system.staves()) {
                long clefs = staff.glyphs().stream()
                        .filter(g -> g.glyph() == Glyph.CLEF_G
                                || g.glyph() == Glyph.CLEF_F
                                || g.glyph() == Glyph.CLEF_C)
                        .count();
                assertTrue(clefs >= 1,
                        "each system must emit at least one clef, got " + clefs);
            }
        }
    }

    @Test
    void shortScoreFitsOnOneSystem() {
        int divisions = 1;
        Part.Builder part = Part.builder("P1");
        Measure.Builder m = Measure.builder(1)
                .attributes(Attributes.builder()
                        .divisions(divisions)
                        .keySignature(KeySignature.cMajor())
                        .timeSignature(TimeSignature.fourFour())
                        .clef(Clef.treble())
                        .build());
        m.addElement(Note.builder().pitch(new Pitch(Step.C, 4)).duration(new Duration(4, divisions))
                .type(NoteType.WHOLE).build());
        part.addMeasure(m.build());
        Score score = Score.builder().addPart(part.build()).build();

        LayoutResult layout = new Engraver().layout(score, LayoutOptions.defaults());
        assertEquals(1, layout.systems().size(),
                "single-measure score should fit in one system");
    }
    }
