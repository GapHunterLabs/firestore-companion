# Demo

`incomplete-service-account.json` is a **fake, placeholder** file (no
real project, no real key material) with a deliberately missing
`private_key` field. It exists only to demonstrate this plugin's clear
error message when a required field is missing:

> Service account JSON is missing required field "private_key"

...as opposed to the cited competitor's opaque:

> Connection test failed: java.util.NoSuchElementException: Collection
> contains no element matching the predicate.

To screenshot the actual browsing UI (collections/documents/table),
you need a real GCP project with a real service account key -- not
included here, and never should be (see `CONSTITUTION.md` §1/testing
guidance: never test against a real Firebase project in this repo).
