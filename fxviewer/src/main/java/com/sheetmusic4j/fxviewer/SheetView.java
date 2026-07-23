package com.sheetmusic4j.fxviewer;

import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Optional;

import com.sheetmusic4j.core.model.MusicElement;
import com.sheetmusic4j.core.model.Score;
import com.sheetmusic4j.engraving.Engraver;
import com.sheetmusic4j.engraving.layout.LayoutOptions;
import com.sheetmusic4j.engraving.layout.LayoutResult;
import com.sheetmusic4j.engraving.glyph.MarkingCategory;

import com.sheetmusic4j.engraving.placement.BracketPlacement;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * A JavaFX control that renders a {@link Score}. It engraves the score into a
 * {@link LayoutResult} via {@link Engraver} and draws it on a {@link Canvas}.
 *
 * <p>The view is <em>content-sized</em>: after each engrave, the underlying
 * canvas and the region's preferred/min/max sizes track {@link LayoutResult}'s
 * width and height. That way, when the view is wrapped in a
 * {@code ScrollPane}, the pane sees the real content size and shows scrollbars
 * whenever the content is larger than the viewport.
 *
 * <p>Callers can override the engraving width via {@link #setSystemWidth(double)}
 * (or the {@link #systemWidthProperty()} JavaFX property, e.g. by binding it to
 * a container's width). The default is {@link LayoutOptions#defaults()}
 * {@code .systemWidth()}. Callers can also scale the rendered result via
 * {@link #setZoom(double)}.
 *
 * <p>For per-note highlighting during playback, mutate the
 * {@link #noteHighlights()} map: adding {@code (element, color)} pairs
 * repaints the view without re-engraving, so highlights are cheap enough to
 * drive from real-time MIDI events.
 */
public final class SheetView extends Region {

    private static final double FALLBACK_HEIGHT = LayoutOptions.defaults().staffHeight()
            + LayoutOptions.defaults().topMargin() * 2;

    private final Canvas canvas = new Canvas();
    private final Engraver engraver = new Engraver();
    private final ScoreRenderer renderer = new ScoreRenderer();

    private final DoubleProperty systemWidth =
            new SimpleDoubleProperty(this, "systemWidth", LayoutOptions.defaults().systemWidth());

    private final DoubleProperty zoom = new SimpleDoubleProperty(this, "zoom", 1.0);

    private final ObservableSet<MarkingCategory> hiddenTextCategories =
            FXCollections.observableSet(EnumSet.noneOf(MarkingCategory.class));

    private final BooleanProperty bracketsVisible =
            new SimpleBooleanProperty(this, "bracketsVisible", true);

    /**
     * Live map of per-element highlight colours. Uses identity comparisons
     * on keys so a caller can hold the exact {@link MusicElement} instance
     * returned by the model builder as the map key. Mutations trigger a
     * cheap {@link #repaint()} - no re-engrave.
     */
    private final ObservableMap<MusicElement, Color> noteHighlights =
            FXCollections.observableMap(new IdentityHashMap<>());

    private Score score;
    private LayoutResult layout;

    /** Creates an empty score view at the default engraving width. */
    public SheetView() {
        getChildren().add(canvas);
        systemWidth.addListener((obs, oldV, newV) -> rebuild());
        zoom.addListener((obs, oldV, newV) -> rebuild());
        hiddenTextCategories.addListener((SetChangeListener<MarkingCategory>) change -> repaint());
        bracketsVisible.addListener((obs, oldV, newV) -> repaint());
        noteHighlights.addListener((MapChangeListener<MusicElement, Color>) change -> repaint());
        renderer.setNoteColorProvider(this::highlightFor);
        // Initial empty canvas at the default width; setScore replaces it.
        canvas.setWidth(systemWidth.get() * zoom.get());
        canvas.setHeight(FALLBACK_HEIGHT * zoom.get());
        setMinSize(200, 120);
        setPrefSize(canvas.getWidth(), canvas.getHeight());
    }

    /** Sets the score to display and rebuilds the engraved layout. */
    public void setScore(Score score) {
        this.score = score;
        rebuild();
    }

    /** Returns the score currently displayed by this view, or {@code null}. */
    public Score getScore() {
        return score;
    }

    /**
     * The most recently engraved layout for the current score, or
     * {@code null} when no score has been set. Callers building playback
     * cursors read anchors and call {@link LayoutResult#xAtTime(double)}
     * from this result.
     */
    public LayoutResult getLayout() {
        return layout;
    }

    /**
     * The system width used by the engraver. Changing this triggers a rebuild.
     * Callers can bind this to a container's width (e.g., the ScrollPane
     * viewport) to make the score reflow while still relying on the layout
     * to report the actual content size.
     *
     * @return the writable width property used by the engraver
     */
    public DoubleProperty systemWidthProperty() {
        return systemWidth;
    }

    /** Returns the current system width used by the engraver. */
    public double getSystemWidth() {
        return systemWidth.get();
    }

    /** Updates the system width used by the engraver, if the width is positive. */
    public void setSystemWidth(double width) {
        if (width > 0) {
            systemWidth.set(width);
        }
    }

    /**
     * Render zoom factor. Values above 1 enlarge the score; values between 0
     * and 1 shrink it. Changing this triggers a rebuild.
     */
    public DoubleProperty zoomProperty() {
        return zoom;
    }

    /** @return the current render zoom factor. */
    public double getZoom() {
        return zoom.get();
    }

    /** Update the render zoom factor, if positive. */
    public void setZoom(double factor) {
        if (factor > 0) {
            zoom.set(factor);
        }
    }

    /**
     * Live-observable set of {@link MarkingCategory categories} that this
     * view should hide. Mutations trigger a rebuild.
     *
     * @return the observable set (never {@code null})
     */
    public ObservableSet<MarkingCategory> hiddenTextCategoriesProperty() {
        return hiddenTextCategories;
    }

    /**
     * JavaFX property controlling whether {@link BracketPlacement
     * bracket placements} (both implicit grand-staff braces and
     * {@code <part-group>}-driven brackets) are drawn. Defaults to
     * {@code true}; mutations trigger a repaint (no re-engrave).
     *
     * @return the writable brackets-visible property
     */
    public BooleanProperty bracketsVisibleProperty() {
        return bracketsVisible;
    }

    /** @return {@code true} when brackets are currently drawn. */
    public boolean isBracketsVisible() {
        return bracketsVisible.get();
    }

    /** Update the bracket visibility flag, triggering a repaint. */
    public void setBracketsVisible(boolean visible) {
        bracketsVisible.set(visible);
    }

    /**
     * Live-observable per-element highlight colour map. Add entries to
     * tint the notehead + stem + flag + accidental of individual notes
     * (looked up by identity), and remove entries to clear them. Every
     * mutation triggers a {@link #repaint()} but not a re-engrave, so
     * this is inexpensive even at real-time MIDI event rates.
     *
     * @return the observable map (never {@code null})
     */
    public ObservableMap<MusicElement, Color> noteHighlights() {
        return noteHighlights;
    }

    /**
     * Look up the highlight colour for the given source element, if any.
     * Wrapped as a {@link RenderColor} so the surface-agnostic
     * {@link ScorePainter} can apply it without dragging in JavaFX.
     */
    private Optional<RenderColor> highlightFor(MusicElement element) {
        Color c = noteHighlights.get(element);
        if (c == null) {
            return Optional.empty();
        }
        return Optional.of(new RenderColor(
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255)));
    }

    /**
     * Re-engrave the current score (produces a fresh {@link LayoutResult}),
     * resize the canvas to the layout's content extent, and repaint. Called
     * only when a score/layout knob changes; per-note highlight changes go
     * through {@link #repaint()} instead.
     */
    private void rebuild() {
        double zoomFactor = Math.max(zoom.get(), 0.01);
        if (score == null) {
            layout = null;
            canvas.setWidth(systemWidth.get() * zoomFactor);
            canvas.setHeight(FALLBACK_HEIGHT * zoomFactor);
            canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            layout = engraver.layout(score, layoutOptions());
            canvas.setWidth(Math.max(layout.width() * zoomFactor, 1.0));
            canvas.setHeight(Math.max(layout.height() * zoomFactor, 1.0));
            paintCurrent();
        }
        setPrefSize(canvas.getWidth(), canvas.getHeight());
        setMinSize(Math.min(200, canvas.getWidth()), Math.min(120, canvas.getHeight()));
        setMaxSize(canvas.getWidth(), canvas.getHeight());
        requestLayout();
        if (getParent() != null) {
            getParent().requestLayout();
        }
    }

    /**
     * Redraw the cached layout without re-engraving. Used when only
     * per-note highlights or the bracket visibility flag change.
     */
    private void repaint() {
        if (layout == null) {
            return;
        }
        paintCurrent();
    }

    private void paintCurrent() {
        double zoomFactor = Math.max(zoom.get(), 0.01);
        renderer.setHiddenTextCategories(hiddenTextCategories);
        renderer.setBracketsVisible(bracketsVisible.get());
        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();
        gc.scale(zoomFactor, zoomFactor);
        renderer.render(gc, layout, layout.width(), layout.height());
        gc.restore();
    }

    @Override
    protected void layoutChildren() {
        canvas.relocate(0, 0);
    }

    @Override
    protected double computePrefWidth(double height) {
        return canvas.getWidth();
    }

    @Override
    protected double computePrefHeight(double width) {
        return canvas.getHeight();
    }

    @Override
    protected double computeMinWidth(double height) {
        return Math.min(200, canvas.getWidth());
    }

    @Override
    protected double computeMinHeight(double width) {
        return Math.min(120, canvas.getHeight());
    }

    @Override
    protected double computeMaxWidth(double height) {
        return canvas.getWidth();
    }

    @Override
    protected double computeMaxHeight(double width) {
        return canvas.getHeight();
    }

    private LayoutOptions layoutOptions() {
        LayoutOptions defaults = LayoutOptions.defaults();
        double width = systemWidth.get() > 0 ? systemWidth.get() : defaults.systemWidth();
        return defaults.toBuilder().systemWidth(width).build();
    }
}
