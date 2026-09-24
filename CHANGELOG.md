<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Firestore Companion Changelog

## [Unreleased]

## [2026.1.0]

### Added

- **Firestore Companion Pro**, an optional paid tier on top of
  everything below (all free-tier features stay free, no exceptions):
  saved multi-project profiles (dev/staging/prod, switchable without
  retyping the service account path and project ID every time), a
  query/filter builder (field + operator + typed value) against
  Firestore's own `:runQuery` structured-query endpoint instead of
  always listing an entire collection, and exporting a collection's
  documents to a pretty-printed JSON file, preserving int64 precision
  exactly as Firestore stores it (same rule already applied to
  editing). Version scheme moves from semver to a date-based scheme
  (required for the Freemium `<product-descriptor>` JetBrains assigns
  by release-version, not a stylistic choice).

## [0.2.4]

### Fixed

- A collection or document ID containing a space (or another character
  that isn't valid raw in a URI) is now percent-encoded per path
  segment before building any Firestore REST URL. Firestore IDs can
  contain such characters, and the real HTTP transport
  (`java.net.URI.create`) throws an opaque `IllegalArgumentException`
  on a literal space -- exactly the kind of crash this plugin exists
  to avoid. Affects browsing a collection/subcollection by name and
  saving an edited document.
- "Rate on Marketplace" now links to this plugin's own reviews page
  instead of the vendor page.

## [0.2.3]

### Added

- Review/star CTA: after 5 successful Firestore connections (never
  counted for a failed auth attempt), a one-time notification asks
  whether to rate the plugin on Marketplace, with a permanent "Don't
  ask again" option.

## [0.2.2]

### Fixed

- Tool window no longer shows the generic platform icon in the sidebar —
  the real Gap Hunter Labs mark is now declared via `icon=` on
  `<toolWindow>`.

## [0.2.1]

### Fixed

- Tool window content (service account/project ID fields, path label,
  collection list, documents table) was rendering flush against the
  tool window's own border, with no margin — fixed with an 8px empty
  border on the root panel.

## [0.2.0]

### Added

- Document editing in the tool window: "Edit Selected Document" opens a
  dialog with one row per field. Scalar fields (string, integer,
  double, boolean) are editable in place; map/array/geoPoint/
  reference/timestamp/null fields stay read-only, same deliberate
  scope cut as everything else in this plugin. Saves only the fields
  that actually changed, via the `patchDocument` REST call that
  already existed (tested since 0.1.0, only unwired to the UI until
  now).

## [0.1.0]

### Added

- Service account JSON -> signed JWT -> OAuth2 access token exchange,
  entirely local, with explicit field-by-field validation (never a bare
  `NoSuchElementException`).
- Firestore tool window: browse root collections, view documents in a
  table, and drill into a selected document's own subcollections.
- Simple REST client (`firestore.googleapis.com/v1`, no gRPC
  dependency) with a tested (not yet UI-wired) `PATCH` for field
  updates.

[Unreleased]: https://github.com/GapHunterLabs/firestore-companion/compare/2026.1.0...HEAD
[2026.1.0]: https://github.com/GapHunterLabs/firestore-companion/compare/0.2.4...2026.1.0
[0.2.4]: https://github.com/GapHunterLabs/firestore-companion/compare/0.2.3...0.2.4
[0.2.3]: https://github.com/GapHunterLabs/firestore-companion/compare/0.2.2...0.2.3
[0.2.2]: https://github.com/GapHunterLabs/firestore-companion/compare/0.2.1...0.2.2
[0.2.1]: https://github.com/GapHunterLabs/firestore-companion/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/GapHunterLabs/firestore-companion/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/GapHunterLabs/firestore-companion/commits/0.1.0
