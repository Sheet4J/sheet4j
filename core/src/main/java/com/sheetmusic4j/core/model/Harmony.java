package com.sheetmusic4j.core.model;

import java.util.Optional;

/**
 * A chord-symbol annotation attached to a measure at the beat where it should
 * appear (e.g. {@code BMaj7}, {@code D♯m9}, {@code G7/D}). Corresponds to the
 * MusicXML {@code <harmony>} element which sits as a sibling of
 * {@code <note>}/{@code <direction>} at measure level.
 *
 * <p>Harmony is a top-level {@link MusicElement} — not a
 * {@link DirectionType} — because MusicXML places it outside
 * {@code <direction-type>} and because its data shape (root + kind + optional
 * bass) is structurally different from any direction payload.
 *
 * <p>{@link #duration()} returns {@link Duration#ZERO} so the presence of a
 * chord symbol never affects measure timing or MIDI output.
 *
 * @param root         the chord root (mandatory)
 * @param kind         the chord quality (mandatory; {@link HarmonyKind#OTHER}
 *                     when the source declares an unrecognised value)
 * @param bass         the optional slash-chord bass (empty for non-slash chords)
 * @param textOverride the optional {@code text} attribute from
 *                     {@code <kind text="...">}, which takes precedence over
 *                     {@link HarmonyKind#shortLabel()} when rendering
 */
public record Harmony(Root root, HarmonyKind kind, Optional<Bass> bass, Optional<String> textOverride)
        implements MusicElement {

    public Harmony {
        if (root == null) {
            throw new IllegalArgumentException("Harmony requires a root");
        }
        if (kind == null) {
            kind = HarmonyKind.OTHER;
        }
        if (bass == null) {
            bass = Optional.empty();
        }
        if (textOverride == null) {
            textOverride = Optional.empty();
        }
    }

    @Override
    public Duration duration() {
        return Duration.ZERO;
    }

    /**
     * Compose the printable label for this chord symbol.
     *
     * <p>Format: {@code <root><accidental?><kindShort><\/bass?>} where the
     * kind portion is {@link #textOverride} when present, otherwise
     * {@link HarmonyKind#shortLabel()}. Accidentals use unicode glyphs
     * ({@code ♯}/{@code ♭}/{@code 𝄪}/{@code 𝄫}) so the label renders
     * correctly in any plain-text font.
     *
     * @return the rendered chord-symbol string
     */
    public String displayLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append(root.step().name()).append(accidentalGlyph(root.alter()));
        sb.append(textOverride.orElseGet(kind::shortLabel));
        bass.ifPresent(b -> sb.append('/').append(b.step().name()).append(accidentalGlyph(b.alter())));
        return sb.toString();
    }

    /**
     * Map a chromatic alter value ({@code -2..+2}) to its unicode accidental
     * glyph. Returns an empty string for {@code 0} and for out-of-range values
     * (defensive: MusicXML permits fractional alters we don't render yet).
     */
    private static String accidentalGlyph(int alter) {
        return switch (alter) {
            case -2 -> "\uD834\uDD2B"; // 𝄫 double flat
            case -1 -> "\u266D";       // ♭
            case 0 -> "";
            case 1 -> "\u266F";        // ♯
            case 2 -> "\uD834\uDD2A"; // 𝄪 double sharp
            default -> "";
        };
    }

    /**
     * The root of a chord symbol: a pitch class (step + chromatic alter) with
     * no octave, since chord symbols are position-independent.
     *
     * @param step  the diatonic root step
     * @param alter the chromatic alteration (-1 = flat, +1 = sharp, ...)
     */
    public record Root(Step step, int alter) {
        public Root {
            if (step == null) {
                throw new IllegalArgumentException("Root requires a step");
            }
        }

        /**
         * Convenience constructor for a natural root (alter = 0).
         *
         * @param step the diatonic root step
         */
        public Root(Step step) {
            this(step, 0);
        }
    }

    /**
     * Optional slash-chord bass note: same shape as {@link Root}.
     *
     * @param step  the diatonic bass step
     * @param alter the chromatic alteration (-1 = flat, +1 = sharp, ...)
     */
    public record Bass(Step step, int alter) {
        public Bass {
            if (step == null) {
                throw new IllegalArgumentException("Bass requires a step");
            }
        }

        /**
         * Convenience constructor for a natural bass (alter = 0).
         *
         * @param step the diatonic bass step
         */
        public Bass(Step step) {
            this(step, 0);
        }
    }
}
