package com.sheetmusic4j.engraving.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LayoutOptionsBuilderTest {

    @Test
    void defaultsProduceReasonableValues() {
        LayoutOptions o = LayoutOptions.defaults();
        assertTrue(o.staffLineGap() > 0);
        assertTrue(o.systemWidth() > 0);
        assertTrue(o.showClef());
        assertTrue(o.showKeySignature());
        assertTrue(o.showTimeSignature());
        assertTrue(o.showTitleTexts());
        assertEquals(LayoutMode.PAGE, o.layoutMode());
    }

    @Test
    void builderOverridesFields() {
        LayoutOptions o = LayoutOptions.builder()
                .systemWidth(1234)
                .layoutMode(LayoutMode.STRIP)
                .showClef(false)
                .showKeySignature(false)
                .showTimeSignature(false)
                .showTitleTexts(false)
                .build();
        assertEquals(1234, o.systemWidth());
        assertEquals(LayoutMode.STRIP, o.layoutMode());
        assertFalse(o.showClef());
        assertFalse(o.showKeySignature());
        assertFalse(o.showTimeSignature());
        assertFalse(o.showTitleTexts());
    }

    @Test
    void toBuilderRoundTripsExistingValues() {
        LayoutOptions original = LayoutOptions.builder()
                .systemWidth(800)
                .fontSize(28)
                .build();
        LayoutOptions copy = original.toBuilder().leftMargin(50).build();
        assertEquals(800, copy.systemWidth());
        assertEquals(28, copy.fontSize());
        assertEquals(50, copy.leftMargin());
    }

    @Test
    void legacyEightArgConstructorStillWorks() {
        LayoutOptions o = new LayoutOptions(10, 60, 1000, 40, 20, 40, 120, 32);
        assertEquals(10, o.staffLineGap());
        assertEquals(1000, o.systemWidth());
        // Legacy constructor defaults new flags on so pre-existing callers
        // observe unchanged behaviour.
        assertTrue(o.showClef());
        assertEquals(LayoutMode.PAGE, o.layoutMode());
    }
}
