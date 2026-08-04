# Firestore Companion

Browse Firestore collections, documents, and subcollections in a tool
window, authenticating with a GCP service account key via simple REST
calls.

## Why it exists

**Firebase Firestore** (JetBrains Marketplace id 23609), 14,735
downloads, paid, vendor Anton Shuvaev (also the author of JetClient/
DynamoDB/Elasticsearch companions -- not Google). Rated 3.80, notably
the lowest among that vendor's ~6 database plugins (DynamoDB 4.41,
Elasticsearch 4.62), confirming this is a problem specific to this
plugin, not general vendor reputation. Real, verbatim reviewer
complaints (same exact error, 3 users, 8+ months):

- *"fails with the error NoSuchElementException whenever trying to
  connect to Firestore"* (2026-05-09)
- *"'Connection test failed: java.util.NoSuchElementException:
  Collection contains no element matching the predicate.'"*
  (2024-09-07)
- *"Can neither connect with service account nor application default
  credentials JSON. The only feedback I get is 'Connection test failed:
  java.util.NoSuchElementException...'"* (2024-09-06)
- *"it works very poorly with sub-collections. I would like to see
  these sub-collections in tabular form as well, not just in JSON."*
  (4 stars, 2026-01-29)

## Why built this way

- **Every service-account field is looked up explicitly, by name.**
  `ServiceAccountParser` never uses a bare `.first {}`/`.single {}` over
  a collection that might not contain a match -- the exact shape of bug
  that produces an opaque `NoSuchElementException`. A missing or
  malformed field always fails with a message naming that field, and a
  file with the wrong credential type (e.g. application default
  credentials, `"type": "authorized_user"`) is rejected with a message
  saying so, not a generic crash.
- **One HTTP POST, no interactive flow.** `JwtBuilder` signs a JWT
  bearer assertion locally (RS256, via the JDK's own `java.security`
  APIs -- no external crypto library) and `OAuthTokenClient` exchanges
  it for an access token with a single POST to the token URI named in
  the service account file itself.
- **Simple REST calls to `firestore.googleapis.com/v1`**, not a heavy
  gRPC SDK dependency -- `FirestoreRestClient` uses the JDK's own
  `java.net.http.HttpClient`.
- **Subcollections in a real table**, not just JSON -- selecting a
  document and clicking "Open Subcollections" drills into that
  document's own subcollections, listed the same way as root
  collections. Directly answers the fourth complaint above.
- **Credentials are never logged or persisted.** Only the service
  account file's *path* and the project ID are saved (via
  `PropertiesComponent`, not a secret store -- neither value is a
  secret); the JSON contents, the parsed private key, and the resulting
  access token exist only in memory for the current session.
- **All network I/O runs off the EDT** (`executeOnPooledThread`), with
  results only ever applied to Swing via `invokeLater`.

### v1 scope cuts (documented, not silent)

Field editing is implemented and tested at the REST layer
(`FirestoreRestClient.patchDocument`, a real `PATCH` with the update
mask built from field names) but not yet wired into the tool window UI
-- v1 is read-only browsing. A full field-editing UI is deferred to a
later version rather than shipped rushed.

## Usage

Open the "Firestore" tool window, set the path to your service account
JSON key file and your GCP project ID, and click Connect. Select a
collection on the left to see its documents in the table; select a
document row and click "Open Subcollections of Selected Document" to
drill into that document's own subcollections.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**kennyj.diazm@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
