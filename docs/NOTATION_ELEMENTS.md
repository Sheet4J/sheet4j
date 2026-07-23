# Sheet music notation elements → code

A visual glossary of the notation elements Sheetmusic4J renders, so you can point at something in a screenshot and know
exactly which class/method is responsible for it. Rendered from
`fxdemo/src/test/resources/xmlsamples/SchbAvMaSample.musicxml`
(Schubert's *Ave Maria*), the same fixture used throughout development.

![Annotated notation elements](notation-elements-annotated.png)

## How to use this when reporting a difference

Instead of "the horizontal line above the notes is too low", say **"the beam is too low"** — then check the table below
for `Beam` and you (or I)
can jump straight to `Engraver.computeStemTips` / `naturalStem`. The goal is to skip a round of "what do you mean by X"
every time something looks off.

## Element reference

Every element flows through the same three layers (see `CLAUDE.md`):
**MusicXML → core model → engraving layout → painter**. The table gives the concrete name at each stage.

| Element                          | What it looks like                                          | MusicXML source                                                             | Core model (`core`)                        | Layout (`engraving.Engraver`)                                                                     | Placement type                                         | Drawn by (`fxviewer.ScorePainter`) |
|----------------------------------|-------------------------------------------------------------|-----------------------------------------------------------------------------|--------------------------------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------------------|------------------------------------|
| **Clef**                         | Treble/bass symbol at the start of a staff                  | `<clef>`                                                                    | `Clef`, `ClefSign`                         | `clefGlyph()`, `clefAnchorLineIndex()`                                                            | `GlyphPlacement` (`CLEF_G/F/C`)                        | `drawGlyph`                        |
| **Key signature**                | Sharps/flats after the clef                                 | `<key><fifths>`                                                             | `KeySignature`                             | `placeKeySignature()`, `KeySignatureLayout`                                                       | `GlyphPlacement` (`ACCIDENTAL_SHARP/FLAT`)             | `drawGlyph`                        |
| **Time signature**               | e.g. `4/4` after the key sig                                | `<time>`                                                                    | `TimeSignature`                            | `placeTimeSignature()`                                                                            | `GlyphPlacement` (`TIME_DIGIT_0`…`9`)                  | `drawGlyph`                        |
| **Barline**                      | Vertical line ending a measure/system                       | implicit / `<barline>`                                                      | —                                          | measure loop in `layoutStaffRow`; `SystemBarline`                                                 | `SystemBarline`                                        | `drawSystemBarline`, `drawStaff`   |
| **Measure (bar)**                | Horizontal span of music between two barlines               | `<measure>`                                                                 | `Measure`                                  | measure loop in `layoutStaffRow`; `sharedMeasureMinWidths()`                                      | (implicit — bounded by two `SystemBarline`s)           | —                                  |
| **Notehead**                     | The oval note head itself                                   | `<note><type>`                                                              | `Note`, `NoteType`                         | `noteheadGlyph()`, `placeNote()`                                                                  | `GlyphPlacement` (`NOTEHEAD_BLACK/HALF/WHOLE`)         | `drawGlyph`                        |
| **Chord**                        | 2+ noteheads stacked at the same beat                       | `<chord/>`                                                                  | `Chord` (wraps `List<Note>`)               | `placeElement()` Chord branch                                                                     | multiple `GlyphPlacement`s, one shared `StemPlacement` | `drawGlyph` × N                    |
| **Stem**                         | Vertical line from a notehead                               | implicit, or explicit `<stem>up\|down</stem>`                               | `Note.stemUp()`                            | `naturalStem()`, `computeStemTips()` (run-wide flattening + ledger-line clearance), `placeNote()` | `StemPlacement(x, y1, y2)`                             | `drawStem`                         |
| **Beam**                         | Thick bar(s) joining stems of short notes                   | `<beam number="N">begin/continue/end</beam>`                                | `Beam`, `Beam.State`                       | `processBeams()`, `computeStemTips()`                                                             | `BeamPlacement`                                        | `drawBeam`                         |
| **Flag**                         | Curly tail on an *unbeamed* 8th/16th note                   | implicit from `<type>` when not beamed                                      | —                                          | `flagGlyph()`                                                                                     | `GlyphPlacement` (`FLAG_8TH/16TH_UP/DOWN`)             | `drawGlyph`                        |
| **Accidental**                   | ♯ ♭ ♮ directly before a notehead                         | `<accidental>` (explicit only — see note below)                             | `Accidental`, `Note.displayedAccidental()` | `accidentalGlyph()`, `hasAccidental()`                                                            | `GlyphPlacement` (`ACCIDENTAL_*`)                      | `drawGlyph`                        |
| **Augmentation dot**             | Small dot after a note/rest, extends duration               | `<dot/>`                                                                    | `Note.dots()` / `Rest.dots()`              | dot loop in `placeNote()`/`placeElement()`                                                        | `GlyphPlacement` (`AUG_DOT`)                           | `drawGlyph`                        |
| **Rest**                         | Symbol for a silent beat                                    | `<rest/>`                                                                   | `Rest`, `NoteType`                         | `restGlyph()`, `restAnchorStaffStep()`                                                            | `GlyphPlacement` (`REST_WHOLE`…`REST_128TH`)           | `drawGlyph`                        |
| **Tuplet number/bracket**        | Small italic count (e.g. "6", "3") over/under a grouped run | `<time-modification>` + `<notations><tuplet>`                               | `TimeModification`, `Tuplet`               | `updateTupletCandidates()`, `TupletRun` (tracks the run's pitch extreme)                          | `TupletPlacement`                                      | `drawTuplet`                       |
| **Slur**                         | Curved line over/under a *phrase* (different pitches)       | `<notations><slur>`                                                         | `Slur`, `Slur.Placement`                   | slur matching in `placeNote()`, `SlurStart` (tracks pitch extremes spanned)                       | `SlurPlacement`                                        | `drawSlur`                         |
| **Tie**                          | Curved line joining two notes of the *same* pitch           | `<tie type="start\|stop"/>`                                                 | `Note.tieStart()`/`tieStop()`              | tie matching in `placeNote()`, `PlacedNote`                                                       | `TiePlacement`                                         | `drawTie`                          |
| **Articulation — staccato**      | Dot directly above/below a notehead                         | `<notations><articulations><staccato/>`                                     | `Articulation.STACCATO`                    | articulation loop in `placeNote()`                                                                | `GlyphPlacement` (`ARTICULATION_STACCATO`)             | `drawGlyph`                        |
| **Articulation — accent**        | `>` mark above/below a notehead                             | `<notations><articulations><accent/>`                                       | `Articulation.ACCENT`                      | articulation loop in `placeNote()`                                                                | `GlyphPlacement` (`ARTICULATION_ACCENT`)               | `drawGlyph`                        |
| **Dynamics**                     | *pp, mf, ff, ...*                                           | `<direction-type><dynamics>`                                                | `DynamicMark`, `DirectionType.Dynamic`     | `placeDirection()` Dynamic branch, `dynamicGlyph()`                                               | `GlyphPlacement` (`DYNAMIC_*`)                         | `drawGlyph`                        |
| **Hairpin**                      | Opening/closing `<`/`>` wedge (cresc./dim.)                 | `<direction-type><wedge>`                                                   | `DirectionType.Wedge`, `WedgeType`         | `placeDirection()` Wedge branch, `WedgeStart`                                                     | `HairpinPlacement`                                     | `drawHairpin`                      |
| **Tempo / words text**           | e.g. "Sehr langsam" above the staff                         | `<direction-type><words>`                                                   | `DirectionType.Words`                      | `placeDirection()` Words branch                                                                   | `TextPlacement` (`MarkingCategory.DIRECTION`)          | `drawText`                         |
| **Metronome mark**               | e.g. ♩ = 60                                                | `<metronome>`                                                               | `DirectionType.Metronome`                  | `placeDirection()` Metronome branch, `metronomeText()`                                            | `TextPlacement` (`MarkingCategory.TEMPO`)              | `drawText`                         |
| **Rehearsal mark**               | Boxed letter/number (A, B, 12...)                           | `<rehearsal>`                                                               | `DirectionType.Rehearsal`                  | `placeDirection()` Rehearsal branch                                                               | `TextPlacement`, boxed (`MarkingCategory.REHEARSAL`)   | `drawText`                         |
| **Chord symbol**                 | Guitar/piano chord name above the staff (Cmaj7...)          | `<harmony>`                                                                 | `Harmony`, `HarmonyKind`                   | `placeHarmony()`                                                                                  | `TextPlacement` (`MarkingCategory.CHORD_SYMBOL`)       | `drawText`                         |
| **Lyrics**                       | Syllables under a vocal line                                | `<lyric>`                                                                   | `Lyric`, `Syllabic`                        | `placeLyrics()`                                                                                   | `TextPlacement` (`MarkingCategory.LYRIC`)              | `drawText`                         |
| **Brace / bracket ("accolade")** | Curly/square brace grouping staves at the left              | `<part-group>`, or implicit for a multi-staff part (e.g. piano grand staff) | `PartGroup`, `GroupSymbol`                 | grand-staff brace + `<part-group>` bracket logic in `layout()`                                    | `BracketPlacement`                                     | `drawBracket`                      |
| **Part label**                   | Instrument name at the left of a system (e.g. "Piano")      | `<part-name>` / `<part-abbreviation>`                                       | `Part`                                     | `emitPartLabel()`                                                                                 | `TextPlacement` (`MarkingCategory.PART_LABEL`)         | `drawText`                         |

## Notes on a few non-obvious rules

- **Accidentals are never inferred from pitch.** A note's `alter` (e.g. -1 for flat) only encodes the *sounding* pitch —
  it does **not** mean the flat should be drawn. Only an explicit `<accidental>` element in the source causes a glyph to
  appear. This matches standard MusicXML: the authoring software already decided where accidentals are visually needed
  (key signature, courtesy, previous note in the measure) and encodes that decision directly.
- **Stem direction prefers the explicit `<stem>` element** over a pitch-based guess, because a beamed run can span notes
  on both sides of the staff's middle line — guessing per note independently can flip direction mid-beam.
- **A beamed run shares one stem length**, computed from the highest/lowest notehead across the *entire run* (not each
  note's own pitch), so every stem reaches the same beam height instead of some falling short.
- **Slurs track the pitch extremes of every note they span** (not just their two endpoints), so a phrase that arcs up to
  a peak and back down gets a curve that clears the peak instead of cutting through it.
- **Tuplets and slurs both use MusicXML's `number` attribute** to pair a
  `start` with its matching `stop` when several are nested/concurrent.
- **"Measure" and "bar" are the same thing** — the horizontal span between two barlines. *Measure* is the American
  term, *bar* the British one; the codebase uses `Measure` throughout (mirroring MusicXML's `<measure>`), but bug
  reports and issues may use either interchangeably.

## Regenerating the annotated image

The screenshot above is a real headless render (not a mockup), produced via
`HeadlessScoreImage` / `CompareFxViewWithReferenceTest`'s machinery. To regenerate it after a layout change:

```bash
mvn -pl fxdemo test -Dtest=CompareFxViewWithReferenceTest
# -> fxdemo/target/sheetmusic4j-diff/SchbAvMaSample/rendered.png
```
