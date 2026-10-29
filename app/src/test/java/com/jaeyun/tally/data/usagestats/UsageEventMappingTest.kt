package com.jaeyun.tally.data.usagestats

import com.jaeyun.tally.domain.model.RawEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** `UsageEvents.Event` 상수값으로 고정한다. 플랫폼 상수가 바뀌면 여기서 드러난다 */
class UsageEventMappingTest {

    @Test
    fun `복원에 쓰는 4종만 옮긴다`() {
        assertEquals(RawEventType.ACTIVITY_RESUMED, rawEventTypeOf(1))
        assertEquals(RawEventType.SCREEN_INTERACTIVE, rawEventTypeOf(15))
        assertEquals(RawEventType.SCREEN_NON_INTERACTIVE, rawEventTypeOf(16))
        assertEquals(RawEventType.KEYGUARD_HIDDEN, rawEventTypeOf(18))
    }

    @Test
    fun `나머지 이벤트는 버린다`() {
        assertNull(rawEventTypeOf(2)) // ACTIVITY_PAUSED — 다음 RESUMED가 곧 끝이라 쓰지 않는다 (§6.1)
        assertNull(rawEventTypeOf(23)) // ACTIVITY_STOPPED
        assertNull(rawEventTypeOf(17)) // KEYGUARD_SHOWN
        assertNull(rawEventTypeOf(10)) // NOTIFICATION_SEEN
        assertNull(rawEventTypeOf(0)) // NONE
    }
}
