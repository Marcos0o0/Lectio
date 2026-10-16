---
name: Lectio
description: A modern dark reading space for understanding English in Spanish.
colors:
  primary-light: "#365F99"
  on-primary-light: "#FFFFFF"
  primary-container-light: "#D8E6FF"
  on-primary-container-light: "#102F57"
  secondary-light: "#526176"
  on-secondary-light: "#FFFFFF"
  secondary-container-light: "#E0E7F2"
  on-secondary-container-light: "#202E40"
  tertiary-light: "#635C7E"
  on-tertiary-light: "#FFFFFF"
  tertiary-container-light: "#E9E1FF"
  on-tertiary-container-light: "#302B4A"
  background-light: "#F6F8FC"
  on-background-light: "#191D25"
  surface-light: "#F6F8FC"
  on-surface-light: "#191D25"
  surface-variant-light: "#E2E7EF"
  on-surface-variant-light: "#414B5A"
  outline-light: "#707C8D"
  outline-variant-light: "#C2CAD6"
  surface-container-lowest-light: "#FFFFFF"
  surface-container-low-light: "#F0F3F9"
  surface-container-light: "#EAEEF5"
  surface-container-high-light: "#E3E8F0"
  surface-container-highest-light: "#DCE2EC"
  surface-dim-light: "#D6DDE8"
  surface-bright-light: "#F6F8FC"
  inverse-surface-light: "#2B3038"
  inverse-on-surface-light: "#EDF1F8"
  inverse-primary-light: "#A8C7FA"
  surface-tint-light: "#365F99"
  error-light: "#BA1A1A"
  on-error-light: "#FFFFFF"
  error-container-light: "#FFDAD6"
  on-error-container-light: "#410002"
  scrim-light: "#000000"
  primary-dark: "#A8C7FA"
  on-primary-dark: "#0A305F"
  primary-container-dark: "#243E62"
  on-primary-container-dark: "#D6E5FF"
  secondary-dark: "#BBC5D6"
  on-secondary-dark: "#263141"
  secondary-container-dark: "#303A49"
  on-secondary-container-dark: "#E0E7F2"
  tertiary-dark: "#C5BFE4"
  on-tertiary-dark: "#302B4A"
  tertiary-container-dark: "#47415F"
  on-tertiary-container-dark: "#E9E1FF"
  background-dark: "#101216"
  on-background-dark: "#E3E7EF"
  surface-dark: "#101216"
  on-surface-dark: "#E3E7EF"
  surface-variant-dark: "#424B59"
  on-surface-variant-dark: "#B5BECC"
  outline-dark: "#8691A2"
  outline-variant-dark: "#424B59"
  surface-container-lowest-dark: "#0C0E12"
  surface-container-low-dark: "#171A20"
  surface-container-dark: "#1D2129"
  surface-container-high-dark: "#262B35"
  surface-container-highest-dark: "#303641"
  surface-dim-dark: "#101216"
  surface-bright-dark: "#363C47"
  inverse-surface-dark: "#E3E7EF"
  inverse-on-surface-dark: "#2B3038"
  inverse-primary-dark: "#365F99"
  surface-tint-dark: "#A8C7FA"
  error-dark: "#FFB4AB"
  on-error-dark: "#690005"
  error-container-dark: "#93000A"
  on-error-container-dark: "#FFDAD6"
  scrim-dark: "#000000"
  cover-blue: "#293F5D"
  cover-violet: "#3E425C"
  cover-teal: "#2E4A53"
  cover-slate: "#414956"
  cover-title: "#E6ECF5"
  cover-metadata: "#CCD7E6"
  cover-spine: "rgba(0, 0, 0, 0.12)"
typography:
  display-small:
    fontFamily: "Literata, serif"
    fontSize: "36sp"
    lineHeight: "44sp"
  headline-large:
    fontFamily: "Literata, serif"
    fontSize: "32sp"
    lineHeight: "40sp"
  headline-medium:
    fontFamily: "Literata, serif"
    fontSize: "28sp"
    lineHeight: "36sp"
  headline-small:
    fontFamily: "Literata, serif"
    fontSize: "24sp"
    lineHeight: "32sp"
  title-large:
    fontFamily: "sans-serif"
    fontSize: "22sp"
    fontWeight: 500
    lineHeight: "28sp"
  title-medium:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
  title-small:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
  body-large:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    lineHeight: "25sp"
  body-medium:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    lineHeight: "21sp"
  body-small:
    fontFamily: "sans-serif"
    fontSize: "12sp"
    lineHeight: "18sp"
  label-large:
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
rounded:
  resume-card: "16dp"
  book-cover: "3dp 8dp 8dp 3dp"
spacing:
  gap-tight: "4dp"
  gap-small: "8dp"
  gap-medium: "12dp"
  gap-section: "16dp"
  card-padding: "20dp"
  screen-inset: "24dp"
  empty-inset: "28dp"
  sheet-bottom: "32dp"
  empty-vertical: "40dp"
components:
  button-primary-light:
    backgroundColor: "{colors.primary-light}"
    textColor: "{colors.on-primary-light}"
    typography: "{typography.label-large}"
  button-primary-dark:
    backgroundColor: "{colors.primary-dark}"
    textColor: "{colors.on-primary-dark}"
    typography: "{typography.label-large}"
  resume-card-light:
    backgroundColor: "{colors.primary-container-light}"
    textColor: "{colors.on-primary-container-light}"
    rounded: "{rounded.resume-card}"
    padding: "{spacing.card-padding}"
  resume-card-dark:
    backgroundColor: "{colors.primary-container-dark}"
    textColor: "{colors.on-primary-container-dark}"
    rounded: "{rounded.resume-card}"
    padding: "{spacing.card-padding}"
  section-heading:
    typography: "{typography.headline-small}"
  lookup-heading:
    typography: "{typography.headline-medium}"
  selected-passage-light:
    backgroundColor: "{colors.surface-container-light}"
    typography: "{typography.body-medium}"
    padding: "{spacing.gap-section}"
  selected-passage-dark:
    backgroundColor: "{colors.surface-container-dark}"
    typography: "{typography.body-medium}"
    padding: "{spacing.gap-section}"
---

# Design System: Lectio

## Overview

**Creative North Star: "A quiet reading room"**

Lectio pairs graphite surfaces, slate layers, and soft blue actions with literary headings and clear native controls. Dark is the initial theme; explicit Claro and Sistema preferences remain available and persisted. Reading and contextual understanding share one space: a selected word opens a meaning sheet, while optional AI stays behind explicit actions. Local use requires no account.

This is a code-led record of the native Android Compose implementation, not a reference composition. Compose dimensions use dp and text uses sp; the isolated EPUB WebView has its own CSS reading surface. Native components in the JSON sidecar are descriptive records, not HTML/CSS previews. Token frontmatter is normative; framework defaults remain owned by Material 3, and implementation limitations below are not new design rules.

**Key Characteristics:**
- Modern graphite dark surfaces, slate containers, soft blue actions, and a cool light scheme.
- Serif literary headings with sans serif functional text.
- Material 3 controls, tonal containers, and native modal sheets.
- Adjustable reading type and contextual meanings with visible provenance.
- Four destinations that adapt from bottom navigation to a rail.

## Colors

The palette uses graphite and slate neutrals with soft blue primary actions. Both schemes explicitly define every Material 3 color role in ui/Theme.kt; the frontmatter records those assignments without deriving a replacement palette. There is no dynamic wallpaper color implementation.

### Primary

Deep blue (primary-light) and soft blue (primary-dark) carry filled actions, reading progress, selected chapter indicators, activity values, and source labels. Their on-primary partners carry foregrounds on filled actions. The primary-container pairs provide the continuing-reading card and activity chart surface. The launcher icon uses the dark background and primary colors; the native startup window uses the same graphite background.

### Secondary

Slate blue-gray supplies the secondary roles; secondary-container distinguishes the vocabulary review summary and saved EPUB highlights. Use the foreground corresponding to the selected container. The continuing-reading card explicitly pairs its primary-container foregrounds; some summary surfaces currently inherit text rather than explicitly applying the matching container foreground.

### Tertiary

Muted violet completes the explicit Material scheme for components that request tertiary roles. It does not establish a separate promotional accent or a new screen treatment.

### Neutral

Background and surface share graphite in dark mode and cool near-white in light mode; on-background and on-surface supply the matching text. On-surface-variant carries authors, explanations, metadata, and helper copy. Outline-variant carries dividers. The five surface-container levels provide tonal layers for native components; selected passages and EPUB blockquotes use surface-container. Inverse, tint, error, scrim, bright, and dim roles are explicitly recorded, rather than inherited from a warm default scheme.

**The Paired Roles Rule.** Resolve UI colors through MaterialTheme.colorScheme and use the foreground role corresponding to its background role.

EPUB selection uses primary-container / on-primary-container; saved highlights use secondary-container / on-secondary-container; dividers use outline-variant. The reader error fallback also follows the current background and foreground. The local cover-* tokens describe deterministic fallback illustrations only; they are not general UI accents.

## Typography

**Display Font:** bundled Literata (`Literary` in Compose; generic serif fallback in the WebView).
**Body Font:** Android `FontFamily.SansSerif`.
**Label Font:** Material default family; only `labelLarge` size, line height, and medium weight are overridden.

Literata display and headline roles create the literary character; sans serif titles, body text, and controls keep navigation practical. The offline variable font is bundled at `app/src/main/res/font/literata.ttf`, with its OFL license at `app/src/main/assets/fonts/OFL.txt`. The same font supplies fallback cover titles and the reader's serif option.

### Hierarchy

- **Display small:** library introductory heading; use the `display-small` token.
- **Headline large:** vocabulary and activity introductions, saved-word sheet heading.
- **Headline medium:** app bar titles, lookup heading, weekly reading total.
- **Headline small:** section headings, empty states, continuing-book title, word translation.
- **Title large:** vocabulary row word and saved-word translation.
- **Title medium:** book row title, settings groups, reader title, search result chapter.
- **Body large / medium / small:** introductory support, explanations, metadata respectively; use the frontmatter's distinct leading.
- **Label large:** calls to action, progress text, and continuing-reading context. Unoverridden label roles retain Material defaults.

**The Two Reading Voices Rule.** Use the Compose type roles for app UI; allow the separate reading page to follow the reader's serif/sans, size, and line-height preferences.

The EPUB page defaults to bundled Literata / generic serif, size (20 CSS px), and line height (1.8), with sans serif as the alternate. Controls permit size (16–32 CSS px) and line height (1.4–2.2). WebView `textZoom` follows the Android system font scale, so these preference values scale with the OS. The reader font is bundled at `app/src/main/assets/fonts/literata.ttf` and served from an intercepted local URL without network access. WebView h1 uses (1.8em), regular weight and leading (1.2); h2 uses (1.25em) and leading (1.35).

## Layout

The app has four persistent destinations: Biblioteca, Vocabulario, Actividad, and Ajustes. Below (600dp) available width they use a bottom `NavigationBar`; at or above that width they use a `NavigationRail`. Both show labels. Opening a book replaces destination navigation with the reader top bar and progress/chapter toolbar.

Lists and settings use the `screen-inset` token. Common list spacing is `gap-section`; card interiors use `card-padding`; related controls use `gap-small` and `gap-medium`. Empty states use the wider `empty-inset` and `empty-vertical` spacing. Modal sheets use the screen inset with `sheet-bottom` bottom padding. `FlowRow` wraps library filters, vocabulary filters, settings theme choices, and AI actions. Some reader appearance choices still use plain rows.

The continuing-reading card pairs a cover (98 × 148dp) with a flexible text column. Library rows use covers (72 × 108dp), a flexible title/metadata column, and a native overflow action. These are observed component dimensions, not a universal grid. Expanded screens currently retain a single main content column; the rail does not imply a tablet multi-pane layout.

The EPUB page is centered with maximum width (740 CSS px) and padding (24px 28px 70px). Images fit its width; tables scroll horizontally and preformatted text wraps. There is no established character-count measure.

**The Navigation Width Rule.** Use the existing compact bar / expanded rail boundary; do not copy the compact bar onto the expanded layout.

Insets are managed across native scaffolds and activity setup, with a reader navigation-bar inset. The library Scaffold consumes the bottom safe-drawing inset, keeping the import FAB above the taskbar on expanded windows. Android 15 emulator captures cover phone, expanded window, landscape, light/dark themes, and 130% text; they do not establish physical Redmi hardware or full keyboard/gesture coverage. See VALIDATION.md for evidence and limits.

## Elevation & Depth

Tonal surfaces provide most of the visual structure. The continuing-reading card and review/activity summaries use colored containers; selected passages use a neutral container, and dividers use the outline variant. Native FABs, menus, dialogs, bottom sheets, and buttons retain Material 3 default elevation and state behavior. There are no custom shadow, elevation, or motion token definitions to export. Do not invent CSS shadows or bespoke hover transitions for native controls.

## Shapes

The continuing-reading card has the explicit `resume-card` radius. Book covers have the asymmetric `book-cover` corners, with a narrower left edge suggesting a spine. Other surfaces use `MaterialTheme.shapes.large` or `.medium`; chart bars use `.small`. `LectioTheme` does not override Material 3's shape system, so those dimensions remain framework defaults. Buttons, chips, fields, dialogs, and sheets likewise keep their component shapes.

## Components

### Buttons

Filled `Button` is used for saving vocabulary and configuration; `FilledTonalButton` for review and saving a highlight; `OutlinedButton` for optional AI setup; `TextButton` for lower emphasis actions and dialog choices. Most form actions fill available width. Icons, selected/loading/disabled behavior, ripple, focus, and touch targets come from Material 3, without custom web hover styling. The import action is one extended FAB with primary/onPrimary colors and a progress indicator while importing.

### Chips

`FilterChip` expresses selected library/vocabulary filters, theme, and reading font. `AssistChip` expresses optional AI actions and is disabled during lookup loading. Their colors, borders, state layers, and shapes retain Material defaults. Wrap groups using the existing `FlowRow` patterns where space varies.

### Cards / Containers

The continuing-reading card uses the explicit paired primary-container foregrounds, card inset, cover, textual status, linear progress, and forward icon. Vocabulary review uses a secondary container; activity uses a primary container. These are tonal highlights, not a rule to put every row into a card. Library entries are plain clickable rows; vocabulary uses `ListItem` and thin dividers.

### Inputs / Fields

Search, API setup, personal translation, and notes use `OutlinedTextField`. Search is single line; translation and notes allow up to four lines. The API key has native password transformation and a visibility icon. Settings validation appears as separate error-role text, not a custom error border system. Field focus, cursor, border, and disabled treatment remain Material defaults.

### Navigation

`TopAppBar` gives screen context, with headline-medium titles outside the reader. The reader uses a compact title and chapter subtitle, back, search, appearance, contents, previous/next chapter, completion, and bookmark controls. Bottom navigation and rail share the same four labeled destinations. `PrimaryTabRow` separates contents and annotations. Transient feedback uses the `SnackbarHost` of the scaffold that owns the screen's bottom bar or FAB, keeping feedback above those controls. Deletion uses an `AlertDialog`.

### Book Covers

Real EPUB cover images crop to the cover bounds. Missing covers get a deterministic muted color, serif title, author or demonstration text, and a dark translucent spine. Color choice comes from four cool blue/slate/violet local constants (cover-blue, cover-violet, cover-teal, cover-slate). Titles use cover-title, metadata uses cover-metadata, and the 5dp spine uses cover-spine. These illustration tokens are not a primary-action palette. Tiny uppercase edition labels in this fallback are an existing implementation defect, not a reusable eyebrow/kicker pattern.

### Meaning Sheet

Text selection opens `LookupSheet`, a fully expanded `ModalBottomSheet` with a scrollable column, native close/pronunciation controls, translation and explanation, optional examples and synonyms, and a visible source/cache/approximate CEFR line. Longer selections show a neutral passage container. A local result can be saved; absent results permit a personal translation. AI actions remain explicit and show generated-response provenance. An expandable note field saves a highlight. The sheet stays in the reading context and requires no account.

### Reading Surface

An isolated WebView renders reflowable content using theme background/foreground, bundled Literata or sans serif, reader preferences, and OS text scaling. Native selection opens the meaning sheet; saved highlights are applied to page text. The native frame keeps chapter context and progress visible. Selection and saved highlights use paired theme container roles, blockquotes use the surface container, and dividers use the theme outline variant. The error HTML fallback uses the same background and foreground as the reading page.

## Do's and Don'ts

### Do:

- **Do** use Material 3 color roles with matching foregrounds in both explicit schemes.
- **Do** use serif headline roles and sans serif functional type through `LectioTypography`.
- **Do** retain the compact navigation bar and expanded navigation rail behavior.
- **Do** keep reading preferences separate from app UI typography.
- **Do** preserve visible meaning provenance and explicit optional AI actions in the reading context.
- **Do** use native Material controls and their built-in interaction states.

### Don't:

- **Don't** treat fallback cover colors as general app accents or reintroduce fixed warm colors into themed reader content.
- **Don't** promote tiny uppercase cover labels into a reusable heading pattern.
- **Don't** describe framework defaults as custom Lectio shadow, shape, motion, or error tokens.
- **Don't** introduce account UI as a prerequisite to local reading or vocabulary.
- **Don't** present Android 15 emulator captures or DOM selection tests as physical-phone gesture, TTS, or performance verification.
