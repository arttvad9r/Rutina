package com.artt.rutina.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.artt.rutina.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitSheet(habit: Habit?, onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int) -> Unit, onDelete: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var name by rememberSaveable(habit?.id) { mutableStateOf(habit?.name.orEmpty()) }
    var remind by rememberSaveable(habit?.id) { mutableStateOf(habit != null && habit.hour >= 0) }
    var hour by rememberSaveable(habit?.id) { mutableIntStateOf(habit?.hour?.takeIf { it >= 0 } ?: 8) }
    var minute by rememberSaveable(habit?.id) { mutableIntStateOf(habit?.minute ?: 0) }
    var limited by rememberSaveable(habit?.id) { mutableStateOf((habit?.durationDays ?: 0) > 0) }
    var daysText by rememberSaveable(habit?.id) { mutableStateOf((habit?.durationDays?.takeIf { it > 0 } ?: 30).toString()) }
    var picker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val days = daysText.toIntOrNull()
    val daysValid = days != null && days in 1..3650
    val start = habit?.let { startDay(it.createdAt) } ?: LocalDate.now()
    val format = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, shape = Radius.sheet) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(if (habit == null) "Новая привычка" else "Изменить привычку", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = name, onValueChange = { name = it.take(120) },
                label = { Text("Название") }, placeholder = { Text("Например: Прочитать 10 страниц") },
                singleLine = true, shape = Radius.field, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Напоминание", style = MaterialTheme.typography.bodyLarge)
                    Text("Уведомление каждый день", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = remind, onCheckedChange = { remind = it }, colors = rutinaSwitchColors(),
                    modifier = Modifier.semantics { contentDescription = "Напоминание" })
            }
            if (remind) {
                Text("Время", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { picker = true }, shape = Radius.field, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                    Text("%02d:%02d".format(Locale.ROOT, hour, minute))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ограничить срок", style = MaterialTheme.typography.bodyLarge)
                    Text("Для курса или временной привычки", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = limited, onCheckedChange = { limited = it }, colors = rutinaSwitchColors(),
                    modifier = Modifier.semantics { contentDescription = "Ограничить срок" })
            }
            if (limited) {
                OutlinedTextField(value = daysText, onValueChange = { raw -> daysText = raw.filter(Char::isDigit).take(4) },
                    label = { Text("Длительность, дней") }, singleLine = true, shape = Radius.field,
                    isError = !daysValid,
                    supportingText = { if (!daysValid) Text("Введите от 1 до 3650 дней") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }), modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 30).forEach { n ->
                        FilterChip(selected = days == n, onClick = { daysText = n.toString() },
                            label = { Text(daysWord(n)) }, shape = Radius.field, colors = selectionChipColors())
                    }
                }
                if (daysValid) Text("С ${start.format(format)} по ${courseEnd(start, days!!)!!.format(format)} включительно",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
                if (habit != null) Text("Срок считается от даты создания привычки. Пауза не продлевает срок.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
            FlowRow(Modifier.fillMaxWidth().padding(top = 20.dp),
                maxItemsInEachRow = if (LocalDensity.current.fontScale > 1.2f) 1 else 2,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, shape = Radius.field, modifier = Modifier.weight(1f)) { Text("Отмена") }
                Button(onClick = { keyboard?.hide(); onSave(name.trim(), if (remind) hour else -1, minute, if (limited) days!! else NO_LIMIT) },
                    enabled = name.trim().isNotEmpty() && (!limited || daysValid), shape = Radius.field, modifier = Modifier.weight(1f)) {
                    Text(if (habit == null) "Добавить" else "Сохранить")
                }
            }
            if (habit != null) TextButton(onClick = { confirmDelete = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Удалить привычку") }
        }
    }
    if (picker) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(onDismissRequest = { picker = false }, shape = Radius.card,
            title = { Text("Время напоминания") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { TimePicker(state) } },
            confirmButton = { TextButton(onClick = { hour = state.hour; minute = state.minute; picker = false }) { Text("Ок") } },
            dismissButton = { TextButton(onClick = { picker = false }) { Text("Отмена") } })
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, shape = Radius.card,
        title = { Text("Удалить «${habit?.name}»?") }, text = { Text("Вместе с историей отметок. Отменить нельзя.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Оставить") } })
}

@Composable
internal fun selectionChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer)
