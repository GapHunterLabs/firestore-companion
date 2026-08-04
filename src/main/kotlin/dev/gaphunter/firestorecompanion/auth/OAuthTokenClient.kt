package dev.gaphunter.firestorecompanion.auth

import dev.gaphunter.firestorecompanion.json.JsonNode
import dev.gaphunter.firestorecompanion.json.MinimalJsonParser
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * Exchanges a signed JWT assertion for a real OAuth2 access token: a
 * single HTTP POST to the token endpoint named in the service account
 * file itself (never a hardcoded/guessed URI), no interactive browser
 * flow. [httpPost] is injectable so tests can supply a fake HTTP
 * transport (a captured real response fixture) instead of a live
 * network call -- see OAuthTokenClientTest.
 */
class OAuthTokenClient(
    private val httpPost: (url: String, formBody: String) -> String = ::defaultHttpPost,
) {
    fun fetchAccessToken(credentials: ServiceAccountCredentials, signedAssertion: String): String {
        val formBody = "grant_type=" + urlEncode("urn:ietf:params:oauth:grant-type:jwt-bearer") +
            "&assertion=" + urlEncode(signedAssertion)

        val responseBody = try {
            httpPost(credentials.tokenUri, formBody)
        } catch (e: Exception) {
            throw FirestoreAuthException("Token request to ${credentials.tokenUri} failed: ${e.message}")
        }

        val root = try {
            MinimalJsonParser.parse(responseBody)
        } catch (e: Exception) {
            throw FirestoreAuthException("Token endpoint returned a response that isn't valid JSON")
        }
        val obj = root as? JsonNode.Obj
            ?: throw FirestoreAuthException("Token endpoint response was not a JSON object")

        val errorNode = obj.entries["error"] as? JsonNode.Str
        if (errorNode != null) {
            val description = (obj.entries["error_description"] as? JsonNode.Str)?.value
            throw FirestoreAuthException(
                "Token endpoint rejected the request: ${errorNode.value}" +
                    (description?.let { " ($it)" } ?: "")
            )
        }

        return (obj.entries["access_token"] as? JsonNode.Str)?.value
            ?: throw FirestoreAuthException("Token endpoint response is missing \"access_token\"")
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    companion object {
        private val client = HttpClient.newHttpClient()

        fun defaultHttpPost(url: String, formBody: String): String {
            val request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build()
            return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
        }
    }
}
