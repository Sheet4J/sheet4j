package com.sheetmusic4j.engraving.layout;

import com.sheetmusic4j.core.model.MusicElement;

/**
 * Identity + musical time + bounding box for a rendered
 * {@link MusicElement}. Used by callers (typically a playback view) to look
 * up "where on screen is this note?" and to place a playback cursor at a
 * given musical time.
 *
 * <p>{@code elementRef} is the same object instance that was placed inside
 * the source {@code Score}, so callers may use it as an
 * {@code IdentityHashMap} key.
 *
 * @param elementRef      the source {@link MusicElement} (note / rest /
 *                        chord) this anchor belongs to; usable with
 *                        identity semantics
 * @param partIndex       0-based index into {@code Score#parts()}
 * @param staffIndex      0-based staff index within the part (for a grand
 *                        staff, 0 is the treble staff, 1 the bass staff)
 * @param measureNumber   1-based measure number this element sits in
 * @param onsetQuarters   musical onset from the start of the score, in
 *                        quarter notes
 * @param durationQuarters length of the element in quarter notes; zero for
 *                        directions and other timeless elements
 * @param x               notehead / rest anchor x in layout units
 * @param y               notehead / rest anchor y in layout units
 * @param width           bounding-box width (accidental + notehead + flag)
 * @param height          bounding-box height (stem tip to opposite side of
 *                        the notehead)
 */
public record NoteAnchor(
        MusicElement elementRef,
        int partIndex,
        int staffIndex,
        int measureNumber,
        double onsetQuarters,
        double durationQuarters,
        double x,
        double y,
        double width,
        double height) {

    /** End of the element on the timeline, in quarter notes. */
    public double endQuarters() {
        return onsetQuarters + durationQuarters;
    }
}
