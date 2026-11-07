package dev.carince.eraser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.carince.eraser.data.DueItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DueListScreen(
    items: List<DueItem>,
    now: Instant,
    zone: ZoneId,
    onRefresh: () -> Unit,
    onEditFeed: () -> Unit,
    refreshing: Boolean = false,
    warning: String? = null,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        val visible = items.filter { it.dueAt != null && isActionable(it.dueAt, now, zone) }
        val grouped = visible.groupBy { bucketFor(it.dueAt!!, it.dueHasTime, now, zone) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today",
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (visible.size == 1) "1 item" else "${visible.size} items",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onRefresh) {
                    Text("Refresh", fontSize = 12.5.sp)
                }
                TextButton(onClick = onEditFeed) {
                    Text("Feed", fontSize = 12.5.sp)
                }
            }

            if (refreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }

            if (warning != null) {
                Text(
                    text = warning,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                )
            }

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No deadlines in this feed yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Scaffold
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                bucketOrder.forEach { bucket ->
                    val bucketItems = grouped[bucket].orEmpty()
                    if (bucketItems.isEmpty()) return@forEach

                    item(key = "header-$bucket") {
                        SectionHeader(bucket)
                    }

                    items(bucketItems, key = { it.id }) { item ->
                        DueRow(item, now, zone)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(bucket: DueBucket) {
    Text(
        text = when (bucket) {
            DueBucket.OVERDUE -> "Overdue"
            DueBucket.TODAY -> "Today"
            DueBucket.THIS_WEEK -> "This week"
            DueBucket.LATER -> "Later"
        }.uppercase(Locale.getDefault()),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun DueRow(item: DueItem, now: Instant, zone: ZoneId) {
    val due = item.dueAt ?: return
    val relative = relativeDue(due, item.dueHasTime, now, zone, Locale.getDefault())
    val labelColor: Color = when (relative.urgency) {
        DueUrgency.OVERDUE, DueUrgency.SOON -> MaterialTheme.colorScheme.error
        DueUrgency.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.courseCode?.let { code ->
                        CourseChip(code)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = absoluteDue(item, zone, now),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = relative.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
            )
        }
    }
}

@Composable
private fun CourseChip(code: String) {
    val index = code.hashCode().mod(3)
    val container = when (index) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = when (index) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = code,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = content,
        )
    }
}

private val bucketOrder = listOf(
    DueBucket.OVERDUE,
    DueBucket.TODAY,
    DueBucket.THIS_WEEK,
    DueBucket.LATER,
)

private fun absoluteDue(item: DueItem, zone: ZoneId, now: Instant): String {
    val due = item.dueAt ?: return "No date"
    val sameYear = due.atZone(zone).year == now.atZone(zone).year
    val pattern = when {
        !sameYear -> "MMM d, yyyy"
        item.dueHasTime -> "EEE h:mm a"
        else -> "EEE, MMM d"
    }
    return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).withZone(zone).format(due)
}
