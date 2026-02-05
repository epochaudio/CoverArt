package com.example.roonplayer.domain

data class AutoReconnectDecision(
    val shouldReconnect: Boolean,
    val reason: String
)

class AutoReconnectPolicy(
    private val validWindowMs: Long = 7L * 24 * 60 * 60 * 1000
) {
    fun decide(
        lastHost: String?,
        lastPort: Int,
        lastConnectionTime: Long,
        now: Long = System.currentTimeMillis(),
        isValidHost: (String) -> Boolean
    ): AutoReconnectDecision {
        if (lastHost.isNullOrBlank()) {
            return AutoReconnectDecision(false, "missing_host")
        }
        if (lastPort <= 0) {
            return AutoReconnectDecision(false, "invalid_port")
        }
        if (!isValidHost(lastHost)) {
            return AutoReconnectDecision(false, "invalid_host")
        }

        val cutoff = now - validWindowMs
        if (lastConnectionTime <= cutoff) {
            return AutoReconnectDecision(false, "stale_connection")
        }

        return AutoReconnectDecision(true, "recent_success")
    }
}
