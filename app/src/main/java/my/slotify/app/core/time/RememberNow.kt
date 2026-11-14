package my.slotify.app.core.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Current local date-time, refreshed at the start of every minute. */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        val now = LocalDateTime.now()
        value = now
        delay(((60 - now.second) * 1000L).coerceAtLeast(1000L))
    }
}
