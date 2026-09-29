package com.artt.rutina.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.artt.rutina.data.Habit

/** Готовые варианты времени: показываются как чипы, как и «Другое». */
private val PRESETS = listOf(
    "Утро" to (8 to 0),
    "День" to (14 to 0),
    "Вечер" to (22 to 0),
)

/** Добавление и редактирование дела. Всё содержимое компактное — укладывается на один экран. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitSheet(
    habit: Habit?,
    onDismiss: () -> Unit,
    onSave: (name: String, hour: Int, minute: Int) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboard = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf(habit?.name.orEmpty()) }
    var remind by remember { mutableStateOf(if (habit == null) true else habit.hour >= 0) }
    var hour by remember { mutableStateOf(if ((habit?.hour ?: -1) >= 0) habit!!.hour else 8) }
    var minute by remember { mutableStateOf(habit?.minute ?: 0) }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Клавиатура не открывается сама: только когда пользователь сам поставит курсор в поле.

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = Radius.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = if (habit == null) "Новое дело" else "Изменить дело",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(Space.m))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                placeholder = { Text("Например: Капли в нос") },
                singleLine = true,
                shape = Radius.field,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Space.m))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Напоминать", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Switch(checked = remind, onCheckedChange = { remind = it })
            }

            if (remind) {
                Spacer(Modifier.height(Space.s))
                // У всех вариантов один паттерн выделения: выбран ровно один чип из четырёх.
                // Раньше пресеты были чипами, а «Другое» — кнопкой с зелёным контуром,
                // и понять, что именно выбрано, было нельзя.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PRESETS.forEach { (label, hm) ->
                        FilterChip(
                            selected = hour == hm.first && minute == hm.second && !showPicker,
                            onClick = {
                                hour = hm.first
                                minute = hm.second
                                showPicker = false
                            },
                            label = { Text("$label ${"%02d:%02d".format(hm.first, hm.second)}") },
                            shape = Radius.field,
                            colors = selectionChipColors(),
                        )
                    }
                    // «Другое» — такой же чип, только открывает пикер времени.
                    // Время показываем лишь когда оно не совпадает с пресетом, иначе рядом
                    // стоят два чипа с одинаковым «08:00» и непонятно, что именно выбрано.
                    val isPreset = PRESETS.any { it.second.first == hour && it.second.second == minute }
                    FilterChip(
                        selected = showPicker,
                        onClick = { showPicker = true },
                        label = {
                            Text(
                                if (isPreset) "Другое…"
                                else String.format("Другое %02d:%02d", hour, minute),
                            )
                        },
                        shape = Radius.field,
                        colors = selectionChipColors(),
                    )
                }
            }

            Spacer(Modifier.height(Space.l))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (habit != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(
                            // приглушённый красный: не главный акцент экрана
                            contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        ),
                    ) {
                        Text("Удалить")
                    }
                }
                Spacer(Modifier.weight(1f))
                // «Отмена» нейтральная: зелёный закреплён только за главным действием
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("Отмена")
                }
                Spacer(Modifier.width(Space.s))
                Button(
                    onClick = {
                        keyboard?.hide()
                        onSave(name.trim(), if (remind) hour else -1, minute)
                    },
                    enabled = name.trim().isNotEmpty(),
                    shape = Radius.field,
                ) {
                    Text(if (habit == null) "Добавить" else "Сохранить")
                }
            }
        }
    }

    if (showPicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = {
                showPicker = false
                // если пикер закрыли, не тронув время, возвращаем выбор к пресетам
                if (PRESETS.none { it.second.first == hour && it.second.second == minute }) {
                    hour = 8
                    minute = 0
                }
            },
            onConfirm = { h, m ->
                hour = h
                minute = m
                showPicker = false
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить «${habit?.name}»?") },
            text = { Text("Вместе с историей отметок. Отменить нельзя.") },
            shape = Radius.card,
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Text("Оставить")
                }
            },
        )
    }
}

/** Единый вид выбранного состояния чипа времени. */
@Composable
private fun selectionChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Radius.card,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Ок") } },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text("Отмена")
            }
        },
        title = { Text("Время напоминания") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        },
    )
}
