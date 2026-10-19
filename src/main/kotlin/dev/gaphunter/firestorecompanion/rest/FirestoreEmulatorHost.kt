package dev.gaphunter.firestorecompanion.rest

/**
 * The Firestore emulator's address, as the Firebase tools print it and as
 * `FIRESTORE_EMULATOR_HOST` holds it: `host:port` (`localhost:8080`,
 * `127.0.0.1:8080`, `[::1]:8080`). A leading `http://` and a trailing `/`
 * are accepted and dropped. The emulator speaks plain HTTP and needs no
 * credentials: it accepts the `owner` token, which also bypasses its
 * security rules, the same way the Firebase Admin SDKs talk to it.
 */
object FirestoreEmulatorHost {
    /** Token the emulator accepts in place of an OAuth access token. Not a secret. */
    const val TOKEN = "owner"

    private val HOST_PORT = Regex("""^(\[[0-9A-Fa-f:.]+]|[A-Za-z0-9.-]+):(\d{1,5})$""")

    /** `host:port`, normalized; throws [IllegalArgumentException] with a message for the user otherwise. */
    fun parse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("http://", ignoreCase = true)) text = text.substring("http://".length)
        text = text.trimEnd('/')
        if (text.startsWith("https://", ignoreCase = true)) {
            throw IllegalArgumentException("The Firestore emulator speaks plain HTTP: use host:port, e.g. localhost:8080.")
        }
        val match = HOST_PORT.matchEntire(text)
            ?: throw IllegalArgumentException("Emulator host must be host:port, e.g. localhost:8080 (got \"${raw.trim()}\").")
        val port = match.groupValues[2].toInt()
        if (port !in 1..65535) throw IllegalArgumentException("Emulator port must be between 1 and 65535 (got $port).")
        return text
    }

    /** The REST API root for the emulator at [hostPort] (already [parse]d). */
    fun apiRoot(hostPort: String): String = "http://$hostPort/v1"
}
