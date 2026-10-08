package my.slotify.app.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SlotifyNotificationManagerTest {

    @Test
    fun testNotificationChannelsAndIdsAreUnique() {
        val channels = listOf(
            SlotifyNotificationManager.CHANNEL_ATTIRE,
            SlotifyNotificationManager.CHANNEL_TRANSIT,
            SlotifyNotificationManager.CHANNEL_FINANCE
        )
        // All channels must be distinct strings
        assertEquals(3, channels.distinct().size)

        val ids = listOf(
            SlotifyNotificationManager.NOTIFICATION_ID_TEST,
            SlotifyNotificationManager.NOTIFICATION_ID_ATTIRE,
            SlotifyNotificationManager.NOTIFICATION_ID_TRANSIT,
            SlotifyNotificationManager.NOTIFICATION_ID_FINANCE
        )
        // All IDs must be distinct integers
        assertEquals(4, ids.distinct().size)
    }

    @Test
    fun testChannelNamingConvention() {
        assertEquals("slotify_attire_channel", SlotifyNotificationManager.CHANNEL_ATTIRE)
        assertEquals("slotify_transit_channel", SlotifyNotificationManager.CHANNEL_TRANSIT)
        assertEquals("slotify_finance_channel", SlotifyNotificationManager.CHANNEL_FINANCE)
    }
}
