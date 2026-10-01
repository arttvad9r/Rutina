package com.artt.rutina.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artt.rutina.RutinaApp
import com.artt.rutina.data.*
import com.artt.rutina.notif.CaffeineTimer
import com.artt.rutina.notif.Reminders
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru"))

@Composable
fun RutinaRoot(notificationsAllowed: () -> Boolean) {
    var screen by rememberSaveable { mutableStateOf("today") }
    var habitId by rememberSaveable { mutableLongStateOf(0L) }
    var settingsFrom by rememberSaveable { mutableStateOf("coffee") }
    var today by remember { mutableStateOf(LocalDate.now()) }
    val app = LocalContext.current.applicationContext as RutinaApp
    val vm = mainViewModel()
    val caffeineSettings by vm.caffeineSettings.collectAsStateWithLifecycle()
    val intakes by vm.caffeineIntakes.collectAsStateWithLifecycle()
    fun back() { screen = if (screen == "coffee-settings") settingsFrom else "today" }
    BackHandler(enabled = screen != "today") { back() }
    LaunchedEffect(Unit) {
        Reminders.rescheduleAll(app, app.repo.habits.first())
        val settings = app.repo.caffeineSettingsNow()
        settings.timerEndMs?.takeIf { settings.enabled && it > System.currentTimeMillis() }
            ?.let { CaffeineTimer.schedule(app, it) }
        while (true) {
            val now = LocalDate.now()
            if (now != today) {
                if (vm.day == today) vm.backToToday()
                today = now
            }
            delay(10_000)
        }
    }
    when (screen) {
        "history" -> HistoryScreen(habitId = habitId, onBack = { back() }, today = today)
        "settings" -> SettingsScreen(caffeineSettings, { back() }, vm::setCaffeineEnabled,
            onOpenCaffeineSettings = { settingsFrom = "settings"; screen = "coffee-settings" })
        "coffee" -> CaffeineScreen(caffeineSettings, intakes, today, { back() },
            onOpenSettings = { settingsFrom = "coffee"; screen = "coffee-settings" },
            onToggleIntake = vm::toggleCaffeine,
            onEditDose = vm::editCaffeineDose,
            onStartTimer = vm::startCaffeineTimer, onCancelTimer = vm::cancelCaffeineTimer)
        "coffee-settings" -> CaffeineSettingsScreen(caffeineSettings, { back() },
            vm::setCaffeineTarget, vm::setCaffeineWake, vm::setCaffeineBedtime)
        else -> TodayScreen(notificationsAllowed,
            onOpenHistory = { habitId = it; screen = "history" },
            onOpenCaffeine = { screen = "coffee" }, onOpenSettings = { screen = "settings" }, today = today)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    notificationsAllowed: () -> Boolean,
    onOpenHistory: (Long) -> Unit,
    onOpenCaffeine: () -> Unit,
    onOpenSettings: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val vm = mainViewModel()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val settings by vm.caffeineSettings.collectAsStateWithLifecycle()
    val intakes by vm.caffeineIntakes.collectAsStateWithLifecycle()
    val all = snapshot?.habits.orEmpty()
    val day = vm.day
    val active = all.filter { it.active && habitOnDay(it, day) }
    val paused = all.filter { !it.active && !finishedOn(it, today) }
    val finished = all.filter { finishedOn(it, today) }
    val doneCount = active.count { snapshot?.records?.get(it.id)?.contains(day.toString()) == true }
    val taken = intakes.filter { it.day == today.toString() && it.recorded }
    var showPaused by rememberSaveable { mutableStateOf(false) }
    var showFinished by rememberSaveable { mutableStateOf(false) }
    var sheetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showPermission by rememberSaveable { mutableStateOf(!notificationsAllowed()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        showPermission = !it
    }
    fun edit(habit: Habit?) { sheetId = habit?.id; showSheet = true }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text(if (day == today) "Сегодня" else "Привычки") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = { IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки")
                } })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { edit(null) },
                modifier = Modifier.semantics { contentDescription = "Добавить привычку" },
                icon = { Icon(Icons.Default.Add, contentDescription = null) }, text = { Text("Добавить") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp))
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Space.screen, end = Space.screen, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item("date") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.shiftDay(-1) }) { Icon(Icons.Default.ChevronLeft, "Предыдущий день") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.format(DAY_FORMAT).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium)
                        if (day != today) TextButton(onClick = vm::backToToday) { Text("Сегодня") }
                    }
                    IconButton(onClick = { vm.shiftDay(1) }, enabled = day < today) {
                        Icon(Icons.Default.ChevronRight, "Следующий день")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (showPermission && !notificationsAllowed()) item("permission") {
                Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text("Для напоминаний нужно разрешение на уведомления", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        TextButton(onClick = { showPermission = false }) { Text("Позже") }
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else showPermission = false
                        }) { Text("Разрешить") }
                    }
                }
            }
            if (active.isNotEmpty()) item("progress") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Привычки", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (doneCount == active.size) "Всё выполнено" else "$doneCount из ${active.size}",
                        style = MaterialTheme.typography.bodyMedium)
                }
                LinearProgressIndicator(progress = { doneCount / active.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                    color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant,
                    drawStopIndicator = {})
            }
            if (settings.enabled && day == today) item("caffeine") {
                CaffeineRow(taken, settings.targetMg, onOpenCaffeine)
                Spacer(Modifier.height(12.dp))
            }
            if (snapshot != null && active.isEmpty()) item("empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (all.isEmpty()) "Пока нет привычек" else "На этот день привычек нет",
                        style = MaterialTheme.typography.titleLarge)
                    if (all.isEmpty()) Text("Добавьте первую — и отмечайте каждый день",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
            items(active, key = { "habit-${it.id}" }) { habit ->
                HabitRow(habit, snapshot?.records?.get(habit.id)?.contains(day.toString()) == true,
                    onToggle = { vm.toggle(habit.id) }, onHistory = { onOpenHistory(habit.id) },
                    onEdit = { edit(habit) }, onPause = { vm.setActive(habit, false) },
                    onFinish = { vm.finishHabit(habit) }, onResume = { vm.resumeHabit(habit) })
            }
            if (day == today) {
                if (paused.isNotEmpty()) item("paused-title") {
                    TextButton(onClick = { showPaused = !showPaused }, modifier = Modifier.fillMaxWidth()) {
                        Text("На паузе · ${paused.size}", modifier = Modifier.weight(1f))
                        Icon(if (showPaused) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                }
                if (showPaused) items(paused, key = { "paused-${it.id}" }) { habit ->
                    HabitRow(habit, false, secondary = "На паузе", onToggle = {},
                        onHistory = { onOpenHistory(habit.id) }, onEdit = { edit(habit) },
                        onPause = {}, onFinish = { vm.finishHabit(habit) }, onResume = { vm.resumeHabit(habit) })
                }
                if (finished.isNotEmpty()) item("finished-title") {
                    TextButton(onClick = { showFinished = !showFinished }, modifier = Modifier.fillMaxWidth()) {
                        Text("Завершённые · ${finished.size}", modifier = Modifier.weight(1f))
                        Icon(if (showFinished) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    }
                }
                if (showFinished) items(finished, key = { "finished-${it.id}" }) { habit ->
                    HabitRow(habit, false, secondary = "Завершена", onToggle = {},
                        onHistory = { onOpenHistory(habit.id) }, onEdit = { edit(habit) },
                        onPause = {}, onFinish = {}, onResume = { vm.resumeHabit(habit) })
                }
            }
        }
    }
    if (showSheet) {
        val habit = all.firstOrNull { it.id == sheetId }
        HabitSheet(habit, onDismiss = { showSheet = false }, onSave = { name, hour, minute, duration ->
            vm.saveHabit(habit, name, hour, minute, duration); showSheet = false
        }, onDelete = { habit?.let { vm.delete(it.id) }; showSheet = false })
    }
}

@Composable
private fun HabitRow(habit: Habit, done: Boolean, secondary: String? = null,
    onToggle: () -> Unit, onHistory: () -> Unit, onEdit: () -> Unit,
    onPause: () -> Unit, onFinish: () -> Unit, onResume: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 76.dp), verticalAlignment = Alignment.CenterVertically) {
            if (secondary == null) IconToggleButton(checked = done, onCheckedChange = { onToggle() },
                modifier = Modifier.semantics { contentDescription = if (done) "Снять отметку: ${habit.name}" else "Отметить ${habit.name}" }) {
                Surface(shape = CircleShape, modifier = Modifier.size(26.dp),
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                    border = if (done) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)) {
                    if (done) Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null,
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
                }
            }
            Column(Modifier.weight(1f).clip(Radius.field).clickable(onClick = onHistory)
                .padding(vertical = 14.dp, horizontal = 6.dp)) {
                Text(habit.name, style = MaterialTheme.typography.bodyLarge,
                    color = if (done || secondary != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                val end = courseEnd(habit)
                Text(secondary ?: buildString {
                    append(if (habit.hour < 0) "Без напоминания" else "%02d:%02d".format(Locale.ROOT, habit.hour, habit.minute))
                    if (end != null) append(" · до " + end.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))))
                }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp))
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Действия: ${habit.name}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Изменить") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(if (secondary == null) "Поставить на паузу" else "Возобновить") },
                        onClick = { menu = false; if (secondary == null) onPause() else onResume() })
                    if (secondary != "Завершена") DropdownMenuItem(text = { Text("Завершить") }, onClick = { menu = false; onFinish() })
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CaffeineRow(intakes: List<CaffeineIntake>, targetMg: Int, onClick: () -> Unit) {
    val moments = maxOf(CaffeineLogic.splitDoses(targetMg).size, (intakes.maxOfOrNull { it.slot ?: 0 } ?: -1) + 1)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalCafe, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Кофеин", Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                Text("${intakes.sumOf { it.mg }} мг", style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.padding(start = 26.dp, top = 9.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(moments) { slot -> Box(Modifier.width(24.dp).height(5.dp).clip(Radius.segment)
                    .background(if (intakes.any { it.slot == slot }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) }
            }
        }
    }
}
