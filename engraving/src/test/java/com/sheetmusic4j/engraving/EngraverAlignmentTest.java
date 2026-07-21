package com.sheetmusic4j.engraving;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

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

class EngraverAlignmentTest {

    private static Score twoPartScore() {
        int divisions = 1;
        Measure.Builder p1m1 = Measure.builder(1).attributes(Attributes.builder()
                .divisions(divisions)
                .keySignature(KeySignature.cMajor())
                .timeSignature(TimeSignature.fourFour())
                .clef(Clef.treble())
                .build());
        p1m1.addElement(Note.builder().pitch(new Pitch(Step.C, 5)).duration(new Duration(4, divisions))
                .type(NoteType.WHOLE).build());
        Measure.Builder p1m2 = Measure.builder(2);
        p1m2.addElement(Note.builder().pitch(new Pitch(Step.D, 5)).duration(new Duration(4, divisions))
                .type(NoteType.WHOLE).build());

        Measure.Builder p2m1 = Measure.builder(1).attributes(Attributes.builder()
                .divisions(divisions)
                .keySignature(KeySignature.cMajor())
                .timeSignature(TimeSignature.fourFour())
                .clef(Clef.bass())
                .build());
        p2m1.addElement(Note.builder().pitch(new Pitch(Step.C, 3)).duration(new Duration(4, divisions))
                .type(NoteType.WHOLE).build());
        Measure.Builder p2m2 = Measure.builder(2);
        p2m2.addElement(Note.builder().pitch(new Pitch(Step.D, 3)).duration(new Duration(4, divisions))
                .type(NoteType.WHOLE).build());

        Part part1 = Part.builder("P1").addMeasure(p1m1.build()).addMeasure(p1m2.build()).build();
        Part part2 = Part.builder("P2").addMeasure(p2m1.build()).addMeasure(p2m2.build()).build();
        return Score.builder().addPart(part1).addPart(part2).build();
    }

    @Test
    void multiPartBarlinesAlign() {
        LayoutResult layout = new Engraver().layout(twoPartScore(), LayoutOptions.defaults());
        List<StaffLayout> staves = layout.staves();
        assertEquals(2, staves.size(), "expected exactly one staff per part on one system");

        StaffLayout top = staves.get(0);
        StaffLayout bottom = staves.get(1);
        assertEquals(top.measures().size(), bottom.measures().size(),
                "both parts must have the same number of measure layouts per system");
        for (int i = 0; i < top.measures().size(); i++) {
            double topBar = top.measures().get(i).right();
            double bottomBar = bottom.measures().get(i).right();
            assertEquals(topBar, bottomBar, 1e-6,
                    "measure " + i + " barline x-coord must match across parts (top=" + topBar
                            + ", bottom=" + bottomBar + ")");
        }
    }
}
