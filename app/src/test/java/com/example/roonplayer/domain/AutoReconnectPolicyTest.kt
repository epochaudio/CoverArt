package com.example.roonplayer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoReconnectPolicyTest {

    private val policy = AutoReconnectPolicy()
    private val now = 1_000_000_000L
    private val weekMs = 7L * 24 * 60 * 60 * 1000

    @Test
    fun `allows reconnect when host and port are valid and connection is recent`() {
        val decision = policy.decide(
            lastHost = "192.168.1.100",
            lastPort = 9330,
            lastConnectionTime = now - 1000,
            now = now,
            isValidHost = { true }
        )

        assertTrue(decision.shouldReconnect)
        assertEquals("recent_success", decision.reason)
    }

    @Test
    fun `rejects reconnect when host is missing`() {
        val decision = policy.decide(
            lastHost = null,
            lastPort = 9330,
            lastConnectionTime = now,
            now = now,
            isValidHost = { true }
        )

        assertFalse(decision.shouldReconnect)
        assertEquals("missing_host", decision.reason)
    }

    @Test
    fun `rejects reconnect when port is invalid`() {
        val decision = policy.decide(
            lastHost = "192.168.1.100",
            lastPort = 0,
            lastConnectionTime = now,
            now = now,
            isValidHost = { true }
        )

        assertFalse(decision.shouldReconnect)
        assertEquals("invalid_port", decision.reason)
    }

    @Test
    fun `rejects reconnect when host validator fails`() {
        val decision = policy.decide(
            lastHost = "by_core_id_xxx",
            lastPort = 9330,
            lastConnectionTime = now,
            now = now,
            isValidHost = { host -> !host.startsWith("by_core_id_") }
        )

        assertFalse(decision.shouldReconnect)
        assertEquals("invalid_host", decision.reason)
    }

    @Test
    fun `rejects reconnect when last connection is stale`() {
        val decision = policy.decide(
            lastHost = "192.168.1.100",
            lastPort = 9330,
            lastConnectionTime = now - weekMs - 1,
            now = now,
            isValidHost = { true }
        )

        assertFalse(decision.shouldReconnect)
        assertEquals("stale_connection", decision.reason)
    }

    @Test
    fun `treats exactly seven days old connection as stale to match app logic`() {
        val decision = policy.decide(
            lastHost = "192.168.1.100",
            lastPort = 9330,
            lastConnectionTime = now - weekMs,
            now = now,
            isValidHost = { true }
        )

        assertFalse(decision.shouldReconnect)
        assertEquals("stale_connection", decision.reason)
    }
}
