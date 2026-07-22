package com.sheetmusic4j.engraving.placement;

/**
 * A positioned slur connecting the first and last note of a phrase.
 * Rendered similarly to {@link TiePlacement} (a two-segment curve) but kept
 * as a distinct type since slurs and ties are semantically different
 * markings that may diverge in styling later.
 *
 * @param x1      x of the slur start
 * @param y1      y of the slur start
 * @param x2      x of the slur end
 * @param y2      y of the slur end
 * @param curveUp whether the slur's peak curves upward (false = downward)
 * @param clearY  the y the curve's peak must clear - the highest (if
 *                {@code curveUp}) or lowest notehead y among every note the
 *                slur spans, not just its two endpoints, so a phrase that
 *                arcs up to a peak and back down doesn't get a curve shallow
 *                enough to cut through that peak
 */
public record SlurPlacement(double x1, double y1, double x2, double y2, boolean curveUp, double clearY) {
}
