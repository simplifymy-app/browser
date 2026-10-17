package app.simplifymy.browser.core.history

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class HistoryPeriodTest {

    private val zone = ZoneId.of("Europe/Kyiv")
    private val systemZone = TimeZone.getDefault()
    private val now = ZonedDateTime.of(2026, 3, 29, 1, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Before
    fun useFixedZone() = TimeZone.setDefault(TimeZone.getTimeZone(zone))

    @After
    fun restoreZone() = TimeZone.setDefault(systemZone)

    @Test
    fun `today starts at local midnight of the given moment`() {
        val midnight = ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(midnight, HistoryPeriod.Today.since(now))
    }

    @Test
    fun `today follows now, not the wall clock`() {
        val earlier = ZonedDateTime.of(2020, 1, 15, 23, 59, 0, 0, zone).toInstant().toEpochMilli()
        val midnight = ZonedDateTime.of(2020, 1, 15, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(midnight, HistoryPeriod.Today.since(earlier))
    }

    @Test
    fun `fixed periods count back from now`() {
        assertEquals(now - TimeUnit.HOURS.toMillis(1), HistoryPeriod.LastHour.since(now))
        assertEquals(now - TimeUnit.DAYS.toMillis(7), HistoryPeriod.LastWeek.since(now))
        assertEquals(now - TimeUnit.DAYS.toMillis(30), HistoryPeriod.LastMonth.since(now))
        assertEquals(now - TimeUnit.DAYS.toMillis(365), HistoryPeriod.LastYear.since(now))
    }

    @Test
    fun `all has no lower bound`() {
        assertNull(HistoryPeriod.All.since(now))
    }
}
