package com.sheetmusic4j.core.model;

import java.util.Locale;

/**
 * Chord-quality classification for a {@link Harmony} chord symbol. Mirrors the
 * subset of MusicXML {@code <kind>} values commonly seen in real-world scores.
 *
 * <p>Each constant carries two accessors:
 * <ul>
 *   <li>{@link #xmlValue()} — the hyphenated MusicXML spelling used inside
 *       {@code <kind>} element text (e.g. {@code "major-seventh"}).</li>
 *   <li>{@link #shortLabel()} — the printable shorthand used for engraved
 *       chord symbols when no explicit text override is present (e.g.
 *       {@code "maj7"}).</li>
 * </ul>
 *
 * <p>Unknown or unsupported MusicXML values map to {@link #OTHER} via
 * {@link #fromXml(String)}; the reader retains the raw text override in a
 * separate field so the display label still reads well.
 */
public enum HarmonyKind {
    /** Major triad (MusicXML {@code major}). */
    MAJOR("major", ""),
    /** Minor triad (MusicXML {@code minor}). */
    MINOR("minor", "m"),
    /** Dominant triad; MusicXML uses {@code dominant} as an alias of dominant-seventh. */
    DOMINANT("dominant", "7"),
    /** Diminished triad (MusicXML {@code diminished}). */
    DIMINISHED("diminished", "°"),
    /** Augmented triad (MusicXML {@code augmented}). */
    AUGMENTED("augmented", "+"),
    /** Major seventh chord (MusicXML {@code major-seventh}). */
    MAJOR_SEVENTH("major-seventh", "maj7"),
    /** Minor seventh chord (MusicXML {@code minor-seventh}). */
    MINOR_SEVENTH("minor-seventh", "m7"),
    /** Dominant seventh chord (MusicXML {@code dominant-seventh}). */
    DOMINANT_SEVENTH("dominant-seventh", "7"),
    /** Half-diminished seventh chord (MusicXML {@code half-diminished}). */
    HALF_DIMINISHED("half-diminished", "ø"),
    /** Diminished seventh chord (MusicXML {@code diminished-seventh}). */
    DIMINISHED_SEVENTH("diminished-seventh", "°7"),
    /** Major sixth chord (MusicXML {@code major-sixth}). */
    MAJOR_SIXTH("major-sixth", "6"),
    /** Minor sixth chord (MusicXML {@code minor-sixth}). */
    MINOR_SIXTH("minor-sixth", "m6"),
    /** Major ninth chord (MusicXML {@code major-ninth}). */
    MAJOR_NINTH("major-ninth", "maj9"),
    /** Minor ninth chord (MusicXML {@code minor-ninth}). */
    MINOR_NINTH("minor-ninth", "m9"),
    /** Dominant ninth chord (MusicXML {@code dominant-ninth}). */
    DOMINANT_NINTH("dominant-ninth", "9"),
    /** Suspended fourth chord (MusicXML {@code suspended-fourth}). */
    SUSPENDED_FOURTH("suspended-fourth", "sus4"),
    /** Suspended second chord (MusicXML {@code suspended-second}). */
    SUSPENDED_SECOND("suspended-second", "sus2"),
    /** Power (root+fifth) chord (MusicXML {@code power}). */
    POWER("power", "5"),
    /** Explicit "no chord" marker (MusicXML {@code none}). */
    NONE("none", "N.C."),
    /** Fallback for kinds not modelled explicitly. */
    OTHER("other", "");

    private final String xmlValue;
    private final String shortLabel;

    HarmonyKind(String xmlValue, String shortLabel) {
        this.xmlValue = xmlValue;
        this.shortLabel = shortLabel;
    }

    /**
     * MusicXML {@code <kind>} spelling for this constant (hyphenated form).
     *
     * @return the MusicXML string, never {@code null}
     */
    public String xmlValue() {
        return xmlValue;
    }

    /**
     * Printable shorthand used as the default rendering of the chord quality
     * when the source does not carry an explicit {@code text} override on the
     * {@code <kind>} element.
     *
     * @return the shorthand string, may be empty (e.g. plain major triad)
     */
    public String shortLabel() {
        return shortLabel;
    }

    /**
     * Parse a MusicXML {@code <kind>} body value into the matching constant.
     * Whitespace and case are tolerated; unknown values fall back to
     * {@link #OTHER} so the reader stays lenient with real-world inputs.
     *
     * @param value the raw MusicXML text (may be {@code null})
     * @return the matching constant, or {@link #OTHER} for unknown inputs
     */
    public static HarmonyKind fromXml(String value) {
        if (value == null) {
            return OTHER;
        }
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        if (normalised.isEmpty()) {
            return OTHER;
        }
        for (HarmonyKind kind : values()) {
            if (kind.xmlValue.equals(normalised)) {
                return kind;
            }
        }
        return OTHER;
    }
}
