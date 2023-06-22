package com.aftertime

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import junit.framework.TestCase.assertEquals
import kotlin.test.Test

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
//        application {
//            configureRouting()
//        }
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Healthy!", bodyAsText())
        }
    }
}
