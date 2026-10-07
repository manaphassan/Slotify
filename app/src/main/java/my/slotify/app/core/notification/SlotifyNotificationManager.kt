package my.slotify.app.core.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import my.slotify.app.MainActivity
import java.util.Calendar

object SlotifyNotificationManager {

    const val CHANNEL_ATTIRE = "slotify_attire_channel"
    const val CHANNEL_TRANSIT = "slotify_transit_channel"
    const val CHANNEL_FINANCE = "slotify_finance_channel"

    const val NOTIFICATION_ID_TEST = 1001
    const val NOTIFICATION_ID_ATTIRE = 1002
    const val NOTIFICATION_ID_TRANSIT = 1003
    const val NOTIFICATION_ID_FINANCE = 1004

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val attireChannel = NotificationChannel(
                CHANNEL_ATTIRE,
                "Slotify: Peringatan Uniform Sekolah",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Peringatan jadual uniform KPM dan buku sekolah harian"
                enableVibration(true)
            }

            val transitChannel = NotificationChannel(
                CHANNEL_TRANSIT,
                "Slotify: Transit & Logistik",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Peringatan drop beg transit dan jadual van sekolah"
                enableVibration(true)
            }

            val financeChannel = NotificationChannel(
                CHANNEL_FINANCE,
                "Slotify: Peringatan Yuran",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Peringatan komitmen yuran bulanan dan belanja kantin"
            }

            notificationManager.createNotificationChannels(
                listOf(attireChannel, transitChannel, financeChannel)
            )
        }
    }

    private fun getLaunchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun showTestNotification(context: Context, title: String, message: String) {
        createNotificationChannels(context)

        val pendingIntent = getLaunchPendingIntent(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_FINANCE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TEST, builder.build())
        } catch (_: SecurityException) {
            // Handled when permission is missing on Android 13+
        }
    }

    fun showAttireReminder(context: Context, childName: String, attire: String, books: String) {
        createNotificationChannels(context)

        val pendingIntent = getLaunchPendingIntent(context)
        val title = "Peringatan Uniform Esok: $childName"
        val body = "Pakaian: $attire\nBawa: $books"

        val builder = NotificationCompat.Builder(context, CHANNEL_ATTIRE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Uniform: $attire • Semak beg sekolah!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ATTIRE, builder.build())
        } catch (e: Exception) {
            Log.e("SlotifyNotification", "Failed to send attire reminder", e)
        }
    }

    fun showTransitAlert(context: Context, childName: String, transitMsg: String) {
        createNotificationChannels(context)

        val pendingIntent = getLaunchPendingIntent(context)
        val title = "Logistik Transit: $childName"

        val builder = NotificationCompat.Builder(context, CHANNEL_TRANSIT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(transitMsg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(transitMsg))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TRANSIT, builder.build())
        } catch (e: Exception) {
            Log.e("SlotifyNotification", "Failed to send transit alert", e)
        }
    }

    fun scheduleDailyReminders(context: Context) {
        createNotificationChannels(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val immutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        // Schedule Evening Attire Reminder at 8:30 PM (20:30)
        val eveningIntent = Intent(context, SlotifyAlarmReceiver::class.java).apply {
            action = SlotifyAlarmReceiver.ACTION_EVENING_ATTIRE
        }
        val eveningPending = PendingIntent.getBroadcast(
            context,
            2001,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag
        )

        val eveningCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            eveningCalendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            eveningPending
        )

        // Fix 2.4: Schedule Morning Transit Bag Drop alert at 06:30 AM (previously dead code)
        val morningIntent = Intent(context, SlotifyAlarmReceiver::class.java).apply {
            action = SlotifyAlarmReceiver.ACTION_MORNING_TRANSIT
        }
        val morningPending = PendingIntent.getBroadcast(
            context,
            2002,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag
        )

        val morningCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            morningCalendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            morningPending
        )
    }
}
