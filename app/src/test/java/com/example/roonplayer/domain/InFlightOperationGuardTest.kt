package com.example.roonplayer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InFlightOperationGuardTest {

    @Test
    fun `allows only one start until finish`() {
        val guard = InFlightOperationGuard()

        assertTrue(guard.tryStart())
        assertFalse(guard.tryStart())

        guard.finish()

        assertTrue(guard.tryStart())
    }

    @Test
    fun `isInProgress reflects current state`() {
        val guard = InFlightOperationGuard()

        assertFalse(guard.isInProgress())
        guard.tryStart()
        assertTrue(guard.isInProgress())
        guard.finish()
        assertFalse(guard.isInProgress())
    }

    @Test
    fun `concurrent starts should have only one winner`() {
        val guard = InFlightOperationGuard()
        val pool = Executors.newFixedThreadPool(8)
        val tasks = (1..32).map {
            pool.submit<Boolean> { guard.tryStart() }
        }

        val successCount = tasks.count { it.get() }

        pool.shutdown()
        pool.awaitTermination(3, TimeUnit.SECONDS)

        assertEquals(1, successCount)
        assertTrue(guard.isInProgress())
    }
}
