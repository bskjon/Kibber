package no.iktdev.kibber

import no.iktdev.tibber.TibberPulseSubscription

interface TibberListener {
    fun onMeasurementReceived(measurement: TibberPulseSubscription.LiveMeasurement)
    fun onInvalidMeasurementReceived()
    fun onMeasurementRetry(attempt: Long, throwable: Throwable? = null) {}
    fun onRetryExited(attempt: Long, throwable: Throwable? = null, message: String = "") {}
}