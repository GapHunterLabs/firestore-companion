package dev.gaphunter.firestorecompanion.rest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreEmulatorHostTest {
    @Test
    fun `accepts host and port as the Firebase tools print them`() {
        assertEquals("localhost:8080", FirestoreEmulatorHost.parse("localhost:8080"))
        assertEquals("127.0.0.1:8080", FirestoreEmulatorHost.parse("  127.0.0.1:8080 "))
        assertEquals("[::1]:8080", FirestoreEmulatorHost.parse("[::1]:8080"))
        assertEquals("firestore-emulator:8080", FirestoreEmulatorHost.parse("firestore-emulator:8080"))
    }

    @Test
    fun `drops a leading http scheme and a trailing slash`() {
        assertEquals("localhost:8080", FirestoreEmulatorHost.parse("http://localhost:8080/"))
        assertEquals("localhost:8080", FirestoreEmulatorHost.parse("HTTP://localhost:8080"))
    }

    @Test
    fun `rejects https, a missing port and a path`() {
        val https = assertThrows(IllegalArgumentException::class.java) { FirestoreEmulatorHost.parse("https://localhost:8080") }
        assertTrue(https.message!!.contains("plain HTTP"))
        assertThrows(IllegalArgumentException::class.java) { FirestoreEmulatorHost.parse("localhost") }
        assertThrows(IllegalArgumentException::class.java) { FirestoreEmulatorHost.parse("localhost:8080/v1") }
        assertThrows(IllegalArgumentException::class.java) { FirestoreEmulatorHost.parse("") }
    }

    @Test
    fun `rejects a port outside the valid range`() {
        val e = assertThrows(IllegalArgumentException::class.java) { FirestoreEmulatorHost.parse("localhost:70000") }
        assertTrue(e.message!!.contains("70000"))
        assertThrows(IllegalArgumentException::class.java) { FirestoreEmulatorHost.parse("localhost:0") }
    }

    @Test
    fun `the client talks to the emulator over plain http with the owner token`() {
        val urls = mutableListOf<String>()
        val client = FirestoreRestClient(
            projectId = "demo-store",
            accessToken = FirestoreEmulatorHost.TOKEN,
            apiRoot = FirestoreEmulatorHost.apiRoot("localhost:8080"),
            httpGet = { url -> urls += url; """{"documents": []}""" },
            httpPost = { url, _ -> urls += url; """{"collectionIds": ["orders"]}""" },
            httpPatch = { url, _ -> urls += url; """{"name": "projects/demo-store/databases/(default)/documents/orders/o1"}""" },
        )
        assertEquals(listOf("orders"), client.listRootCollectionIds())
        client.listDocuments("orders")
        client.patchDocument(
            "projects/demo-store/databases/(default)/documents/orders/o1",
            dev.gaphunter.firestorecompanion.json.JsonNode.Obj(
                linkedMapOf("status" to dev.gaphunter.firestorecompanion.json.JsonNode.Obj(linkedMapOf("stringValue" to dev.gaphunter.firestorecompanion.json.JsonNode.Str("shipped"))))
            ),
        )
        assertEquals("http://localhost:8080/v1/projects/demo-store/databases/(default)/documents:listCollectionIds", urls[0])
        assertEquals("http://localhost:8080/v1/projects/demo-store/databases/(default)/documents/orders", urls[1])
        // The document name is percent-encoded per segment, "(default)" included, as against Google Cloud.
        assertTrue(urls[2].startsWith("http://localhost:8080/v1/projects/demo-store/databases/%28default%29/documents/orders/o1?"))
        assertTrue(urls.none { it.contains("googleapis.com") })
    }

    @Test
    fun `without an emulator the client still talks to Google Cloud`() {
        var url: String? = null
        val client = FirestoreRestClient(
            projectId = "p",
            accessToken = "t",
            httpGet = { error("not expected") },
            httpPost = { u, _ -> url = u; """{"collectionIds": []}""" },
            httpPatch = { _, _ -> error("not expected") },
        )
        client.listRootCollectionIds()
        assertEquals("https://firestore.googleapis.com/v1/projects/p/databases/(default)/documents:listCollectionIds", url)
    }
}
