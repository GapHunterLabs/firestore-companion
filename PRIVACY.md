# Privacy Policy — Firestore Companion

**Effective date:** 2026-08-04

Firestore Companion is a Gap Hunter Labs plugin for IntelliJ Platform IDEs.
This policy is short because the plugin's design makes it short: there
is nothing to disclose beyond what's below.

## What this plugin collects

**Nothing.** Firestore Companion does not collect, store, transmit, or sell
any data of its own — no usage analytics, no telemetry, no crash reports,
no personally identifiable information. The plugin has no backend and no
Gap Hunter Labs server ever sees your data or credentials.

## Network access

Firestore Companion's entire purpose is browsing and editing **your own**
Firestore data, which requires network access by design — unlike most
Gap Hunter Labs plugins, this one is not "zero network calls," and we
won't claim otherwise. What actually happens:

- **No network call is automatic.** Nothing happens on install or in the
  background. The only network activity is what you trigger yourself —
  clicking "Connect" (or an action against a collection/document) after
  you provide your own GCP service account key and project ID.
- **Every request goes directly from your machine to Google**, never
  through a Gap Hunter Labs server: a signed JWT bearer assertion is
  exchanged for an access token at the token URI named in your own
  service account file, then REST calls go straight to
  `firestore.googleapis.com/v1`. We have no server in this path — we
  could not see this traffic even if we wanted to.
- **No telemetry or usage data is ever sent.** The only network traffic
  is the Firestore reads/writes you explicitly perform.

## Credentials

Only the *path* to your service account file and your project ID are
saved locally (via the IDE's own settings storage) — neither is a
secret by itself. The file's contents, the parsed private key, and the
resulting access token exist only in memory for the current IDE session;
none of it is logged or persisted to disk by the plugin.

## Third parties

None. Firestore Companion has no third-party SDKs, no analytics libraries,
no ad networks. The only outbound calls are the ones described above, made
directly to Google's own Firestore API with your own credentials — never
to any Gap Hunter Labs server.

## Changes to this policy

If this ever changes, this file will be updated and the change will be
noted in the plugin's `CHANGELOG.md`.

## Contact

Questions about this policy: **gaphunterlabs@gmail.com**
