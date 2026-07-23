package com.sheetmusic4j.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PitchTest {

    @Test
    void middleCIsMidi60() {
        assertEquals(60, new Pitch(Step.C, 4).toMidiNumber());
    }

    @Test
    void a4IsMidi69() {
        assertEquals(69, new Pitch(Step.A, 4).toMidiNumber());
    }

    @Test
    void sharpRaisesBySemitone() {
        assertEquals(61, new Pitch(Step.C, 4, 1).toMidiNumber());
    }

    @Test
    void roundTripThroughMidi() {
        for (int midi = 21; midi <= 108; midi++) {
            assertEquals(midi, Pitch.fromMidiNumber(midi).toMidiNumber());
        }
    }

    @Test
    void keyAwareRoundTripsThroughMidi() {
        KeySignature[] keys = {
                KeySignature.cMajor(),
                new KeySignature(3),   // A major (sharp key)
                new KeySignature(-3),  // E-flat major (flat key)
                new KeySignature(-4),  // A-flat major (flat key)
        };
        for (KeySignature key : keys) {
            for (int midi = 21; midi <= 108; midi++) {
                Pitch p = Pitch.fromMidiNumber(midi, key);
                assertEquals(midi, p.toMidiNumber(),
                        "round trip must preserve midi in key " + key.fifths()
                                + " for midi " + midi);
            }
        }
    }

    @Test
    void flatKeyPrefersFlats() {
        KeySignature eFlat = new KeySignature(-3);
        // B-flat 4 = MIDI 70. In a flat key it should spell as B♭, not A♯.
        Pitch p = Pitch.fromMidiNumber(70, eFlat);
        assertEquals(Step.B, p.step());
        assertEquals(-1, p.alter());
    }

    @Test
    void sharpKeyPrefersSharps() {
        KeySignature aMajor = new KeySignature(3);
        // F-sharp 4 = MIDI 66. In a sharp key it should spell as F♯, not G♭.
        Pitch p = Pitch.fromMidiNumber(66, aMajor);
        assertEquals(Step.F, p.step());
        assertEquals(1, p.alter());
    }

    @Test
    void nullKeyDefaultsToSharps() {
        Pitch p = Pitch.fromMidiNumber(70, null);
        assertEquals(Step.A, p.step());
        assertEquals(1, p.alter());
    }
    }
