package my.slotify.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.slotify.app.core.designsystem.theme.SlotStatus
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyShapes
import my.slotify.app.core.designsystem.theme.statusColors

/** Big screen title with an optional date/subtitle (style guide: screen title + body). */
@Composable
fun SlotifyScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 4.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.displayLarge, color = SlotifyColors.TextPrimary)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = SlotifyColors.TextSecondary)
        }
    }
}

/** Section heading such as "Jadual hari ini". */
@Composable
fun SlotifySectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.displaySmall,
            color = SlotifyColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Status pill: colour, icon and label together so the state reads before the details. */
@Composable
fun StatusChip(text: String, status: SlotStatus, modifier: Modifier = Modifier, onHero: Boolean = false) {
    val c = statusColors(status)
    val bg = if (onHero) Color.White.copy(alpha = 0.18f) else c.accent.copy(alpha = 0.14f)
    val fg = if (onHero) Color.White else c.onContainer
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics { stateDescription = text },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            SlotStatus.COMPLETED, SlotStatus.ON_TIME ->
                Icon(Icons.Default.Check, null, tint = fg, modifier = Modifier.size(13.dp))

            SlotStatus.NOW -> Box(Modifier.size(6.dp).background(fg, CircleShape))
            SlotStatus.NEXT, SlotStatus.UPCOMING, SlotStatus.DELAYED, SlotStatus.OFFLINE ->
                Icon(Icons.Outlined.Schedule, null, tint = fg, modifier = Modifier.size(13.dp))

            else -> {}
        }
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
    }
}

/** Icon + text metadata line (time, room, teacher). */
@Composable
fun MetaLine(icon: ImageVector, text: String, color: Color = SlotifyColors.TextSecondary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Schedule row: tinted card with a coloured accent bar. [accent] defaults to the status colour;
 * pass a subject colour to make courses recognisable at a glance.
 */
@Composable
fun ScheduleCard(
    title: String,
    time: String,
    status: SlotStatus,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleIcon: ImageVector = Icons.Default.Place,
    chip: String? = null,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    footer: @Composable (ColumnScope.() -> Unit)? = null
) {
    val c = statusColors(status)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(SlotifyShapes.Large)
            .background(c.container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accent ?: c.accent)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SlotifyColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (chip != null) {
                    Spacer(Modifier.width(8.dp))
                    StatusChip(chip, status)
                }
            }
            MetaLine(Icons.Outlined.Schedule, time)
            if (!subtitle.isNullOrBlank()) MetaLine(subtitleIcon, subtitle)
            footer?.invoke(this)
        }
        trailing?.invoke()
    }
}

/**
 * Brand hero card ("KELAS SEMASA"). Decorative circles echo the reference's wave; content is
 * white on the deep brand colour.
 */
@Composable
fun HeroCard(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    chip: String? = null,
    chipStatus: SlotStatus = SlotStatus.NOW,
    container: Color = SlotifyColors.HeroContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SlotifyShapes.Large)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                Color.White.copy(alpha = 0.07f),
                radius = size.height * 0.95f,
                center = Offset(size.width * 1.02f, size.height * 1.15f)
            )
            drawCircle(
                Color.White.copy(alpha = 0.06f),
                radius = size.height * 0.6f,
                center = Offset(size.width * 0.86f, size.height * 1.05f)
            )
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                if (chip != null) StatusChip(chip, chipStatus, onHero = true)
            }
            Text(
                title,
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            content()
        }
    }
}

/** White metadata line for use inside [HeroCard]. */
@Composable
fun HeroMetaLine(icon: ImageVector, text: String) = MetaLine(icon, text, Color.White.copy(alpha = 0.9f))
