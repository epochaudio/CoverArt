package com.example.roonplayer.domain

import java.util.concurrent.atomic.AtomicBoolean

class InFlightOperationGuard {
    private val inProgress = AtomicBoolean(false)

    fun tryStart(): Boolean = inProgress.compareAndSet(false, true)

    fun finish() {
        inProgress.set(false)
    }

    fun isInProgress(): Boolean = inProgress.get()
}
