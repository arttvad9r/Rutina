package com.artt.rutina.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FULL_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru"))

/**
 * Сколько последних отметок показываем списком.
 * Список ограничен, чтобы экран гарантированно укладывался без прокрутки:
 * календарь и так показывает всю историю за 60 дней.
 */
private const val MAX_RECENT = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(habitId: Long, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.artt.rutina.RutinaApp

    var habitName by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(-1) }
    var minute by remember { mutableStateOf(0) }
    var createdAt by remember { mutableStateOf(LocalDate.EPOCH) }
    var days by remember { mutableStateOf<List<LocalDate>>(emptyList()) }

    LaunchedEffect(habitId) {
        app.repo.habit(habitId)?.let {
            habitName = it.name
            hour = it.hour
            minute = it.minute
            // день создания: дни до него не показываем как «пропущенные»
            createdAt = runCatching {
                java.time.Instant.ofEpochMilli(it.createdAt)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }.getOrDefault(LocalDate.EPOCH)
        }
        app.repo.observeHabit(habitId).collectLatest { list ->
            days = list.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
        }
    }

    val daySet = days.map { it.toString() }.toSet()
    val today = LocalDate.now()
    val last30 = (0 until 30).count { daySet.contains(today.minusDays(it.toLong()).toString()) }
    val streak = streakOf(daySet, today)
    val best = bestStreak(daySet, today)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (habitName.isBlank()) "История" else habitName,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hour >= 0) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSize.caption),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(Space.xs))
                                Text(
                                    text = String.format("%02d:%02d", hour, minute),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = "без напоминания",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        // Экран не прокручивается: календарь занимает всё оставшееся место и сам
        // подбирает размер клетки, поэтому список отметок никогда не выталкивает контент.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.screen)
                .padding(top = Space.xs, bottom = Space.m),
            // Зазоры фиксированные, а свободная высота остаётся внизу: так на узком
            // экране не возникает провал между статистикой и календарём.
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            StatsCard(streak = streak, best = best, last30 = last30)

            Text(
                text = "Последние 60 дней",
                style = MaterialTheme.typography.titleMedium,
            )

            HistoryGrid(
                daySet = daySet,
                today = today,
                createdAt = createdAt,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "закрашено — отмечено · серый — пропущено · рамка — сегодня",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val recent = days.sortedDescending().take(MAX_RECENT)
            if (recent.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Последние отметки", style = MaterialTheme.typography.titleMedium)
                    recent.forEach { d ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(Space.s))
                            Text(
                                text = d.format(FULL_FORMAT),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = if (d == today) "сегодня" else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Серия, рекорд и отметки за 30 дней — три равные колонки с разделителями. */
@Composable
private fun StatsCard(streak: Int, best: Int, last30: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatColumn(
                value = if (streak == 0) "—" else daysWord(streak),
                label = "серия",
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            StatColumn(
                value = if (best == 0) "—" else daysWord(best),
                label = "рекорд",
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            StatColumn(
                value = "$last30/30",
                label = "за 30 дней",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Тонкий разделитель: без него три значения сливались в одну строку текста. */
@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(26.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * Календарь за 60 дней: колонка = неделя, строка = день недели (Пн сверху).
 * Клетки — квадраты, размер считается от доступного места (BoxWithConstraints),
 * поэтому сетка целиком помещается на экран при любом размере и не требует прокрутки.
 * Ориентация выбрана именно такая: тогда подписи месяцев над колонками совпадают
 * с неделями, к которым относятся.
 */
@Composable
private fun HistoryGrid(
    daySet: Set<String>,
    today: LocalDate,
    createdAt: LocalDate,
    modifier: Modifier = Modifier,
) {
    val weeks = historyGrid(today, days = 60)
    val monthFormat = DateTimeFormatter.ofPattern("LLL", Locale("ru"))
    val gap = Radius.gridGap
    val labels = monthLabels(weeks)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val weekCount = weeks.size // колонки
        // Клетка — квадрат, вписанный в доступное место: ограничена и шириной, и высотой.
        // Верхняя граница нужна только чтобы на планшете ячейки не превращались в плитку.
        val byWidth = (maxWidth - gap * (weekCount - 1)) / weekCount
        // Из высоты вычитаем строку подписей месяцев и все межстрочные зазоры,
        // иначе на низком экране сетка вылезала за пределы блока.
        val labelRow = 16.dp
        val byHeight = ((maxHeight - labelRow - gap * 7) / 7).coerceAtLeast(4.dp)
        val cell = minOf(byWidth, byHeight, 72.dp).coerceAtLeast(4.dp)

        val gridWidth = cell * weekCount + gap * (weekCount - 1)

        Column(
            // Не fillMaxSize: сетка занимает ровно свою высоту и не собирает
            // вокруг себя пустой провал — свободное место раздаёт родитель.
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.width(gridWidth)) {
                // Подписи месяцев: одна на колонку, ровно над своей неделей
                Row {
                    weeks.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier.width(cell),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            labels[i]?.let { day ->
                                Text(
                                    text = day.format(monthFormat),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                        if (i != weeks.lastIndex) Spacer(Modifier.width(gap))
                    }
                }
                Spacer(Modifier.height(gap))

                // Строки — дни недели (Пн = 0)
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(7) { dayOfWeek ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            weeks.forEach { week ->
                                val d = week.getOrNull(dayOfWeek)
                                val done = d != null && daySet.contains(d.toString())
                                val isToday = d == today
                                // дни до создания дела — пустые, а не «пропущенные»
                                val beforeBirth = d != null &&
                                    createdAt != LocalDate.EPOCH && d.isBefore(createdAt)
                                Box(
                                    modifier = Modifier
                                        .size(cell)
                                        .clip(Radius.cell)
                                        .background(
                                            when {
                                                d == null -> Color.Transparent
                                                done -> MaterialTheme.colorScheme.primary
                                                beforeBirth -> MaterialTheme.colorScheme
                                                    .surfaceVariant.copy(alpha = 0.4f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        )
                                        .then(
                                            // «сегодня» — рамка контрастного цвета поверх заливки
                                            if (isToday) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    shape = Radius.cell,
                                                )
                                            } else {
                                                Modifier
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Самая длинная серия за всю историю. */
internal fun bestStreak(daySet: Set<String>, today: LocalDate): Int {
    if (daySet.isEmpty()) return 0
    val sorted = daySet.sorted()
    var best = 1
    var run = 1
    for (i in 1 until sorted.size) {
        val prev = LocalDate.parse(sorted[i - 1])
        val cur = LocalDate.parse(sorted[i])
        run = if (cur == prev.plusDays(1)) run + 1 else 1
        if (run > best) best = run
    }
    return best
}
