package my.slotify.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import my.slotify.app.core.designsystem.theme.SlotifyColors

/**
 * Header for a group of cards. Tapping it collapses or expands the group; [summary] is shown
 * under the title so a collapsed group still says what is inside.
 */
@Composable
fun SlotifySectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    expandLabel: String,
    collapseLabel: String,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "SectionChevron")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onToggle, role = Role.Button)
            .semantics { stateDescription = if (expanded) collapseLabel else expandLabel }
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SlotifyColors.TextPrimary
            )
            if (!summary.isNullOrBlank() && !expanded) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = SlotifyColors.TextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = SlotifyColors.TextSecondary,
            modifier = Modifier.rotate(rotation)
        )
    }
}

/** Adds a collapsible group to a lazy list: a [SlotifySectionHeader] followed by [content] when expanded. */
fun LazyListScope.slotifySection(
    key: String,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    expandLabel: String,
    collapseLabel: String,
    summary: String? = null,
    content: LazyListScope.() -> Unit
) {
    item(key = "section-$key") {
        SlotifySectionHeader(title, expanded, onToggle, expandLabel, collapseLabel, summary = summary)
    }
    if (expanded) content()
}
