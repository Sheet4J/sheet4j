package com.sheetmusic4j.core.model;

import java.util.Locale;

/**
 * Position of a syllable within a lyric word, corresponding to the MusicXML
 * {@code <syllabic>} element attached to {@code <lyric>}.
 */
public enum Syllabic {
    /** Stand-alone syllable — a complete word. */
    SINGLE,
    /** First syllable of a multi-syllable word. */
    BEGIN,
    /** Interior syllable of a multi-syllable word. */
    MIDDLE,
    /** Last syllable of a multi-syllable word. */
    END;

    /**
     * MusicXML lowercase form used inside {@code <syllabic>}.
     */
    public String xmlValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parse a MusicXML {@code <syllabic>} text value.
     *
     * @param value the element text (case-insensitive); {@code null}, blank
     *              or unknown values fall back to {@link #SINGLE}
     * @return the matching syllabic, or {@link #SINGLE} as a lenient default
     */
    public static Syllabic fromXml(String value) {
        if (value == null) {
            return SINGLE;
        }
        String v = value.trim().toUpperCase(Locale.ROOT);
        if (v.isEmpty()) {
            return SINGLE;
        }
        for (Syllabic s : values()) {
            if (s.name().equals(v)) {
                return s;
            }
        }
        return SINGLE;
    }
}
