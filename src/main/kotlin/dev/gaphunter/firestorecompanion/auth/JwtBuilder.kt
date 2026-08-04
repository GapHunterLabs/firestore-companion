package dev.gaphunter.firestorecompanion.auth

import dev.gaphunter.firestorecompanion.json.JsonNode
import dev.gaphunter.firestorecompanion.json.JsonWriter
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

private const val FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore"
private const val TOKEN_LIFETIME_SECONDS = 3600L

/**
 * Builds and RS256-signs a JWT bearer assertion (RFC 7523) from a service
 * account's private key -- entirely local and in-memory via the JDK's own
 * `java.security` APIs, no external crypto library. This is the "single
 * HTTP POST, no interactive flow" auth exchange from NEXT_BATCH_PLAN.md:
 * this object produces the assertion string; [OAuthTokenClient] posts it.
 * The private key is used only for the duration of this call and never
 * logged or persisted.
 */
object JwtBuilder {
    fun buildSignedAssertion(credentials: ServiceAccountCredentials, nowEpochSeconds: Long): String {
        val header = JsonNode.Obj(linkedMapOf("alg" to JsonNode.Str("RS256"), "typ" to JsonNode.Str("JWT")))
        val claims = JsonNode.Obj(
            linkedMapOf(
                "iss" to JsonNode.Str(credentials.clientEmail),
                "scope" to JsonNode.Str(FIRESTORE_SCOPE),
                "aud" to JsonNode.Str(credentials.tokenUri),
                "iat" to JsonNode.Num(nowEpochSeconds.toDouble()),
                "exp" to JsonNode.Num((nowEpochSeconds + TOKEN_LIFETIME_SECONDS).toDouble()),
            )
        )

        val encodedHeader = base64Url(JsonWriter.write(header).toByteArray(Charsets.UTF_8))
        val encodedClaims = base64Url(JsonWriter.write(claims).toByteArray(Charsets.UTF_8))
        val signingInput = "$encodedHeader.$encodedClaims"

        val privateKey = parsePrivateKey(credentials.privateKeyPem)
        val signatureBytes = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signingInput.toByteArray(Charsets.UTF_8))
        }.sign()

        return "$signingInput.${base64Url(signatureBytes)}"
    }

    private fun base64Url(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    private fun parsePrivateKey(pem: String): PrivateKey {
        val cleaned = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace(Regex("\\s"), "")
        val decoded = try {
            Base64.getDecoder().decode(cleaned)
        } catch (e: IllegalArgumentException) {
            throw FirestoreAuthException("Service account private_key is not valid base64/PEM: ${e.message}")
        }
        return try {
            KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(decoded))
        } catch (e: Exception) {
            throw FirestoreAuthException("Could not parse service account private key: ${e.message}")
        }
    }
}
