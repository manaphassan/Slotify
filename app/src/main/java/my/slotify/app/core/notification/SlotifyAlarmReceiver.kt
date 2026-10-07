package my.slotify.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SlotifyAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EVENING_ATTIRE = "my.slotify.app.ACTION_EVENING_ATTIRE"
        const val ACTION_MORNING_TRANSIT = "my.slotify.app.ACTION_MORNING_TRANSIT"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_EVENING_ATTIRE -> {
                SlotifyNotificationManager.showAttireReminder(
                    context = context,
                    childName = "Adam & Adam",
                    attire = "Baju T-Shirt Sukan SK / Seluar Track",
                    books = "Buku Teks Matematik, Sains, B. Melayu"
                )
            }
            ACTION_MORNING_TRANSIT -> {
                SlotifyNotificationManager.showTransitAlert(
                    context = context,
                    childName = "Adam & Adam",
                    transitMsg = "Hantar Beg Transit & Baju SK ke Taska Ceria!"
                )
            }
            else -> {
                SlotifyNotificationManager.showTestNotification(
                    context = context,
                    title = "Slotify Routine Alert",
                    message = "Semak jadual aktiviti dan pakaian seragam hari ini!"
                )
            }
        }
    }
}
