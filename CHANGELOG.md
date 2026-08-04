<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Firestore Companion Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/GapHunterLabs/firestore-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/firestore-companion/commits/0.1.0
