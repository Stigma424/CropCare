package com.example.cropcare

/** Never persisted: process death requires starting recovery again. */
object RecoverySession {
    var email: String? = null
        private set
    private var ticket: String? = null
    internal var clock: () -> Long = { System.nanoTime() / 1_000_000 }
    private var expiresAt = 0L
    fun start(value: String) { clear(); email = value }
    fun verified(value: String) { ticket = value; expiresAt = clock() + 600_000L }
    fun ticket(): String? = ticket?.takeIf { clock() < expiresAt }
    fun clear() { email = null; ticket = null; expiresAt = 0 }
}
