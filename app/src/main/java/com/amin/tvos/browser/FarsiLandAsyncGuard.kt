package com.amin.tvos.browser

/** Discards late JS replies/timeouts from an older document, retry or activity lifetime. */
internal class FarsiLandAsyncGuard {
    data class Ticket(val epoch: Long, val request: Long)
    private var epoch = 0L
    private var sequence = 0L
    private var inspection = 0L
    private var recovery = 0L

    fun invalidate() { epoch++; inspection = 0; recovery = 0 }
    fun beginInspection(): Ticket = Ticket(epoch, ++sequence).also { inspection = it.request }
    fun expireInspection() { inspection = 0 }
    fun currentInspection(ticket: Ticket) = ticket.epoch == epoch &&
        inspection != 0L && ticket.request == inspection
    fun finishInspection(ticket: Ticket) { if (currentInspection(ticket)) inspection = 0 }
    fun beginRecovery(): Ticket {
        expireInspection()
        return Ticket(epoch, ++sequence).also { recovery = it.request }
    }
    fun currentRecovery(ticket: Ticket) = ticket.epoch == epoch &&
        recovery != 0L && ticket.request == recovery
    fun finishRecovery(ticket: Ticket) { if (currentRecovery(ticket)) recovery = 0 }
}
