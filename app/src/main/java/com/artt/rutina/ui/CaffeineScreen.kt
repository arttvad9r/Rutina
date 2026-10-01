package com.artt.rutina.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.rutina.data.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaffeineScreen(settings: CaffeineSettings, intakes: List<CaffeineIntake>, today: LocalDate,
    onBack: () -> Unit, onOpenSettings: () -> Unit, onToggleIntake: (Int, Int) -> Unit,
    onEditDose: (Int, Int, Long?) -> Unit,
    onStartTimer: (Int, Int?) -> Unit, onCancelTimer: () -> Unit) {
    val dayIntakes = intakes.filter { it.day == today.toString() }
    val taken = dayIntakes.filter { it.recorded }
    val plan = CaffeineLogic.plan(settings.targetMg, settings.wakeMinutes, settings.bedtimeMinutes)
    val slots = CaffeineLogic.visibleSlots(plan.doses, dayIntakes)
    var editing by rememberSaveable(today.toString()) { mutableIntStateOf(-1) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settings.timerEndMs) {
        now = System.currentTimeMillis()
        while (settings.timerEndMs != null && settings.timerEndMs > now) {
            delay(1000); now = System.currentTimeMillis()
        }
    }
    val timerActive = settings.timerEndMs?.let { it > now } == true
    var historyExpanded by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text("Кофеин") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
            actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Настройки трекера") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = Space.screen).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("${CaffeineLogic.totalMg(dayIntakes)} из ${settings.targetMg} мг", style = MaterialTheme.typography.displaySmall)
                Text("Принято сегодня", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Card(modifier = Modifier.fillMaxWidth(), shape = Radius.card, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Приёмы сегодня", style = MaterialTheme.typography.bodyLarge)
                        Text("${taken.size} из ${slots.size}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    slots.forEachIndexed { rowIndex, slot ->
                        val intake = dayIntakes.firstOrNull { it.slot == slot }
                        val planned = plan.doses.getOrNull(slot)
                        val mg = intake?.mg ?: planned ?: CaffeineLogic.STEP_MG
                        val done = intake?.recorded == true
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f).clip(Radius.field).clickable {
                                editing = -1; onToggleIntake(slot, mg)
                            }.padding(vertical = 18.dp).semantics {
                                contentDescription = "Приём ${slot + 1}, $mg мг, ${if (done) "отмечен" else "не отмечен"}"
                            }, verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = if (done) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.size(26.dp)) {
                                    if (done) Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null,
                                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text("$mg мг", style = MaterialTheme.typography.titleLarge)
                                    Text(buildString {
                                        append(listOf("Первый", "Второй", "Третий").getOrNull(slot)?.plus(" приём") ?: "Приём ${slot + 1}")
                                        if (done) append(" · принято в " + Instant.ofEpochMilli(intake!!.at).atZone(ZoneId.systemDefault()).format(TIME_FORMAT))
                                        else plan.schedule.times.getOrNull(slot)?.let { append(" · " + CaffeineLogic.timeOf(it).format(TIME_FORMAT)) }
                                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (planned != null && mg != planned) Text("По плану $planned мг", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { editing = if (editing == slot) -1 else slot }) {
                                Icon(Icons.Default.Edit, "Изменить приём ${slot + 1}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (editing == slot) DoseEditor(mg, intake?.takeIf { it.recorded }?.at, today,
                            onCancel = { editing = -1 }, onSave = { value, at -> onEditDose(slot, value, at); editing = -1 })
                        if (rowIndex < slots.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Text("Нажми на приём, чтобы отметить или снять отметку.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimerCard(timerActive, if (timerActive) settings.timerEndMs!! - now else 0,
                settings.timerEndMs, onStart = { onStartTimer(it, null) }, onCancel = onCancelTimer)
            Column {
                TextButton(onClick = { historyExpanded = !historyExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Text("История", modifier = Modifier.weight(1f))
                    Icon(if (historyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
                if (historyExpanded) {
                    val history = intakes.filter { it.recorded }.groupBy { it.day }.toSortedMap(compareByDescending { it })
                    if (history.isEmpty()) Text("Пока нет отметок", style = MaterialTheme.typography.bodyMedium)
                    history.entries.take(30).forEach { (date, list) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (date == today.toString()) "Сегодня" else LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))),
                                style = MaterialTheme.typography.bodyMedium)
                            Text("${list.sumOf { it.mg }} мг · ${list.size} приёма", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoseEditor(initialMg: Int, initialAt: Long?, today: LocalDate,
    onCancel: () -> Unit, onSave: (Int, Long?) -> Unit) {
    var mg by rememberSaveable(initialMg, initialAt) { mutableIntStateOf(initialMg) }
    val time = initialAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
    var hour by rememberSaveable(initialAt) { mutableIntStateOf(time?.hour ?: 8) }
    var minute by rememberSaveable(initialAt) { mutableIntStateOf(time?.minute ?: 0) }
    var picker by remember { mutableStateOf(false) }
    Surface(shape = Radius.field, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(12.dp)) {
            Text("Доза · шаг 50 мг", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                StepperButton(Icons.Default.Remove, "Уменьшить дозу на 50 мг", mg > 50) { mg = (mg - 50).coerceAtLeast(50) }
                Text("$mg мг", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                StepperButton(Icons.Default.Add, "Увеличить дозу на 50 мг", mg < CaffeineLogic.MAX_INTAKE_MG) { mg = (mg + 50).coerceAtMost(CaffeineLogic.MAX_INTAKE_MG) }
            }
            if (initialAt != null) TextButton(onClick = { picker = true }) { Text("Время приёма · %02d:%02d".format(Locale.ROOT, hour, minute)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text("Отмена") }
                Button(onClick = {
                    val at = initialAt?.let { today.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
                    onSave(mg, at)
                }, shape = Radius.field) { Text("Сохранить") }
            }
        }
    }
    if (picker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(onDismissRequest = { picker = false }, title = { Text("Время приёма") }, shape = Radius.card,
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { TimePicker(state) } },
            confirmButton = { TextButton(onClick = { hour = state.hour; minute = state.minute; picker = false }) { Text("Ок") } },
            dismissButton = { TextButton(onClick = { picker = false }) { Text("Отмена") } })
    }
}

@Composable
private fun TimerCard(active: Boolean, remaining: Long, end: Long?, onStart: (Int) -> Unit, onCancel: () -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(CaffeineLogic.DEFAULT_TIMER_MINUTES) }
    Card(modifier = Modifier.fillMaxWidth(), shape = Radius.card, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text("Пауза", style = MaterialTheme.typography.bodyLarge)
            if (active) {
                val seconds = ((remaining + 999) / 1000).coerceAtLeast(0)
                Text("%02d:%02d:%02d".format(Locale.ROOT, seconds / 3600, seconds / 60 % 60, seconds % 60),
                    Modifier.fillMaxWidth().padding(vertical = 16.dp), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                Text("До " + Instant.ofEpochMilli(end!!).atZone(ZoneId.systemDefault()).format(TIME_FORMAT),
                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = Radius.field) { Text("Отменить таймер") }
            } else {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(Icons.Default.Remove, "Уменьшить паузу на 15 минут", minutes > 15) { minutes -= 15 }
                    Text("$minutes мин", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                    StepperButton(Icons.Default.Add, "Увеличить паузу на 15 минут", minutes < CaffeineLogic.MAX_TIMER_MINUTES) { minutes += 15 }
                }
                Button(onClick = { onStart(minutes) }, modifier = Modifier.fillMaxWidth(), shape = Radius.field) { Text("Запустить таймер") }
            }
        }
    }
}

@Composable
internal fun StepperButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedIconButton(onClick = onClick, enabled = enabled, shape = Radius.field, modifier = Modifier.size(48.dp)) {
        Icon(icon, description, modifier = Modifier.size(20.dp))
    }
}
