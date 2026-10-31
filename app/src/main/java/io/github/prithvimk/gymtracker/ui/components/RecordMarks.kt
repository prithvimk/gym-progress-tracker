package io.github.prithvimk.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.theme.recordColors
import io.github.prithvimk.gymtracker.ui.theme.tabular

/** A gold pill with a trophy and [text], e.g. "3 PRs", marking something that holds records. */
@Composable
fun RecordChip(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.recordColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(colors.recordContainer, CircleShape)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_trophy),
            contentDescription = null,
            tint = colors.onRecordContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.tabular(),
            color = colors.onRecordContainer,
        )
    }
}

/** [text], usually a set number, on a gold disc: the mark of a set that holds a record. */
@Composable
fun RecordDisc(text: String, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val colors = MaterialTheme.recordColors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).background(colors.record, CircleShape),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge.tabular(), color = colors.onRecord)
    }
}

/** A number with its label under it, e.g. "58 min" over "Duration", on a tinted tile. */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordMarksPreview() {
    GymTrackerTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            RecordChip("3 PRs")
            RecordDisc("1")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("58 min", "Duration", Modifier.weight(1f))
                StatTile("8,420 kg", "Volume", Modifier.weight(1f))
            }
        }
    }
}
