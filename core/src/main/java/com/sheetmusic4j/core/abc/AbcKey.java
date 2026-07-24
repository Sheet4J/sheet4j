package com.sheetmusic4j.core.abc;

import java.util.Locale;

import com.sheetmusic4j.core.model.KeySignature;
import com.sheetmusic4j.core.model.Step;

/**
 * Parses the value of an ABC {@code K:} info-field into a {@link KeySignature}
 * (expressed as the MusicXML {@code fifths} value).
 *
 * <p>Only the key/mode portion is interpreted — trailing tokens like
 * {@code clef=treble} or explicit accidental lists ({@code ^f}) are ignored
 * silently for MVP.
 */
final class AbcKey {

    /** Order of sharps in a key signature: F, C, G, D, A, E, B. */
    private static final Step[] SHARP_ORDER = {Step.F, Step.C, Step.G, Step.D, Step.A, Step.E, Step.B};
    /** Order of flats in a key signature: B, E, A, D, G, C, F. */
    private static final Step[] FLAT_ORDER = {Step.B, Step.E, Step.A, Step.D, Step.G, Step.C, Step.F};

    private AbcKey() {
    }

    /**
     * The chromatic alteration ({@code +1}, {@code 0}, {@code -1}) implied by
     * the given key signature for the given diatonic step.
     */
    static int alterFor(Step step, KeySignature key) {
        if (key == null || key.fifths() == 0) {
            return 0;
        }
        if (key.fifths() > 0) {
            for (int i = 0; i < key.fifths() && i < SHARP_ORDER.length; i++) {
                if (SHARP_ORDER[i] == step) {
                    return 1;
                }
            }
            return 0;
        }
        for (int i = 0; i < -key.fifths() && i < FLAT_ORDER.length; i++) {
            if (FLAT_ORDER[i] == step) {
                return -1;
            }
        }
        return 0;
    }

    /** Fifths values for the twelve+ diatonic major keys. */
    private static int majorFifths(String root) {
        return switch (root) {
            case "CB" -> -7;
            case "GB" -> -6;
            case "DB" -> -5;
            case "AB" -> -4;
            case "EB" -> -3;
            case "BB" -> -2;
            case "F" -> -1;
            case "C" -> 0;
            case "G" -> 1;
            case "D" -> 2;
            case "A" -> 3;
            case "E" -> 4;
            case "B" -> 5;
            case "F#" -> 6;
            case "C#" -> 7;
            default -> throw new AbcException("Unknown key root: " + root);
        };
    }

    /**
     * Parse the raw text after {@code K:}. Accepts inputs such as:
     * {@code C}, {@code Gmaj}, {@code Am}, {@code Dmix}, {@code Bb}, {@code F#dor}.
     * Returns {@code null} for {@code K:none} or {@code K:} with no root.
     */
    static KeySignature parse(String raw) {
        if (raw == null) {
            return KeySignature.cMajor();
        }
        String trimmed = raw.trim();
        // Strip trailing directives like " clef=treble", "exp _b", or "^f".
        int space = trimmed.indexOf(' ');
        if (space > 0) {
            trimmed = trimmed.substring(0, space);
        }
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("none")) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);

        // Extract root: 1 or 2 chars ("C", "C#", "Db", "Bb", ...)
        int rootLen = 1;
        if (upper.length() >= 2) {
            char second = upper.charAt(1);
            if (second == '#' || second == 'B') {
                rootLen = 2;
            }
        }
        String root = upper.substring(0, rootLen);
        String modeRaw = upper.substring(rootLen);

        // Normalise mode to first-three-letters convention used by ABC.
        int modeOffset;
        String mode = modeRaw.length() >= 3 ? modeRaw.substring(0, 3) : modeRaw;
        modeOffset = switch (mode) {
            case "", "MAJ", "ION" -> 0;
            case "MIN", "AEO", "M" -> -3;
            case "DOR" -> -2;
            case "PHR" -> -4;
            case "LYD" -> 1;
            case "MIX" -> -1;
            case "LOC" -> -5;
            default -> {
                // Single-letter minor ("Am", "Bm") — mode string is "M" only.
                if (modeRaw.equals("M")) {
                    yield -3;
                }
                yield 0;
            }
        };

        int rootFifths;
        try {
            rootFifths = majorFifths(root);
        } catch (AbcException e) {
            // Unknown key roots (e.g. K:HP, K:Hp) fall back to C major.
            return KeySignature.cMajor();
        }
        int fifths = rootFifths + modeOffset;
        // Wrap into the standard -7..+7 range using enharmonic equivalents.
        while (fifths > 7) {
            fifths -= 12;
        }
        while (fifths < -7) {
            fifths += 12;
        }
        return new KeySignature(fifths);
    }
}
