package dev.gaphunter.firestorecompanion.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Never touches the network -- [OAuthTokenClient.httpPost] is injected
 * with a fake transport, confirming the token exchange parses a
 * realistic response shape and surfaces errors clearly. */
class OAuthTokenClientTest {
    private val credentials = ServiceAccountCredentials(
        projectId = "p",
        clientEmail = "e@p.iam.gserviceaccount.com",
        privateKeyPem = "unused-in-this-test",
        tokenUri = "https://oauth2.googleapis.com/token",
    )

    @Test
    fun `parses a real-shaped token response`() {
        var capturedUrl: String? = null
        var capturedBody: String? = null
        val client = OAuthTokenClient { url, body ->
            capturedUrl = url
            capturedBody = body
            """{"access_token": "ya29.fake-token", "expires_in": 3599, "token_type": "Bearer"}"""
        }
        val token = client.fetchAccessToken(credentials, "fake.jwt.assertion")
        assertEquals("ya29.fake-token", token)
        assertEquals("https://oauth2.googleapis.com/token", capturedUrl)
        assertTrue(capturedBody!!.contains("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"))
        assertTrue(capturedBody!!.contains("assertion=fake.jwt.assertion"))
    }

    @Test
    fun `surfaces a token endpoint error clearly instead of throwing NoSuchElementException`() {
        val client = OAuthTokenClient { _, _ ->
            """{"error": "invalid_grant", "error_description": "Invalid JWT Signature."}"""
        }
        val exception = assertThrows(FirestoreAuthException::class.java) {
            client.fetchAccessToken(credentials, "fake.jwt.assertion")
        }
        assertTrue(exception.message!!.contains("invalid_grant"))
        assertTrue(exception.message!!.contains("Invalid JWT Signature"))
    }

    @Test
    fun `missing access_token in an otherwise valid response is a clear error`() {
        val client = OAuthTokenClient { _, _ -> """{"token_type": "Bearer"}""" }
        val exception = assertThrows(FirestoreAuthException::class.java) {
            client.fetchAccessToken(credentials, "fake.jwt.assertion")
        }
        assertTrue(exception.message!!.contains("access_token"))
    }

    @Test
    fun `a transport failure is wrapped, never a raw network exception`() {
        val client = OAuthTokenClient { _, _ -> throw java.io.IOException("connection refused") }
        val exception = assertThrows(FirestoreAuthException::class.java) {
            client.fetchAccessToken(credentials, "fake.jwt.assertion")
        }
        assertTrue(exception.message!!.contains("connection refused"))
    }
}
