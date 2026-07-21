package com.sheetmusic4j.core.model;

/**
 * A single lyric syllable attached to a {@link Note}. Corresponds to the
 * MusicXML {@code <lyric>} element.
 *
 * @param text     the syllable text (never {@code null}; may be empty)
 * @param syllabic the syllable's position within its word (never
 *                 {@code null}; defaults to {@link Syllabic#SINGLE})
 * @param verse    the 1-based verse number (values &lt; 1 are clamped to 1)
 */
public record Lyric(String text, Syllabic syllabic, int verse) {

    public Lyric {
        if (text == null) {
            text = "";
        }
        if (syllabic == null) {
            syllabic = Syllabic.SINGLE;
        }
        if (verse < 1) {
            verse = 1;
        }
    }
}
