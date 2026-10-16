package app.simplifymy.browser.core.history

import androidx.annotation.StringRes
import app.simplifymy.browser.R
import mozilla.components.concept.storage.HistoryStorage
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

enum class HistoryPeriod(@StringRes val labelRes: Int) {
    LastHour(R.string.period_last_hour),
    Today(R.string.period_today),
    LastWeek(R.string.period_last_week),
    LastMonth(R.string.period_last_month),
    LastYear(R.string.period_last_year),
    All(R.string.period_all);

    fun since(now: Long = System.currentTimeMillis()): Long? = when (this) {
        LastHour -> now - TimeUnit.HOURS.toMillis(1)
        Today -> LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        LastWeek -> now - TimeUnit.DAYS.toMillis(7)
        LastMonth -> now - TimeUnit.DAYS.toMillis(30)
        LastYear -> now - TimeUnit.DAYS.toMillis(365)
        All -> null
    }
}

suspend fun HistoryStorage.clear(period: HistoryPeriod) {
    val since = period.since()
    if (since == null) deleteEverything() else deleteVisitsSince(since)
}
