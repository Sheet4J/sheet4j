package com.sheetmusic4j.fxdemo.reference;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Renders a MusicXML string into a {@link BufferedImage} by driving a JavaFX
 * {@link WebView} that hosts <a href="https://opensheetmusicdisplay.org/">OpenSheetMusicDisplay</a>.
 *
 * <p>Mirrors the pattern used by lottie4j's {@code WebViewScreenshotGenerator}:
 * bootstrap the JavaFX toolkit, load a small local HTML page, feed data into it
 * via {@link javafx.scene.web.WebEngine#executeScript(String)}, wait for a
 * status signal (here via {@code document.title}) and finally snapshot the
 * WebView node.
 *
 * <p>This class is intentionally tolerant: if JavaFX-web is missing, the
 * OSMD bundle is not committed, or a headless display cannot be opened, it
 * returns a {@link Result#missing()} / {@link Result#error(String)} rather than
 * throwing. Callers - both tests and the demo Diff tab - can then require
 * a real result before comparing, for example with JUnit's
 * {@code Assumptions.assumeTrue(...)} in tests.
 */
public final class WebViewReferenceRenderer {

    /**
     * Sentinel value the HTML page sets on {@code document.title} once OSMD has
     * finished laying out the score.
     */
    private static final String TITLE_DONE = "sheet4j:done";
    private static final String TITLE_MISSING = "sheet4j:missing";
    private static final String TITLE_ERROR_PREFIX = "sheet4j:error";

    private static final AtomicBoolean TOOLKIT_STARTED = new AtomicBoolean(false);

    /**
     * Outcome of a reference render.
     *
     * @param image    the rendered image, or {@code null} if unavailable
     * @param missing  {@code true} when OSMD/JavaFX-web is not usable in this environment
     * @param errorMsg human-readable error message, or {@code null} on success
     */
    public record Result(BufferedImage image, boolean missing, String errorMsg) {
        /**
         * Returns whether the render produced an image successfully.
         *
         * @return {@code true} when an image is available and no error was reported
         */
        public boolean isSuccess() {
            return image != null && errorMsg == null && !missing;
        }

        /**
         * Creates a successful render result.
         *
         * @param image rendered image
         * @return success result carrying the rendered image
         */
        public static Result success(BufferedImage image) {
            return new Result(image, false, null);
        }

        /**
         * Creates a result indicating OSMD or JavaFX-web is unavailable.
         *
         * @return unavailable result
         */
        public static Result unavailable() {
            return new Result(null, true, "OSMD bundle not present or JavaFX-web unavailable");
        }

        /**
         * Creates a failed render result with a message.
         *
         * @param message human-readable error description
         * @return error result
         */
        public static Result error(String message) {
            return new Result(null, false, message);
        }
    }

    private final long timeoutMillis;

    /**
     * Creates a renderer with the default timeout.
     */
    public WebViewReferenceRenderer() {
        this(20_000L);
    }

    /**
     * Creates a renderer with a custom timeout.
     *
     * @param timeoutMillis maximum time to wait for OSMD rendering
     */
    public WebViewReferenceRenderer(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Render the given MusicXML string in an off-screen WebView + OSMD and snapshot
     * the result. Blocks up to the configured timeout waiting for OSMD to signal
     * completion via {@code document.title}.
     *
     * @param musicXml MusicXML content to render
     * @param widthPx  target render width in pixels
     * @param heightPx target render height in pixels
     * @return successful, unavailable, or failed render result
     */
    public Result render(String musicXml, int widthPx, int heightPx) {
        URL page = getClass().getResource("/reference/osmd/index.html");
        if (page == null) {
            return Result.unavailable();
        }
        URL bundle = getClass().getResource("/reference/osmd/opensheetmusicdisplay.min.js");
        if (bundle == null) {
            return Result.unavailable();
        }

        try {
            ensureToolkit();
        } catch (Throwable t) {
            return Result.error("JavaFX toolkit not available: " + t.getMessage());
        }

        CompletableFuture<Result> future = new CompletableFuture<>();
        Platform.runLater(() -> renderOnFxThread(page.toExternalForm(), musicXml, widthPx, heightPx, future));
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            return Result.error("Timed out after " + timeoutMillis + " ms waiting for OSMD");
        } catch (Exception e) {
            return Result.error(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * The WebView is created much taller than the caller's requested height so
     * that OSMD, which lays out an arbitrary number of systems, has room to
     * render its full SVG. The Java side then queries the actual content
     * height from JavaScript and crops the snapshot down to a tight box.
     */
    private static final int WEBVIEW_HEIGHT = 6000;

    private void renderOnFxThread(String pageUrl, String musicXml, int widthPx, int heightPx,
                                  CompletableFuture<Result> future) {
        WebView view;
        Stage stage;
        try {
            int viewHeight = Math.max(heightPx, WEBVIEW_HEIGHT);
            view = new WebView();
            view.setPrefSize(widthPx, viewHeight);
            view.setMinSize(widthPx, viewHeight);
            view.setMaxSize(widthPx, viewHeight);
            // Undecorated: window chrome (title bar, borders) would eat pixels
            // off the client area, so a 300px-tall scene ends up ~272px tall.
            stage = new Stage(StageStyle.UNDECORATED);
            Scene scene = new Scene(view, widthPx, viewHeight, Color.WHITE);
            stage.setScene(scene);
            // WebView's WebKit backing surface only gets composited into the
            // scene graph after the Stage has actually gone through paint
            // pulses. Snapshotting an unshown WebView reliably yields a blank
            // (white) image. We show the Stage far off-screen so it paints for
            // real, then close it once the snapshot has been captured.
            stage.setX(-100_000);
            stage.setY(-100_000);
            stage.show();
        } catch (Throwable t) {
            future.complete(Result.error("Failed to create WebView: " + t.getMessage()));
            return;
        }

        final Stage finalStage = stage;
        final WebView finalView = view;
        final int finalHeightHint = heightPx;

        var engine = view.getEngine();
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.FAILED) {
                completeAndClose(finalStage, future, Result.error("Failed to load OSMD HTML page"));
            } else if (newState == Worker.State.SUCCEEDED) {
                triggerRender(engine, musicXml, widthPx);
            }
        });

        engine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            if (newTitle == null) {
                return;
            }
            if (newTitle.equals(TITLE_DONE)) {
                // Give the JavaFX pulse pipeline a couple of frames to actually
                // commit WebKit's rendered content to the WebView node before
                // snapshotting. Two nested runLaters is a well-known idiom.
                Platform.runLater(() -> Platform.runLater(() ->
                        snapshotAndComplete(finalView, finalStage, engine, widthPx, finalHeightHint, future)));
            } else if (newTitle.equals(TITLE_MISSING)) {
                completeAndClose(finalStage, future, Result.unavailable());
            } else if (newTitle.startsWith(TITLE_ERROR_PREFIX)) {
                completeAndClose(finalStage, future, Result.error(newTitle));
            }
        });

        engine.load(pageUrl);
    }

    private static void completeAndClose(Stage stage, CompletableFuture<Result> future, Result result) {
        try {
            stage.close();
        } catch (Throwable ignore) {
            // best effort
        }
        future.complete(result);
    }

    private void triggerRender(javafx.scene.web.WebEngine engine, String musicXml, int widthPx) {
        try {
            String js = "renderMusicXml(" + jsQuote(musicXml) + ", " + widthPx + ");";
            engine.executeScript(js);
        } catch (Throwable t) {
            // executeScript failures are surfaced via future
            // (the listener above will still fire on title changes if any).
        }
    }

    private void snapshotAndComplete(WebView view, Stage stage, WebEngine engine,
                                     int widthPx, int heightHint,
                                     CompletableFuture<Result> future) {
        try {
            // Force a layout pass so WebView's peer has current bounds before we snapshot.
            if (view.getScene() != null && view.getScene().getRoot() != null) {
                view.getScene().getRoot().applyCss();
                view.getScene().getRoot().layout();
            }
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.WHITE);
            WritableImage fxImage = view.snapshot(params, null);
            BufferedImage image = SwingFXUtils.fromFXImage(fxImage, null);
            BufferedImage cropped = cropToContent(image, engine, widthPx, heightHint);
            completeAndClose(stage, future, Result.success(cropped));
        } catch (Throwable t) {
            completeAndClose(stage, future, Result.error("Snapshot failed: " + t.getMessage()));
        }
    }

    /**
     * Crop the raw WebView snapshot down to the actual OSMD content bounds
     * published by {@code index.html} in {@code window.__sheet4jContentWidth}
     * / {@code __sheet4jContentHeight}. Falls back to the caller's height hint
     * when JavaScript did not publish content bounds (e.g. because OSMD
     * silently produced nothing).
     */
    private static BufferedImage cropToContent(BufferedImage full, WebEngine engine,
                                               int widthPx, int heightHint) {
        int contentWidth = readIntFromJs(engine, "window.__sheet4jContentWidth", widthPx);
        int contentHeight = readIntFromJs(engine, "window.__sheet4jContentHeight", heightHint);
        int w = Math.max(1, Math.min(contentWidth, full.getWidth()));
        int h = Math.max(1, Math.min(contentHeight, full.getHeight()));
        if (w == full.getWidth() && h == full.getHeight()) {
            return full;
        }
        return full.getSubimage(0, 0, w, h);
    }

    private static int readIntFromJs(WebEngine engine, String expression, int fallback) {
        try {
            Object v = engine.executeScript(expression);
            if (v instanceof Number n) {
                int i = (int) Math.ceil(n.doubleValue());
                return i > 0 ? i : fallback;
            }
        } catch (Throwable ignore) {
            // fall through
        }
        return fallback;
    }

    /**
     * Escape a MusicXML string for embedding as a JavaScript string literal.
     */
    private static String jsQuote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 32);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\u2028' -> sb.append("\\u2028");
                case '\u2029' -> sb.append("\\u2029");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static void ensureToolkit() {
        if (!TOOLKIT_STARTED.compareAndSet(false, true)) {
            return;
        }
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException alreadyStarted) {
            // Fine: JavaFX was already booted (e.g. by SheetDemoApp).
        }
        // Keep the JavaFX runtime alive across successive renders.
        Platform.setImplicitExit(false);
    }
}
