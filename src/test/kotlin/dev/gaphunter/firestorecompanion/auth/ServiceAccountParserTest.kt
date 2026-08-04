package dev.gaphunter.firestorecompanion.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Direct regression tests for the cited competitor bug: "Connection test
 * failed: java.util.NoSuchElementException: Collection contains no
 * element matching the predicate." Every malformed/missing-field case
 * here must fail with a clear [FirestoreAuthException] naming the exact
 * field -- never [NoSuchElementException].
 */
class ServiceAccountParserTest {
    private val validJson = """
        {
          "type": "service_account",
          "project_id": "acme-corp-prod",
          "private_key_id": "abc123",
          "private_key": "-----BEGIN PRIVATE KEY-----\nMIIBVQ==\n-----END PRIVATE KEY-----\n",
          "client_email": "firestore-reader@acme-corp-prod.iam.gserviceaccount.com",
          "client_id": "123456789",
          "token_uri": "https://oauth2.googleapis.com/token"
        }
    """.trimIndent()

    @Test
    fun `parses a well-formed service account file`() {
        val credentials = ServiceAccountParser.parse(validJson)
        assertEquals("acme-corp-prod", credentials.projectId)
        assertEquals("firestore-reader@acme-corp-prod.iam.gserviceaccount.com", credentials.clientEmail)
        assertEquals("https://oauth2.googleapis.com/token", credentials.tokenUri)
        assertTrue(credentials.privateKeyPem.contains("BEGIN PRIVATE KEY"))
    }

    @Test
    fun `defaults token_uri when absent`() {
        val json = """{"type": "service_account", "project_id": "p", "client_email": "e@p.iam.gserviceaccount.com", "private_key": "k"}"""
        val credentials = ServiceAccountParser.parse(json)
        assertEquals("https://oauth2.googleapis.com/token", credentials.tokenUri)
    }

    @Test
    fun `empty json object throws a clear error naming the missing field, not NoSuchElementException`() {
        val exception = assertThrows(FirestoreAuthException::class.java) {
            ServiceAccountParser.parse("{}")
        }
        assertTrue(exception.message!!.contains("type"))
    }

    @Test
    fun `missing project_id names the field explicitly`() {
        val json = """{"type": "service_account", "client_email": "e@p.iam.gserviceaccount.com", "private_key": "k"}"""
        val exception = assertThrows(FirestoreAuthException::class.java) {
            ServiceAccountParser.parse(json)
        }
        assertTrue(exception.message!!.contains("project_id"))
    }

    @Test
    fun `wrong type value is rejected with a clear message`() {
        val json = """{"type": "authorized_user", "project_id": "p", "client_email": "e", "private_key": "k"}"""
        val exception = assertThrows(FirestoreAuthException::class.java) {
            ServiceAccountParser.parse(json)
        }
        assertTrue(exception.message!!.contains("authorized_user"))
    }

    @Test
    fun `malformed json is rejected with a clear message, not a crash`() {
        val exception = assertThrows(FirestoreAuthException::class.java) {
            ServiceAccountParser.parse("not json at all")
        }
        assertTrue(exception.message!!.isNotBlank())
    }

    @Test
    fun `application default credentials json is rejected with a clear message`() {
        // ADC files use "authorized_user" type and a completely different
        // shape (client_secret/refresh_token, no private_key/client_email)
        // -- one of the two cited "can neither connect with X nor Y" cases.
        val json = """
            {
              "type": "authorized_user",
              "client_id": "x.apps.googleusercontent.com",
              "client_secret": "secret",
              "refresh_token": "token"
            }
        """.trimIndent()
        val exception = assertThrows(FirestoreAuthException::class.java) {
            ServiceAccountParser.parse(json)
        }
        assertTrue(exception.message!!.contains("service_account"))
    }

    @Test
    fun `blank private_key is rejected as empty, not accepted silently`() {
        val json = """{"type": "service_account", "project_id": "p", "client_email": "e", "private_key": ""}"""
        val exception = assertThrows(FirestoreAuthException::class.java) {
            ServiceAccountParser.parse(json)
        }
        assertTrue(exception.message!!.contains("private_key"))
    }
}
