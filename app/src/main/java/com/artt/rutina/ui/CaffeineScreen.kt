package com.artt.rutina.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artt.rutina.data.CaffeineIntake
import com.artt.rutina.data.CaffeineLogic
import com.artt.rutina.data.CaffeinePlan
import com.artt.rutina.data.CaffeineSettings
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
private val DAY_FORMAT = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

/** Готовые нормы кофеина: чипы, как везде в приложении. */
private val TARGET_PRESETS = listOf(100, 150, 200, 250, 300)

/** Готовые длительности таймера. */
private val TIMER_PRESETS = listOf(30, 60, 90, 120)

/**
 * Экран трекера кофеина: норма, разбивка на приёмы, расписание, таймер и история.
 * Собран из тех же токенов и компонентов, что остальное приложение (Radius/Space,
 * карточки с рамкой, плоские меню, зелёный только на главном действии).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaffeineScreen(
    settings: CaffeineSettings,
    intakes: List<CaffeineIntake>,
    today: LocalDate,
    onBack: () -> Unit,
    onSetTarget: (Int) -> Unit,
    onSetBedtime: (Int) -> Unit,
    onSetWake: (Int) -> Unit,
    onAddIntake: (Int) -> Unit,
    onRemoveIntake: (Long) -> Unit,
    onStartTimer: (Int, Int?) -> Unit,
    onCancelTimer: () -> Unit,
) {
    val todayKey = today.toString()
    val todayIntakes = intakes.filter { it.day == todayKey }.sortedBy { it.at }

    val plan = CaffeineLogic.plan(
        targetMg = settings.targetMg,
        wakeMinutes = settings.wakeMinutes,
        bedtimeMinutes = settings.bedtimeMinutes,
    )
    val total = todayIntakes.sumOf { it.mg }
    val doneCount = todayIntakes.size

    var bedtimePicker by remember { mutableStateOf(false) }
    var wakePicker by remember { mutableStateOf(false) }

    // Тикающий таймер: секунды перерисовываются, только пока отсчёт идёт.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val timerEnd = settings.timerEndMs
    LaunchedEffect(timerEnd) {
        while (timerEnd != null && timerEnd > System.currentTimeMillis()) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val timerActive = timerEnd != null && timerEnd > nowMs

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
                        Text("Кофеин", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "норма ${settings.targetMg} мг · ${plan.doses.size} приёма",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.screen)
                .padding(top = Space.xs, bottom = Space.m),
            // Экраны приложения не прокручиваются: свободное место отдаётся вниз,
            // а не собирается провалом между блоками.
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            TotalCard(
                total = total,
                target = settings.targetMg,
                doneCount = doneCount,
                moments = plan.doses.size,
            )

            DosesCard(
                doses = plan.doses,
                times = plan.schedule.times,
                todayIntakes = todayIntakes,
                onAdd = { onAddIntake(plan.doses.getOrElse(doneCount) { CaffeineLogic.STEP_MG }) },
                onRemove = onRemoveIntake,
            )

            ScheduleCard(
                plan = plan,
                wakeMinutes = settings.wakeMinutes,
                bedtimeMinutes = settings.bedtimeMinutes,
                onPickWake = { wakePicker = true },
                onPickBedtime = { bedtimePicker = true },
            )

            TimerCard(
                timerActive = timerActive,
                remainingMs = if (timerActive) timerEnd!! - nowMs else 0L,
                slot = settings.timerSlot,
                canStart = doneCount < plan.doses.size,
                // slot — индекс только что отмеченного приёма: после первого это 0,
                // поэтому подпись «после N-го» считается как slot + 1.
                onStart = { minutes -> onStartTimer(minutes, doneCount - 1) },
                onCancel = onCancelTimer,
            )

            HistoryCard(intakes = intakes, today = today)
        }
    }

    if (wakePicker) {
        TimeAskDialog(
            title = "Время подъёма",
            initialMinutes = settings.wakeMinutes,
            onDismiss = { wakePicker = false },
            onConfirm = { onSetWake(it); wakePicker = false },
        )
    }
    if (bedtimePicker) {
        TimeAskDialog(
            title = "Время сна",
            initialMinutes = settings.bedtimeMinutes,
            onDismiss = { bedtimePicker = false },
            onConfirm = { onSetBedtime(it); bedtimePicker = false },
        )
    }
}

@Composable
private fun TotalCard(total: Int, target: Int, doneCount: Int, moments: Int) {
    val allDone = doneCount >= moments
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(
            containerColor = if (allDone) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (allDone) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = Space.l, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$total мг",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (allDone) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (allDone) "норма принята" else "из $target мг",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (allDone) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!allDone) {
                Spacer(Modifier.height(Space.s))
                // Сегменты по приёмам: видно, сколько из них уже отмечено.
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(moments) { i ->
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(5.dp)
                                .clip(Radius.segment)
                                .background(
                                    if (i < doneCount) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DosesCard(
    doses: List<Int>,
    times: List<Int>,
    todayIntakes: List<CaffeineIntake>,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = Space.l, vertical = 10.dp)) {
            Text(
                text = "Приёмы сегодня",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.s))

            doses.forEachIndexed { i, mg ->
                // Приём считается отмеченным по порядку: отметки идут друг за другом,
                // поэтому i-я отметка закрывает i-й приём разбивки.
                val intake = todayIntakes.getOrNull(i)
                val done = intake != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        // Снять ошибочную отметку — тапом по самой отметке, там же,
                        // где она ставилась. Отдельного списка отметок не нужно.
                        .then(
                            if (done) {
                                Modifier.clickable { onRemove(intake!!.id) }
                            } else {
                                Modifier
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (done) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(Space.m))
                    // Отмеченная строка показывает фактическую дозу отметки, а не дозу
                    // из текущей разбивки: после смены нормы они расходятся, и «принято
                    // 100 мг» над отметкой в 50 мг читалось бы как ошибка.
                    Text(
                        text = "${intake?.mg ?: mg} мг",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (done) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                    )
                    Spacer(Modifier.weight(1f))
                    if (done) {
                        Text(
                            text = "принято",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        times.getOrNull(i)?.let { t ->
                            Text(
                                text = CaffeineLogic.timeOf(t).format(TIME_FORMAT),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.s))
            Button(
                onClick = onAdd,
                enabled = todayIntakes.size < doses.size,
                modifier = Modifier.fillMaxWidth(),
                shape = Radius.field,
            ) {
                Text(
                    if (todayIntakes.size < doses.size) {
                        "Отметить ${doses[todayIntakes.size]} мг"
                    } else {
                        "Все приёмы отмечены"
                    },
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    plan: CaffeinePlan,
    wakeMinutes: Int,
    bedtimeMinutes: Int,
    onPickWake: () -> Unit,
    onPickBedtime: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = Space.l, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.caption),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Space.s))
                Text("Подъём", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                // тап по значению открывает выбор времени — как в шите дела
                Text(
                    text = CaffeineLogic.timeOf(wakeMinutes).format(TIME_FORMAT),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onPickWake),
                )
            }
            Spacer(Modifier.height(Space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.NightlightRound,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.caption),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Space.s))
                Text("Сон", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    text = CaffeineLogic.timeOf(bedtimeMinutes).format(TIME_FORMAT),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onPickBedtime),
                )
            }
            Spacer(Modifier.height(Space.s))
            val lastAllowed = plan.schedule.lastAllowedMinutes
            Text(
                text = if (plan.schedule.times.isEmpty()) {
                    "При подъёме ${CaffeineLogic.timeOf(wakeMinutes).format(TIME_FORMAT)} " +
                        "и сне ${CaffeineLogic.timeOf(bedtimeMinutes).format(TIME_FORMAT)} " +
                        "окна для приёмов нет"
                } else {
                    "Последний приём не позже " +
                        CaffeineLogic.timeOf(lastAllowed).format(TIME_FORMAT) +
                        " — за ${CaffeineLogic.FREE_HOURS_BEFORE_SLEEP} часов до сна"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TimerCard(
    timerActive: Boolean,
    remainingMs: Long,
    slot: Int?,
    canStart: Boolean,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    var minutes by remember { mutableStateOf(CaffeineLogic.DEFAULT_TIMER_MINUTES) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = Space.l, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Таймер до следующего приёма", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                if (timerActive) {
                    Text(
                        text = formatRemaining(remainingMs),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(Space.s))
            if (timerActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            slot == null -> "отсчёт идёт"
                            slot < 0 -> "отсчёт идёт"
                            else -> "после ${slot + 1}-го приёма"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("Отменить")
                    }
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    TIMER_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = minutes == preset,
                            onClick = { minutes = preset },
                            label = { Text(formatDuration(preset)) },
                            shape = Radius.field,
                            colors = selectionChipColors(),
                        )
                    }
                }
                Spacer(Modifier.height(Space.s))
                Button(
                    onClick = { onStart(minutes) },
                    enabled = canStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Radius.field,
                ) {
                    Text(if (canStart) "Запустить" else "Сначала отметьте приёмы")
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(intakes: List<CaffeineIntake>, today: LocalDate) {
    // Суммы по дням, свежие сверху.
    val byDay = intakes
        .groupBy { it.day }
        .mapNotNull { (day, list) ->
            val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return@mapNotNull null
            date to list
        }
        .sortedByDescending { it.first }
        .take(5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = Space.l, vertical = 10.dp)) {
            Text(
                text = "Последние дни",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Space.xs))
            if (byDay.isEmpty()) {
                Text(
                    text = "Пока пусто — отметьте первый приём",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                byDay.forEach { (date, list) ->
                    val isToday = date == today
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isToday) "Сегодня" else date.format(DAY_FORMAT),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${list.sumOf { it.mg }} мг",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TimeAskDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Radius.card,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Ок") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text("Отмена") }
        },
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        },
    )
}

private fun formatDuration(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0 -> "$m мин"
        m == 0 -> "$h ч"
        else -> "$h ч $m м"
    }
}

private fun formatRemaining(ms: Long): String {
    val total = ((ms + 999) / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%d:%02d:%02d".format(Locale.ROOT, h, m, s)
}
