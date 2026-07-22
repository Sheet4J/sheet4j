package com.sheetmusic4j.engraving.placement;

/**
 * A positioned note stem, drawn as a straight vertical line from the
 * notehead-side endpoint ({@code y1}) to the tip ({@code y2}).
 *
 * <p>Unlike a fixed-length glyph, this carries its own endpoints so a
 * beamed note's stem can be lengthened to reach the beam's actual height
 * (shared across every note in the beam run and clamped to clear the
 * staff), rather than always extending a standard distance from its own
 * notehead.
 *
 * @param x  horizontal position
 * @param y1 y at the notehead side
 * @param y2 y at the tip (beam/flag side)
 */
public record StemPlacement(double x, double y1, double y2) {
}
