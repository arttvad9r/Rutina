package com.artt.rutina.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artt.rutina.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HISTORY_DATE = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(habitId: Long, onBack: () -> Unit, today: LocalDate = LocalDate.now()) {
    val vm = mainViewModel()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val habit = snapshot?.habits?.firstOrNull { it.id == habitId }
    val daySet = snapshot?.records?.get(habitId).orEmpty()
    var monthKey by rememberSaveable(habitId) { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedKey by rememberSaveable(habitId) { mutableStateOf<String?>(null) }
    var edit by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.parse(monthKey)
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text(habit?.name ?: "История", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            actions = { if (habit != null) IconButton(onClick = { edit = true }) { Icon(Icons.Default.Edit, "Изменить привычку") } })
    }) { padding ->
        if (habit != null) Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen).padding(bottom = 24.dp)) {
            Text(buildString {
                append(if (habit.hour < 0) "Без напоминания" else "Напоминание в %02d:%02d".format(Locale.ROOT, habit.hour, habit.minute))
                when {
                    finishedOn(habit, today) -> append(" · Завершена")
                    !habit.active -> append(" · На паузе")
                    courseEnd(habit) != null -> append(" · до " + courseEnd(habit)!!.format(HISTORY_DATE))
                }
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Серия ${daysWord(streakOf(daySet, today))} · Рекорд ${daysWord(bestStreak(daySet, today))}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp, bottom = 18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthKey = month.minusMonths(1).toString(); selectedKey = null },
                    enabled = month > YearMonth.from(startDay(habit.createdAt))) { Icon(Icons.Default.ChevronLeft, "Предыдущий месяц") }
                Text(month.format(MONTH_FORMAT).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconButton(onClick = { monthKey = month.plusMonths(1).toString(); selectedKey = null },
                    enabled = month < YearMonth.from(today)) { Icon(Icons.Default.ChevronRight, "Следующий месяц") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { label ->
                    Text(label, Modifier.weight(1f).padding(vertical = 8.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            monthGrid(month.atDay(1)).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        if (day == null) Spacer(Modifier.weight(1f).height(44.dp)) else {
                            val done = day.toString() in daySet
                            val enabled = canMarkDay(habit, day, today)
                            Surface(onClick = { selectedKey = day.toString() }, enabled = enabled,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics {
                                    contentDescription = "${day.format(HISTORY_DATE)}: ${if (done) "выполнено" else if (enabled) "не отмечено" else "вне периода"}"
                                }, shape = Radius.field,
                                color = if (done) MaterialTheme.colorScheme.primary else if (enabled) MaterialTheme.colorScheme.surface else Color.Transparent,
                                contentColor = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                border = when (day.toString()) {
                                    selectedKey -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    today.toString() -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                                    else -> null
                                }) {
                                Text(day.dayOfMonth.toString(), Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                                    color = if (enabled || done) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f))
                            }
                        }
                    }
                }
            }
            Text("Зелёные дни — выполнено", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            selectedKey?.let { key ->
                val selected = LocalDate.parse(key)
                if (canMarkDay(habit, selected, today)) Card(Modifier.fillMaxWidth().padding(top = 20.dp),
                    shape = Radius.card, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(selected.format(HISTORY_DATE), style = MaterialTheme.typography.bodyMedium)
                            Text(if (key in daySet) "Выполнено" else "Не отмечено", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { vm.toggleOnDay(habit, selected) },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = Radius.field) {
                            Text(if (key in daySet) "Снять отметку" else "Отметить выполнение")
                        }
                    }
                }
            }
        }
    }
    if (edit && habit != null) HabitSheet(habit, { edit = false }, { name, hour, minute, duration ->
        vm.saveHabit(habit, name, hour, minute, duration); edit = false
    }, { vm.delete(habit.id); edit = false; onBack() })
}

internal fun streakOf(daySet: Set<String>, today: LocalDate): Int {
    var day = if (today.toString() in daySet) today else today.minusDays(1)
    var count = 0
    while (day.toString() in daySet) { count++; day = day.minusDays(1) }
    return count
}

internal fun bestStreak(daySet: Set<String>, today: LocalDate): Int {
    val sorted = daySet.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.filter { !it.isAfter(today) }.sorted()
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    sorted.forEach { day ->
        run = if (previous?.plusDays(1) == day) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    return best
}
