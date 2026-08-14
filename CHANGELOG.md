<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Firestore Companion Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/GapHunterLabs/firestore-companion/compare/0.2.1...HEAD
[0.2.1]: https://github.com/GapHunterLabs/firestore-companion/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/GapHunterLabs/firestore-companion/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/GapHunterLabs/firestore-companion/commits/0.1.0
