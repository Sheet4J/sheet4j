package com.sheetmusic4j.core.model;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class HarmonyTest {

    @Test
    void displayLabelPrefersTextOverride() {
        Harmony harmony = new Harmony(
                new Harmony.Root(Step.B, 0),
                HarmonyKind.MAJOR_SEVENTH,
                Optional.empty(),
                Optional.of("Maj7"));
        assertEquals("BMaj7", harmony.displayLabel(),
                "text override must win over kind.shortLabel()");
    }

    @Test
    void displayLabelUsesUnicodeAccidentals() {
        Harmony sharp = new Harmony(
                new Harmony.Root(Step.D, 1),
                HarmonyKind.MINOR_NINTH,
                Optional.empty(),
                Optional.empty());
        assertTrue(sharp.displayLabel().startsWith("D\u266F"),
                "expected D-sharp prefix, got: " + sharp.displayLabel());

        Harmony flat = new Harmony(
                new Harmony.Root(Step.E, -1),
                HarmonyKind.MINOR,
                Optional.empty(),
                Optional.empty());
        assertTrue(flat.displayLabel().startsWith("E\u266D"),
                "expected E-flat prefix, got: " + flat.displayLabel());
    }

    @Test
    void displayLabelIncludesSlashBass() {
        Harmony harmony = new Harmony(
                new Harmony.Root(Step.G, 0),
                HarmonyKind.DOMINANT_SEVENTH,
                Optional.of(new Harmony.Bass(Step.D, 0)),
                Optional.empty());
        assertEquals("G7/D", harmony.displayLabel());
    }

    @Test
    void displayLabelFallsBackToKindShortLabel() {
        Harmony harmony = new Harmony(
                new Harmony.Root(Step.C, 0),
                HarmonyKind.MINOR_SEVENTH,
                Optional.empty(),
                Optional.empty());
        assertEquals("Cm7", harmony.displayLabel());
    }

    @Test
    void displayLabelHandlesBassAccidental() {
        Harmony harmony = new Harmony(
                new Harmony.Root(Step.B, 0),
                HarmonyKind.MAJOR_SEVENTH,
                Optional.of(new Harmony.Bass(Step.D, 1)),
                Optional.of("Maj7"));
        assertEquals("BMaj7/D\u266F", harmony.displayLabel());
    }

    @Test
    void durationIsZero() {
        Harmony harmony = new Harmony(
                new Harmony.Root(Step.C, 0),
                HarmonyKind.MAJOR,
                Optional.empty(),
                Optional.empty());
        assertTrue(harmony.duration().isZero());
    }

    @Test
    void harmonyKindFromXmlKnown() {
        assertEquals(HarmonyKind.MAJOR_SEVENTH, HarmonyKind.fromXml("major-seventh"));
        assertEquals(HarmonyKind.SUSPENDED_FOURTH, HarmonyKind.fromXml("suspended-fourth"));
        assertEquals(HarmonyKind.MINOR_NINTH, HarmonyKind.fromXml("MINOR-NINTH"));
        assertEquals(HarmonyKind.MINOR, HarmonyKind.fromXml("  minor  "));
    }

    @Test
    void harmonyKindFromXmlUnknownIsOther() {
        assertEquals(HarmonyKind.OTHER, HarmonyKind.fromXml("no-such-kind"));
        assertEquals(HarmonyKind.OTHER, HarmonyKind.fromXml(null));
        assertEquals(HarmonyKind.OTHER, HarmonyKind.fromXml(""));
    }

    @Test
    void harmonyDefaultsNullOptionalsToEmpty() {
        Harmony harmony = new Harmony(new Harmony.Root(Step.C, 0), HarmonyKind.MAJOR, null, null);
        assertFalse(harmony.bass().isPresent());
        assertFalse(harmony.textOverride().isPresent());
    }
}
