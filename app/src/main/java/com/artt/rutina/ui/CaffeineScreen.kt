package com.artt.rutina.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artt.rutina.data.CaffeineIntake
import com.artt.rutina.data.CaffeineLogic
import com.artt.rutina.data.CaffeineSettings
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale("ru"))
private val DAY_FORMAT = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))

/**
 * Экран трекера кофеина: норма, разбивка на приёмы, расписание, таймер и история.
 * Собран из тех же токенов и компонентов, что остальное приложение (Radius/Space,
 * карточки с рамкой, плоские меню, зелёный только на главном действии).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaffeineScreen(
    settings: CaffeineSettings,
    intakes: List<CaffeineIntake>,
    today: LocalDate,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
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

    // Тикающий таймер: секунды перерисовываются, только пока отсчёт идёт.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val timerEnd = settings.timerEndMs
    LaunchedEffect(timerEnd) {
        // Пока отсчёт идёт — секунды; в остальное время раз в полминуты, чтобы подсказка
        // о рекомендуемом приёме не устаревала, если экран остался открытым.
        while (true) {
            delay(if (timerEnd != null && timerEnd > System.currentTimeMillis()) 1_000 else 30_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val timerActive = timerEnd != null && timerEnd > nowMs

    // Время последней отметки — точка отсчёта для следующего приёма: отметка позже
    // расписания сдвигает день, и подсказка должна это учитывать.
    val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
    val lastIntakeMinutes = todayIntakes.lastOrNull()?.let { intake ->
        Instant.ofEpochMilli(intake.at).atZone(ZoneId.systemDefault()).toLocalTime()
            .let { it.hour * 60 + it.minute }
    }
    val recommendedMinutes = CaffeineLogic.nextIntakeMinutes(nowMinutes, plan.schedule, lastIntakeMinutes)
    val suggestedMinutes = CaffeineLogic.timerMinutes(nowMinutes, recommendedMinutes)

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
                actions = {
                    // Норма, подъём и сон — на отдельном экране: на этом нужны приёмы и таймер.
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки трекера")
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

            // Таймер нужен, пока есть что отсчитывать. Когда все приёмы отмечены и
            // отсчёта нет, карточка исчезает целиком: строка состояния уже есть в
            // «Приёмах сегодня», и второй раз то же самое только шумит.
            if (doneCount < plan.doses.size || timerActive) {
                TimerCard(
                    timerActive = timerActive,
                    remainingMs = if (timerActive) timerEnd!! - nowMs else 0L,
                    slot = settings.timerSlot,
                    suggestedMinutes = suggestedMinutes,
                    recommendedMinutes = recommendedMinutes,
                    // slot — индекс только что отмеченного приёма: после первого это 0,
                    // поэтому подпись «после N-го» считается как slot + 1.
                    onStart = { minutes -> onStartTimer(minutes, doneCount - 1) },
                    onCancel = onCancelTimer,
                )
            }

            HistoryCard(intakes = intakes, today = today)
        }
    }
}

@Composable
private fun TotalCard(total: Int, target: Int, doneCount: Int, moments: Int) {
    val allDone = doneCount >= moments
    // Заголовок состояния вместо «карточки в карточке»: норма и статус в одной
    // строке, сегменты под ней. Раньше сверху жила ещё зелёная плашка на всю ширину,
    // и экран читался как четыре вложенных контейнера.
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$total / $target мг",
                style = MaterialTheme.typography.titleLarge,
                color = if (allDone) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(Modifier.width(Space.s))
            Text(
                text = if (allDone) "дневная норма выполнена" else "дневная норма",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
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
                        modifier = Modifier.softStrikeThrough(
                            if (done) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            } else {
                                Color.Transparent
                            },
                        ),
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

            // После завершения огромная disabled-кнопка только шумит: она выглядит как
            // действие, которое почему-то недоступно. Вместо неё — состояние.
            if (todayIntakes.size < doses.size) {
                Spacer(Modifier.height(Space.s))
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Radius.field,
                ) {
                    Text("Отметить ${doses[todayIntakes.size]} мг")
                }
            } else {
                Spacer(Modifier.height(Space.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSize.caption),
                    )
                    Spacer(Modifier.width(Space.s))
                    Text(
                        text = "Норма на сегодня выполнена",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    timerActive: Boolean,
    remainingMs: Long,
    slot: Int?,
    suggestedMinutes: Int,
    recommendedMinutes: Int?,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = Space.l, vertical = 10.dp)) {
            if (timerActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            slot == null || slot < 0 -> "Таймер до следующего приёма"
                            else -> "Таймер после ${slot + 1}-го приёма"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatRemaining(remainingMs),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
                // Компактный таймер: только «− время +» и одна строка подписи,
                // без отдельного заголовка и лишних отступов.
                // Ключ сбрасывает счётчик на предложенное время: после отметки приёма
                // рекомендуемый интервал меняется, и старый остаток смысла не имеет.
                var minutes by remember(suggestedMinutes) {
                    mutableStateOf(suggestedMinutes.coerceAtLeast(CaffeineLogic.TIMER_STEP_MINUTES))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    StepperButton(Icons.Filled.Remove, "Меньше", enabled = true) {
                        minutes = (minutes - CaffeineLogic.TIMER_STEP_MINUTES)
                            .coerceAtLeast(CaffeineLogic.TIMER_STEP_MINUTES)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = formatDuration(minutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (recommendedMinutes != null) {
                                "следующий приём в " +
                                    CaffeineLogic.timeOf(recommendedMinutes).format(TIME_FORMAT)
                            } else {
                                // Расписание молчит: окно дня закрыто или отметки ушли за него.
                                "интервал вручную"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StepperButton(Icons.Filled.Add, "Больше", enabled = true) {
                        minutes = (minutes + CaffeineLogic.TIMER_STEP_MINUTES)
                            .coerceAtMost(CaffeineLogic.MAX_TIMER_MINUTES)
                    }
                }
                Spacer(Modifier.height(Space.s))
                Button(
                    onClick = { onStart(minutes) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Radius.field,
                ) {
                    Text("Запустить")
                }
            }
        }
    }
}

/** Круглая кнопка «−» или «+» у таймера. */
@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = Radius.field,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(IconSize.action))
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
