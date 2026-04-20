package com.dakotagroupstaff.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * SessionManager — Global event bus for session lifecycle events.
 *
 * Responsibilities:
 * - Broadcast "session expired" event when refresh token is no longer valid
 * - Any Activity/Fragment can observe [sessionExpiredEvent] and redirect to login
 *
 * This is a singleton injected via Koin, ensuring all components share the same instance.
 */
class SessionManager {

    private val _sessionExpiredEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /**
     * Observe this flow to react to session expiry (e.g., redirect to LoginActivity).
     * The emitted String contains a user-friendly message explaining the reason.
     */
    val sessionExpiredEvent: SharedFlow<String> = _sessionExpiredEvent.asSharedFlow()

    /**
     * Call this to broadcast a session expired event.
     * Should be called from [TokenAuthenticator] when refresh token fails.
     *
     * @param reason A user-facing message (e.g., "Sesi anda telah berakhir, silahkan login kembali")
     */
    fun emitSessionExpired(reason: String = "Sesi anda telah berakhir, silahkan login kembali") {
        _sessionExpiredEvent.tryEmit(reason)
    }
}
