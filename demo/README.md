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
included here, and never should be: never test against a real
Firebase project in this repo.

## Local emulator (`emulator/`)

To browse real data without a Google Cloud project, start the Firestore
emulator from the Firebase tools and load the sample shop:

```
cd demo/emulator
firebase emulators:start --only firestore --project demo-northwind-shop
python seed.py
```

Then, in the Firestore tool window: Emulator host `127.0.0.1:8080`,
Project ID `demo-northwind-shop`, Connect. Project IDs that start with
`demo-` never reach Google. `seed.py` wipes the emulator's data each time
it runs, so the sample starts the same way every time.
