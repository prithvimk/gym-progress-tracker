@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.ui.format.signedVolumeText
import io.github.prithvimk.gymtracker.ui.format.volumeText
import io.github.prithvimk.gymtracker.ui.format.weightText
import io.github.prithvimk.gymtracker.ui.history.formatDate
import io.github.prithvimk.gymtracker.ui.history.formatStartTime
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.theme.recordColors
import io.github.prithvimk.gymtracker.ui.theme.tabular
import io.github.prithvimk.gymtracker.ui.workout.RecordBadge
import io.github.prithvimk.gymtracker.ui.workout.SetLabel
import io.github.prithvimk.gymtracker.ui.workout.formatSetNumber
import io.github.prithvimk.gymtracker.ui.workout.text
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ExerciseDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseDetailViewModel = viewModel(factory = ExerciseDetailViewModel.factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state == ExerciseDetailState.Missing) {
        if (state == ExerciseDetailState.Missing) onBack()
    }

    ExerciseDetailScreen(state = state, onBack = onBack, onEdit = viewModel::onEditClick, modifier = modifier)

    ExerciseEditorHost(viewModel.editor)
}

@Composable
fun ExerciseDetailScreen(
    state: ExerciseDetailState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loaded = state as? ExerciseDetailState.Loaded
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(loaded?.exercise?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.exercise_detail_back),
                    )
                }
            },
            actions = {
                if (loaded != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.exercise_detail_edit))
                    }
                }
            },
        )
        if (loaded == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            ExerciseDetailContent(loaded, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExerciseDetailContent(state: ExerciseDetailState.Loaded, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ExerciseSubtitle(state.exercise) }
        if (state.e1rm.isNotEmpty()) {
            item { E1rmHero(state.e1rm, state.e1rmChange, Modifier.padding(top = 8.dp)) }
        }
        item { SectionTitle(R.string.exercise_detail_records) }
        item { RecordTiles(state.records) }
        item { SectionTitle(R.string.exercise_detail_history) }
        if (state.sessions.isEmpty()) {
            item { MutedText(stringResource(R.string.exercise_detail_empty)) }
        }
        items(state.sessions, key = { it.workoutId }) { session -> SessionCard(session) }
    }
}

@Composable
private fun ExerciseSubtitle(exercise: Exercise) {
    val subtitle =
        stringResource(
            R.string.exercise_subtitle,
            stringResource(exercise.muscleGroup.labelRes),
            stringResource(exercise.equipment.labelRes),
        )
    val tagRes =
        when {
            exercise.archived -> R.string.exercises_archived_tag
            exercise.isCustom -> R.string.exercises_custom_tag
            else -> null
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MutedText(subtitle, Modifier.weight(1f))
        if (tagRes != null) {
            Text(
                text = stringResource(tagRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun SectionTitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * The latest session's estimated 1RM in large type, how it moved over the last 30 days, and a
 * sparkline of every session's.
 */
@Composable
private fun E1rmHero(points: List<E1rmPoint>, change: Double?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.exercise_detail_e1rm),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onPrimaryContainer,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = volumeText(points.last().kg),
                style = MaterialTheme.typography.displaySmall.tabular(),
                color = colors.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            if (points.size >= 2) {
                Sparkline(
                    values = points.map { it.kg },
                    color = colors.primary,
                    modifier = Modifier.width(SPARKLINE_WIDTH).height(SPARKLINE_HEIGHT).padding(bottom = 6.dp),
                )
            }
        }
        if (change != null) E1rmChangeRow(change)
    }
}

/** "↑ +4.2 kg in 30 days", with an arrow only when it moved. */
@Composable
private fun E1rmChangeRow(change: Double) {
    val color = MaterialTheme.colorScheme.onPrimaryContainer
    val text =
        if (change == 0.0) {
            pluralStringResource(R.plurals.exercise_detail_e1rm_no_change, E1RM_CHANGE_DAYS, E1RM_CHANGE_DAYS)
        } else {
            pluralStringResource(
                R.plurals.exercise_detail_e1rm_change,
                E1RM_CHANGE_DAYS,
                signedVolumeText(change),
                E1RM_CHANGE_DAYS,
            )
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (change != 0.0) {
            Icon(
                painter = painterResource(if (change > 0) R.drawable.ic_trending_up else R.drawable.ic_trending_down),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp).padding(end = 4.dp),
            )
        }
        Text(text, style = MaterialTheme.typography.bodyMedium.tabular(), color = color)
    }
}

/** [values] as a line from the oldest on the left to the latest on the right, which gets a dot. */
@Composable
private fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val low = values.min()
        val span = (values.max() - low).takeIf { it > 0 } ?: 1.0
        val dot = 3.dp.toPx()
        fun point(index: Int): Offset = Offset(
            x = dot + (size.width - 2 * dot) * index / (values.size - 1),
            y = dot + (size.height - 2 * dot) * (1 - ((values[index] - low) / span).toFloat()),
        )
        val path = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (index in 1 until values.size) lineTo(point(index).x, point(index).y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = dot, center = point(values.size - 1))
    }
}

private val SPARKLINE_WIDTH = 120.dp
private val SPARKLINE_HEIGHT = 48.dp

/** The standing records as a grid of tiles, two to a row, each with the day it was set. */
@Composable
private fun RecordTiles(records: List<RecordBest>) {
    if (records.isEmpty()) {
        MutedText(stringResource(R.string.exercise_detail_no_records))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        records.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                pair.forEach { RecordTile(it, Modifier.weight(1f).fillMaxHeight()) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A record's value with a gold trophy, and what it is and when it was set under it. */
@Composable
private fun RecordTile(record: RecordBest, modifier: Modifier = Modifier) {
    val date = formatDate(localDate(record.achievedAt), "dMMM")
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_trophy),
                contentDescription = null,
                tint = MaterialTheme.recordColors.record,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = recordValueText(record),
                style = MaterialTheme.typography.titleMedium.tabular(),
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Text(
            text = stringResource(R.string.record_tile_label, stringResource(record.type.tileLabelRes), date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A record's number for its tile: "82.5 kg", "8 @ 82.5 kg", "104.5 kg". */
@Composable
private fun recordValueText(record: RecordBest): String = when (record.type) {
    PrType.WEIGHT -> weightText(record.value)
    PrType.REPS_AT_WEIGHT -> stringResource(
        R.string.record_tile_reps_at_weight,
        record.reps,
        weightText(record.weightKg)
    )
    PrType.E1RM, PrType.VOLUME -> volumeText(record.value)
}

private val PrType.tileLabelRes: Int
    get() = when (this) {
        PrType.WEIGHT -> R.string.record_tile_weight
        PrType.REPS_AT_WEIGHT -> R.string.record_tile_reps
        PrType.E1RM -> R.string.record_tile_e1rm
        PrType.VOLUME -> R.string.record_tile_volume
    }

@Composable
private fun SessionCard(session: HistorySession) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = session.routineName ?: stringResource(R.string.history_workout_untitled),
                style = MaterialTheme.typography.titleSmall,
            )
            MutedText(
                if (session.inProgress) {
                    stringResource(R.string.exercise_detail_in_progress)
                } else {
                    formatStartTime(session.startedAt)
                },
            )
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                session.sets.forEach { HistorySetRow(it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MutedText(stringResource(R.string.exercise_detail_volume, volumeText(session.volumeKg)))
                if (session.volumeRecord) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = stringResource(R.string.exercise_detail_volume_record),
                        tint = MaterialTheme.recordColors.record,
                        modifier = Modifier.padding(start = 6.dp).size(16.dp),
                    )
                }
            }
        }
    }
}

/** "1  80 kg × 8  PR  @ 8": the PR badge follows the numbers when the set holds a record. */
@Composable
internal fun HistorySetRow(historySet: HistorySet, modifier: Modifier = Modifier) {
    val set = historySet.set
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = historySet.label.text(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(SET_LABEL_WIDTH),
        )
        Text(
            text =
            if (set.weightKg > 0) {
                stringResource(R.string.exercise_detail_set, weightText(set.weightKg), set.reps)
            } else {
                pluralStringResource(R.plurals.exercise_detail_set_reps, set.reps, set.reps)
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (historySet.records.isNotEmpty()) {
            RecordBadge(historySet.records, Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.weight(1f))
        set.rpe?.let { MutedText(stringResource(R.string.exercise_detail_rpe, formatSetNumber(it))) }
    }
}

private fun localDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

private val SET_LABEL_WIDTH = 40.dp

@Preview(showBackground = true)
@Composable
private fun ExerciseDetailPreview() {
    val now = System.currentTimeMillis()
    val day = 86_400_000L
    val exercise =
        Exercise("bench", "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, false, false, 0L, 0L)
    val sessions =
        listOf(
            HistorySession(
                workoutId = "b",
                startedAt = now - day,
                inProgress = false,
                routineName = "Push",
                sets =
                listOf(
                    HistorySet(previewSet("b1", 40.0, 10, SetType.WARMUP), SetLabel.Kind(SetType.WARMUP), emptyList()),
                    HistorySet(previewSet("b2", 80.0, 8), SetLabel.Number(1), listOf(PrType.WEIGHT, PrType.E1RM)),
                    HistorySet(previewSet("b3", 80.0, 7, rpe = 9.0), SetLabel.Number(2), emptyList()),
                ),
                volumeKg = 1_600.0,
                volumeRecord = true,
            ),
        )
    val records =
        listOf(
            RecordBest(PrType.WEIGHT, 80.0, 80.0, 8, now - day),
            RecordBest(PrType.E1RM, 101.3, 80.0, 8, now - day),
            RecordBest(PrType.VOLUME, 1_600.0, 80.0, 7, now - day),
        )
    GymTrackerTheme(dynamicColor = false) {
        ExerciseDetailScreen(ExerciseDetailState.Loaded(exercise, records, sessions), onBack = {}, onEdit = {})
    }
}

private fun previewSet(
    id: String,
    weightKg: Double,
    reps: Int,
    type: SetType = SetType.WORKING,
    rpe: Double? = null,
): WorkoutSet = WorkoutSet(id, "we", 0, weightKg, reps, rpe, type, completedAt = 1L, createdAt = 0L, updatedAt = 0L)
