package my.slotify.app.feature.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.strings

private val KEY_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("C", "0", "⌫")
)

/** Shared 4-digit PIN keypad dialog used by approval, setup and change-PIN flows. */
@Composable
internal fun PinEntryDialog(
    title: String,
    subtitle: String,
    message: String?,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onPinComplete: (String) -> Unit
) {
    var enteredPin by remember(title) { mutableStateOf("") }
    val str = strings

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlotifyColors.SurfaceCard, SlotifyShapes.XLarge)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = SlotifyColors.KpmBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SlotifyColors.TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = str.close, tint = SlotifyColors.TextSecondary)
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = SlotifyColors.TextSecondary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .semantics { contentDescription = str.pinDigitsEntered(enteredPin.length) }
                ) {
                    for (i in 0 until 4) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    if (i < enteredPin.length) SlotifyColors.KpmBlue else SlotifyColors.TextTertiary.copy(
                                        alpha = 0.3f
                                    ),
                                    CircleShape
                                )
                        )
                    }
                }

                if (message != null) {
                    Text(
                        text = message,
                        color = SlotifyColors.EnglishRed,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in KEY_ROWS) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (key in row) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable(enabled = enabled) {
                                            when (key) {
                                                "C" -> enteredPin = ""
                                                "⌫" -> enteredPin = enteredPin.dropLast(1)
                                                else -> if (enteredPin.length < 4) {
                                                    val next = enteredPin + key
                                                    if (next.length == 4) {
                                                        enteredPin = ""
                                                        onPinComplete(next)
                                                    } else {
                                                        enteredPin = next
                                                    }
                                                }
                                            }
                                        },
                                    shape = SlotifyShapes.Small,
                                    color = SlotifyColors.CanvasBase
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = if (enabled) SlotifyColors.TextPrimary else SlotifyColors.TextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
