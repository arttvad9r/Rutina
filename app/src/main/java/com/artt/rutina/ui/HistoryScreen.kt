package com.artt.rutina.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import com.artt.rutina.data.NO_LIMIT
import com.artt.rutina.data.courseStatusText
import com.artt.rutina.data.pluralDays
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
    var durationDays by remember { mutableStateOf(NO_LIMIT) }
    var days by remember { mutableStateOf<List<LocalDate>>(emptyList()) }

    LaunchedEffect(habitId) {
        app.repo.habit(habitId)?.let {
            habitName = it.name
            hour = it.hour
            minute = it.minute
            durationDays = it.durationDays
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
                            // Срок курса: без него «0/30» у закончившегося курса читается
                            // как «забросил», хотя дело просто дошло до конца.
                            if (durationDays > NO_LIMIT) {
                                Spacer(Modifier.width(Space.s))
                                Text(
                                    text = courseStatusText(
                                        start = createdAt,
                                        durationDays = durationDays,
                                        day = today,
                                    ),
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
            // Серия и рекорд — одной строкой, без карточки и без «N/30 за 30 дней»:
            // плотность отметок и так видна на календаре ниже, третий способ
            // показывать одни и те же дни только утяжеляет экран.
            Text(
                text = "Серия ${daysWord(streak)} · Рекорд ${daysWord(best)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HistoryGrid(
                daySet = daySet,
                today = today,
                createdAt = createdAt,
                modifier = Modifier.fillMaxWidth(),
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


/** Тонкий разделитель: без него три значения сливались в одну строку текста. */

private val DOW_LABELS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/**
 * Календарь текущего месяца в классической ориентации: строка = неделя,
 * колонка = день недели (Пн слева, Вс справа), сверху — строка подписей дней недели,
 * над ней — название месяца. Число стоит внутри клетки, как в бумажном календаре.
 * Клетки — квадраты, размер считается от доступного места (BoxWithConstraints),
 * поэтому сетка целиком помещается на экран при любом размере и не требует прокрутки.
 */
@Composable
private fun HistoryGrid(
    daySet: Set<String>,
    today: LocalDate,
    createdAt: LocalDate,
    modifier: Modifier = Modifier,
) {
    val weeks = monthGrid(today)
    val monthTitle = today.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
        .replaceFirstChar { it.uppercase(Locale("ru")) }
    val gap = Radius.gridGap

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val byWidth = (maxWidth - gap * 6) / 7
        val byHeight = ((maxHeight - gap * 8) / 8).coerceAtLeast(4.dp)
        val cell = minOf(byWidth, byHeight, 64.dp).coerceAtLeast(4.dp)
        val gridWidth = cell * 7 + gap * 6

        Column(
            // Не fillMaxSize: сетка занимает ровно свою высоту и не собирает
            // вокруг себя пустой провал — свободное место раздаёт родитель.
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.width(gridWidth)) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Space.xs))

                // Шапка дней недели
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    DOW_LABELS.forEach { dow ->
                        Box(
                            modifier = Modifier.width(cell),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = dow,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Space.xs))

                // Строки-недели: внутри — дни Пн→Вс
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    weeks.forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            week.forEach { d ->
                                val done = d != null && daySet.contains(d.toString())
                                val isToday = d == today
                                // дни до создания дела — пустые, а не «пропущенные»
                                val beforeBirth = d != null &&
                                    createdAt != LocalDate.EPOCH && d.isBefore(createdAt)
                                // Число месяца в клетке: календарь читается как настоящий
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
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (d != null && cell >= 20.dp) {
                                        Text(
                                            text = "${d.dayOfMonth}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (done) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
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
