package com.example.cropcare

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class CropCareHttpClientTest {
    @Test fun objectArrayEmptyAndMalformedResponses() {
        assertNotNull(FarmerResponseParser.parse(200, "{}").data)
        assertNotNull(FarmerResponseParser.parse(200, "[]").array)
        assertTrue(FarmerResponseParser.parse(204, "").successful)
        assertFalse(FarmerResponseParser.parse(200, "<html>unavailable</html>").successful)
        listOf(400, 401, 403, 404, 409, 429, 503).forEach { code ->
            val result = FarmerResponseParser.parse(code, "{\"error\":\"Try again\"}")
            assertFalse(result.successful); assertEquals(code, result.statusCode); assertEquals("Try again", result.message)
        }
    }
    @Test fun wirePreservesJsonAndOnlyAddsExplicitBearerToken() {
        val server = MockWebServer(); server.start()
        val client = OkHttpClient()
        try {
            server.enqueue(MockResponse().setResponseCode(202).setBody("{\"status\":\"pending\"}"))
            val request = CropCareWireClient.build(server.url("/api/subscriptions/request").toString(), "POST",
                JSONObject().put("referenceNumber", "0012345678"), "test-token")
            client.newCall(request).execute().use { assertEquals(202, it.code) }
            val received = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("POST", received.method); assertEquals("Bearer test-token", received.getHeader("Authorization"))
            assertEquals("0012345678", JSONObject(received.body.readUtf8()).getString("referenceNumber"))
            assertNull(CropCareWireClient.build(server.url("/api/recovery/request").toString(), "POST", JSONObject(), null).header("Authorization"))
        } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown() }
    }
    @Test fun problemDetailsAndNonJsonErrorsRemainFailures() {
        assertEquals("Invalid input", FarmerResponseParser.parse(400, "{\"title\":\"Invalid input\",\"errors\":{}}").message)
        assertFalse(FarmerResponseParser.parse(503, "service unavailable").successful)
    }
    @Test fun cancellationBeforeAndAfterAttachmentCancelsTheCall() {
        val client = OkHttpClient()
        val first = client.newCall(CropCareWireClient.build("http://localhost/test", "GET", null, null))
        val before = ApiRequestHandle(); before.cancel(); before.attach(first)
        assertTrue(first.isCanceled()); assertFalse(before.active())
        val second = client.newCall(CropCareWireClient.build("http://localhost/test", "GET", null, null))
        val after = ApiRequestHandle(); after.attach(second); after.cancel()
        assertTrue(second.isCanceled()); assertFalse(after.active())
    }
    @Test fun stalledResponseTimesOutWithoutSuccess() {
        val server = MockWebServer(); server.start()
        val client = OkHttpClient.Builder().readTimeout(100, TimeUnit.MILLISECONDS).build()
        try {
            server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
            val request = CropCareWireClient.build(server.url("/test").toString(), "GET", null, null)
            try {
                client.newCall(request).execute().use { fail("A stalled server must not return success") }
            } catch (_: java.io.IOException) { /* Expected transport failure. */ }
        } finally { client.dispatcher.executorService.shutdown(); client.connectionPool.evictAll(); server.shutdown() }
    }}
