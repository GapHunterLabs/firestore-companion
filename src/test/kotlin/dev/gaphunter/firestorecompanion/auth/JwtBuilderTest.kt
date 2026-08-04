package dev.gaphunter.firestorecompanion.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class JwtBuilderTest {
    private fun generatePemPrivateKey(): Pair<String, java.security.PublicKey> {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val encoded = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----\n"
        return pem to keyPair.public
    }

    @Test
    fun `produces a three-part base64url JWT with the expected claims`() {
        val (pem, _) = generatePemPrivateKey()
        val credentials = ServiceAccountCredentials(
            projectId = "acme-corp-prod",
            clientEmail = "reader@acme-corp-prod.iam.gserviceaccount.com",
            privateKeyPem = pem,
            tokenUri = "https://oauth2.googleapis.com/token",
        )
        val jwt = JwtBuilder.buildSignedAssertion(credentials, nowEpochSeconds = 1_700_000_000)
        val parts = jwt.split(".")
        assertEquals(3, parts.size)

        val decoder = Base64.getUrlDecoder()
        val header = String(decoder.decode(parts[0]), Charsets.UTF_8)
        val claims = String(decoder.decode(parts[1]), Charsets.UTF_8)
        assertTrue(header.contains("\"RS256\""))
        assertTrue(claims.contains("\"reader@acme-corp-prod.iam.gserviceaccount.com\""))
        assertTrue(claims.contains("\"https://www.googleapis.com/auth/datastore\""))
        assertTrue(claims.contains("1700000000"))
        assertTrue(claims.contains("1700003600"))
    }

    @Test
    fun `the signature actually verifies against the matching public key`() {
        val (pem, publicKey) = generatePemPrivateKey()
        val credentials = ServiceAccountCredentials(
            projectId = "p",
            clientEmail = "e@p.iam.gserviceaccount.com",
            privateKeyPem = pem,
            tokenUri = "https://oauth2.googleapis.com/token",
        )
        val jwt = JwtBuilder.buildSignedAssertion(credentials, nowEpochSeconds = 1_700_000_000)
        val parts = jwt.split(".")
        val signingInput = "${parts[0]}.${parts[1]}"
        val signatureBytes = Base64.getUrlDecoder().decode(parts[2])

        val verifier = Signature.getInstance("SHA256withRSA").apply {
            initVerify(publicKey)
            update(signingInput.toByteArray(Charsets.UTF_8))
        }
        assertTrue("signature must verify against the real public key", verifier.verify(signatureBytes))
    }

    @Test
    fun `a signature built against a different key does not verify`() {
        val (pem, _) = generatePemPrivateKey()
        val (_, unrelatedPublicKey) = generatePemPrivateKey()
        val credentials = ServiceAccountCredentials("p", "e@p.iam.gserviceaccount.com", pem, "https://oauth2.googleapis.com/token")
        val jwt = JwtBuilder.buildSignedAssertion(credentials, nowEpochSeconds = 1_700_000_000)
        val parts = jwt.split(".")
        val signingInput = "${parts[0]}.${parts[1]}"
        val signatureBytes = Base64.getUrlDecoder().decode(parts[2])

        val verifier = Signature.getInstance("SHA256withRSA").apply {
            initVerify(unrelatedPublicKey)
            update(signingInput.toByteArray(Charsets.UTF_8))
        }
        assertTrue(!verifier.verify(signatureBytes))
    }

    @Test(expected = FirestoreAuthException::class)
    fun `malformed private key throws a clear FirestoreAuthException`() {
        val credentials = ServiceAccountCredentials("p", "e@p.iam.gserviceaccount.com", "not a real key", "https://oauth2.googleapis.com/token")
        JwtBuilder.buildSignedAssertion(credentials, nowEpochSeconds = 1_700_000_000)
    }
}
