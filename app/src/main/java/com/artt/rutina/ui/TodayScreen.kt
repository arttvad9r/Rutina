package com.artt.rutina.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artt.rutina.RutinaApp
import com.artt.rutina.data.CaffeineLogic
import com.artt.rutina.data.Habit
import com.artt.rutina.data.courseLabel
import com.artt.rutina.data.finishedOn
import com.artt.rutina.data.habitOnDay
import com.artt.rutina.notif.Reminders
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
private val WEEKDAY_DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))

private sealed interface Screen {
    data object Today : Screen
    data class History(val habitId: Long) : Screen
    data object Caffeine : Screen
    data object Settings : Screen
}

@Composable
fun RutinaRoot(notificationsAllowed: () -> Boolean) {
    var screen by remember { mutableStateOf<Screen>(Screen.Today) }
    val context = LocalContext.current
    val app = context.applicationContext as RutinaApp
    val vm = mainViewModel()

    // при запуске восстанавливаем будильники — система может снести их после обновления или перезагрузки
    LaunchedEffect(Unit) {
        app.repo.habits.first().let { Reminders.rescheduleAll(context, it) }
    }

    when (val s = screen) {
        is Screen.Today -> TodayScreen(
            notificationsAllowed = notificationsAllowed,
            onOpenHistory = { screen = Screen.History(it) },
            onOpenCaffeine = { screen = Screen.Caffeine },
            onOpenSettings = { screen = Screen.Settings },
        )

        is Screen.History -> HistoryScreen(
            habitId = s.habitId,
            onBack = { screen = Screen.Today },
        )

        is Screen.Settings -> {
            val caffeineSettings by vm.caffeineSettings.collectAsStateWithLifecycle()
            SettingsScreen(
                settings = caffeineSettings,
                onBack = { screen = Screen.Today },
                onSetCaffeineEnabled = { vm.setCaffeineEnabled(it) },
                onSetTarget = { vm.setCaffeineTarget(it) },
            )
        }

        is Screen.Caffeine -> {
            val caffeineSettings by vm.caffeineSettings.collectAsStateWithLifecycle()
            val intakes by vm.caffeineIntakes.collectAsStateWithLifecycle()
            CaffeineScreen(
                settings = caffeineSettings,
                intakes = intakes,
                today = LocalDate.now(),
                onBack = { screen = Screen.Today },
                onSetTarget = { vm.setCaffeineTarget(it) },
                onSetBedtime = { vm.setCaffeineBedtime(it) },
                onSetWake = { vm.setCaffeineWake(it) },
                onAddIntake = { vm.addCaffeine(it) },
                onRemoveIntake = { vm.removeCaffeine(it) },
                onStartTimer = { minutes, slot -> vm.startCaffeineTimer(minutes, slot) },
                onCancelTimer = { vm.cancelCaffeineTimer() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    notificationsAllowed: () -> Boolean,
    onOpenHistory: (Long) -> Unit,
    onOpenCaffeine: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm = mainViewModel()
    val snapshot by vm.snapshot.collectAsState()
    val caffeineSettings by vm.caffeineSettings.collectAsStateWithLifecycle()
    val caffeineIntakes by vm.caffeineIntakes.collectAsStateWithLifecycle()
    val caffeineToday = caffeineIntakes.filter { it.day == LocalDate.now().toString() }

    val allHabits = snapshot?.habits ?: emptyList()
    val records = snapshot?.records ?: emptyMap()
    val caffeineTodayMg = caffeineToday.sumOf { it.mg }
    val caffeineTodayCount = caffeineToday.size

    val today = LocalDate.now()
    val shownDay = vm.day
    val isToday = shownDay == today
    val dayKey = shownDay.toString()

    // Дела на показанный день. Курс, у которого срок уже прошёл, в список не попадает —
    // такие дела уезжают вниз, в группу «Завершённые»: из рутины они ушли, но история нужна.
    val habits = allHabits.filter { habitOnDay(it, shownDay) }
    val completed = allHabits.filter { finishedOn(it, shownDay) }

    var sheetHabit by remember { mutableStateOf<Habit?>(null) }
    var sheetVisible by remember { mutableStateOf(false) }
    var showPermissionCard by remember { mutableStateOf(!notificationsAllowed()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> showPermissionCard = !granted }

    val activeCount = habits.count { it.active }
    val doneCount = habits.count { it.active && records[it.id]?.contains(dayKey) == true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButtonPosition = FabPosition.Center,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Column {
                        Text(
                            text = "Рутина",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = if (isToday) {
                                "сегодня, ${today.format(DAY_FORMAT)}"
                            } else {
                                shownDay.format(WEEKDAY_DAY_FORMAT)
                                    .replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.shiftDay(-1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Предыдущий день")
                    }
                    if (!isToday) {
                        IconButton(onClick = { vm.shiftDay(1) }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Следующий день")
                        }
                        TextButton(onClick = { vm.backToToday() }) { Text("Сегодня") }
                    }
                    // Кофеин — только когда трекер включён: иначе иконка занимала бы
                    // место в шапке у тех, кому он не нужен.
                    if (caffeineSettings.enabled) {
                        IconButton(onClick = onOpenCaffeine) {
                            Icon(
                                Icons.Filled.LocalCafe,
                                contentDescription = "Трекер кофеина",
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Настройки",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            // по центру снизу, как основное действие экрана
            ExtendedFloatingActionButton(
                onClick = {
                    sheetHabit = null
                    sheetVisible = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Добавить") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = Radius.fab,
                // было 6dp по умолчанию: тень делала кнопку «плавающим островом»
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp,
                    focusedElevation = 2.dp,
                    hoveredElevation = 3.dp,
                ),
            )
        },
    ) { padding ->
        // Экраны не прокручиваются: контент раскладывается по доступной высоте.
        // Дела делят свободное место поровну (weight) и сжимаются, если их много,
        // поэтому список никогда не выходит за пределы экрана.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.screen)
                .padding(top = Space.xs, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showPermissionCard) {
                PermissionCard(
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            showPermissionCard = false
                        }
                    },
                    onDismiss = { showPermissionCard = false },
                )
            }

            if (allHabits.isEmpty()) {
                EmptyState(
                    onAdd = {
                        sheetHabit = null
                        sheetVisible = true
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                // Прогресс дня показываем только когда есть что отмечать. Если все дела —
                // завершённые курсы, карточка прогресса с «Все дела на паузе» соврала бы.
                if (habits.isNotEmpty()) {
                    ProgressCard(doneCount = doneCount, total = activeCount)
                }

                // Кофеин — одной строкой между прогрессом и делами: видно норму
                // и сколько приёмов уже отмечено, отметить можно на своём экране.
                if (caffeineSettings.enabled) {
                    CaffeineRow(
                        targetMg = caffeineSettings.targetMg,
                        doneMg = caffeineTodayMg,
                        doneCount = caffeineTodayCount,
                        moments = CaffeineLogic.MOMENTS,
                        onClick = onOpenCaffeine,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    // К верху: при двух-трёх делах список стоит сразу под прогрессом,
                    // а при большом количестве карточки заполняют экран.
                    verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
                ) {
                    groupByPartOfDay(habits).forEach { (title, list) ->
                        SectionHeader(title = title, count = list.size)
                        list.forEach { habit ->
                            HabitCard(
                                habit = habit,
                                done = records[habit.id]?.contains(dayKey) == true,
                                streak = streakOf(records[habit.id].orEmpty(), today),
                                course = courseLabel(habit, today),
                                // Делят свободное место поровну и сжимаются, если дел много;
                                // при малом числе дел сохраняют естественную высоту
                                // (fill = false), иначе карточки растягивались бы на пол-экрана.
                                modifier = Modifier.weight(1f, fill = false),
                                onToggle = { vm.toggle(habit.id) },
                                onOpenHistory = { onOpenHistory(habit.id) },
                                onEdit = {
                                    sheetHabit = habit
                                    sheetVisible = true
                                },
                                onToggleActive = { vm.setActive(habit, !habit.active) },
                                onTestNotification = { vm.testNotification(habit) },
                            )
                        }
                    }

                    // Курсы, у которых срок вышел: отмечать уже нечего, но можно продлить
                    // («Изменить») и заглянуть в историю отметок.
                    if (completed.isNotEmpty()) {
                        SectionHeader(title = "Завершённые", count = completed.size)
                        completed.forEach { habit ->
                            HabitCard(
                                habit = habit,
                                done = records[habit.id]?.contains(dayKey) == true,
                                streak = streakOf(records[habit.id].orEmpty(), today),
                                course = "",
                                finished = true,
                                modifier = Modifier.weight(1f, fill = false),
                                onToggle = {},
                                onOpenHistory = { onOpenHistory(habit.id) },
                                onEdit = {
                                    sheetHabit = habit
                                    sheetVisible = true
                                },
                                onToggleActive = { vm.setActive(habit, !habit.active) },
                                onTestNotification = { vm.testNotification(habit) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (sheetVisible) {
        HabitSheet(
            habit = sheetHabit,
            onDismiss = { sheetVisible = false },
            onSave = { name, hour, minute, durationDays ->
                vm.saveHabit(sheetHabit, name, hour, minute, durationDays)
                sheetVisible = false
            },
            onDelete = {
                sheetHabit?.let { vm.delete(it.id) }
                sheetVisible = false
            },
        )
    }
}

@Composable
private fun ProgressCard(doneCount: Int, total: Int) {
    val allDone = total > 0 && doneCount == total

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
                    text = when {
                        total == 0 -> "Все дела на паузе"
                        allDone -> "Всё сделано"
                        else -> "$doneCount из $total"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (allDone) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (allDone) "можно не думать об этом" else "за день",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (allDone) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            // Сегменты вместо полоски: видно ровно столько дел, сколько их есть.
            // Раньше рядом с полоской жила отдельная зелёная точка — читалось как два индикатора.
            // Ширина сегмента фиксированная, иначе растянутый ряд снова выглядит полоской.
            // При большом числе дел сегменты превращаются в пунктир — тогда лучше без них:
            // «16 из 16» в тексте понятнее.
            if (!allDone && total in 1..12) {
                Spacer(Modifier.height(Space.s))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(total) { i ->
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
private fun CaffeineRow(
    targetMg: Int,
    doneMg: Int,
    doneCount: Int,
    moments: Int,
    onClick: () -> Unit,
) {
    val allDone = doneCount >= moments
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
        Column(Modifier.padding(horizontal = Space.l, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocalCafe,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.caption),
                    tint = if (allDone) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.width(Space.s))
                Text(
                    text = "Кофеин",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (allDone) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$doneMg из $targetMg мг",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (allDone) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!allDone) {
                Spacer(Modifier.height(6.dp))
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
private fun PermissionCard(onGrant: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Без разрешения на уведомления напоминания не придут",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(Space.s))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text("Позже")
                }
                Spacer(Modifier.width(Space.xs))
                Button(onClick = onGrant, shape = Radius.field) { Text("Разрешить") }
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Space.l),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(Space.l))
        Text("Пока пусто", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Space.s))
        Text(
            text = "Добавь дело — например «Лекарство» на 08:00\nи «Капли в нос» на 22:00.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = "Отмечай кружком, когда сделал — и не держи это в голове.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.l))
        Button(onClick = onAdd, shape = Radius.field) { Text("Добавить первое дело") }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 0.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(Locale("ru")),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    done: Boolean,
    streak: Int,
    course: String,
    modifier: Modifier = Modifier,
    finished: Boolean = false,
    onToggle: () -> Unit,
    onOpenHistory: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onTestNotification: () -> Unit,
) {
    var menuVisible by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    // Состояние «выполнено» кодируется только галкой и зачёркнутым приглушённым текстом.
    // Раньше к ним добавлялись ещё зелёная рамка карточки и подсветка названия — тройное
    // кодирование одного состояния. Рамка у всех карточек теперь одинаковая.
    val checkScale by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    val circleColor by animateColorAsState(
        targetValue = if (done) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "circle",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        // Карточка измеряет доступную высоту: когда дел много, места мало — прячем
        // вторую строку (время/серию) и уменьшаем кружок, чтобы всё влезло без прокрутки.
        // ВАЖНО: именно fillMaxWidth, а не fillMaxSize — иначе содержимое растягивается
        // до выделенной доли высоты и при двух делах карточки занимают пол-экрана.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxHeight < 42.dp
            val circle = if (compact) 24.dp else 28.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(start = Space.m, end = Space.xs, top = 1.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Невыполненный кружок получил обводку: раньше он сливался с фоном карточки
                // и не читался как кнопка рядом с насыщенным зелёным выполненных.
                Box(
                    modifier = Modifier
                        .size(circle)
                        .clip(CircleShape)
                        .background(circleColor)
                        .border(
                            width = 1.5.dp,
                            color = when {
                                finished -> MaterialTheme.colorScheme.outlineVariant
                                done -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        )
                        .then(
                            if (finished) {
                                Modifier
                            } else {
                                Modifier.clickable(onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggle()
                                })
                            },
                        )
                        .semantics {
                            contentDescription = when {
                                finished -> "${habit.name}: курс завершён"
                                done -> "${habit.name}: отмечено. Нажми, чтобы снять отметку"
                                else -> "${habit.name}: не отмечено. Нажми, чтобы отметить"
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(if (compact) 14.dp else 16.dp)
                            .graphicsLayer {
                                scaleX = checkScale
                                scaleY = checkScale
                            },
                    )
                }

                Spacer(Modifier.width(Space.m))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenHistory),
                ) {
                    if (compact) {
                        // В сжатом режиме время переезжает в строку с названием,
                        // иначе оно терялось совсем — а это ключевая информация.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (done) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textDecoration = if (done) TextDecoration.LineThrough else null,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (habit.hour >= 0) {
                                Spacer(Modifier.width(Space.s))
                                Text(
                                    text = String.format("%02d:%02d", habit.hour, habit.minute),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (done) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (habit.hour >= 0) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSize.caption),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(Space.xs))
                                Text(
                                    text = String.format("%02d:%02d", habit.hour, habit.minute),
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
                            if (!habit.active) {
                                Spacer(Modifier.width(Space.s))
                                Text(
                                    text = "на паузе",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            } else if (finished) {
                                // Срок вышел: напоминать и отмечать уже нечего, но дело
                                // остаётся внизу списка, пока его не продлят или не удалят.
                                Spacer(Modifier.width(Space.s))
                                Text(
                                    text = "курс завершён",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else if (course.isNotEmpty()) {
                                Spacer(Modifier.width(Space.s))
                                Text(
                                    text = course,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else if (streak > 1) {
                                Spacer(Modifier.width(Space.s))
                                Text(
                                    text = "${daysWord(streak)} подряд",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                Box {
                    // значок остаётся 20dp, а зона нажатия — не меньше 48dp (норма M3)
                    IconButton(
                        onClick = { menuVisible = true },
                        modifier = Modifier.size(if (compact) 44.dp else 48.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Меню: ${habit.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.action),
                        )
                    }
                DropdownMenu(
                    expanded = menuVisible,
                    onDismissRequest = { menuVisible = false },
                    shape = Radius.menu,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp,
                ) {
                    DropdownMenuItem(
                        text = { Text("Изменить") },
                        onClick = {
                            menuVisible = false
                            onEdit()
                        },
                    )
                    if (habit.hour >= 0) {
                        DropdownMenuItem(
                            text = { Text("Проверить напоминание") },
                            onClick = {
                                menuVisible = false
                                onTestNotification()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text(if (habit.active) "Убрать на паузу" else "Вернуть в рутину")
                        },
                        onClick = {
                            menuVisible = false
                            onToggleActive()
                        },
                    )
                }
                }
            }
        }
    }
}

private fun groupByPartOfDay(habits: List<Habit>): List<Pair<String, List<Habit>>> {
    val morning = habits.filter { it.active && it.hour in 0..11 }
    val day = habits.filter { it.active && it.hour in 12..16 }
    val evening = habits.filter { it.active && it.hour >= 17 }
    val none = habits.filter { it.active && it.hour < 0 }
    val paused = habits.filter { !it.active }
    return listOf(
        "Утро" to morning,
        "День" to day,
        "Вечер" to evening,
        "Без времени" to none,
        "На паузе" to paused,
    ).filter { it.second.isNotEmpty() }
}

internal fun streakOf(days: Set<String>, today: LocalDate): Int {
    var cursor = today
    if (!days.contains(cursor.toString())) cursor = cursor.minusDays(1)
    var n = 0
    while (days.contains(cursor.toString())) {
        n++
        cursor = cursor.minusDays(1)
    }
    return n
}
