package com.artt.rutina.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artt.rutina.data.CaffeineLogic
import com.artt.rutina.data.CaffeineSettings
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Настройки трекера кофеина: дневная норма и время подъёма и сна.
 *
 * Раньше подъём и сон стояли прямо на экране кофеина и занимали там две строки с подписью.
 * На самом экране нужны приёмы и таймер, поэтому настройки уехали за шестерёнку в шапке,
 * а вместо них осталась одна строка-сводка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaffeineSettingsScreen(
    settings: CaffeineSettings,
    onBack: () -> Unit,
    onSetTarget: (Int) -> Unit,
    onSetWake: (Int) -> Unit,
    onSetBedtime: (Int) -> Unit,
) {
    val plan = CaffeineLogic.plan(
        targetMg = settings.targetMg,
        wakeMinutes = settings.wakeMinutes,
        bedtimeMinutes = settings.bedtimeMinutes,
    )

    var wakePicker by remember { mutableStateOf(false) }
    var bedtimePicker by remember { mutableStateOf(false) }

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
                    Text("Настройки кофеина", style = MaterialTheme.typography.titleLarge)
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen)
                .padding(top = Space.xs, bottom = Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Radius.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier.padding(horizontal = Space.l, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("План на день", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Space.s))
                    // Ручной выбор как у таймера: «− 150 мг +», шаг 50, границы 50–600.
                    // Чипы-пресеты заставляли выбирать из готового списка — теперь любое
                    // значение из диапазона доступно без захода в «Другое».
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepperButton(Icons.Filled.Remove, "Меньше", enabled = settings.targetMg > CaffeineLogic.MIN_TARGET) {
                            onSetTarget(settings.targetMg - CaffeineLogic.STEP_MG)
                        }
                        Text(
                            text = "${settings.targetMg} мг",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = Space.m),
                            textAlign = TextAlign.Center,
                        )
                        StepperButton(Icons.Filled.Add, "Больше", enabled = settings.targetMg < CaffeineLogic.MAX_TARGET) {
                            onSetTarget(settings.targetMg + CaffeineLogic.STEP_MG)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(200, 400).forEach { mg ->
                            OutlinedButton(onClick = { onSetTarget(mg) }, shape = Radius.field, modifier = Modifier.weight(1f)) {
                                Text("$mg мг")
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth()) {
                        plan.doses.forEachIndexed { index, dose ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$dose мг", style = MaterialTheme.typography.bodyMedium)
                                plan.schedule.times.getOrNull(index)?.let {
                                    Text(CaffeineLogic.timeOf(it).format(TIME_FORMAT), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Text("Дозы кратны 50 мг", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                }
            }

            Text("Отмеченные приёмы сохраняются при изменении плана.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Radius.card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(horizontal = Space.l, vertical = 10.dp)) {
                    // Тап по времени открывает выбор — как в шите дела.
                    TimeRow(
                        icon = { tint ->
                            Icon(
                                Icons.Filled.WbSunny,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.caption),
                                tint = tint,
                            )
                        },
                        label = "Подъём",
                        minutes = settings.wakeMinutes,
                        onClick = { wakePicker = true },
                    )
                    Spacer(Modifier.height(Space.xs))
                    TimeRow(
                        icon = { tint ->
                            Icon(
                                Icons.Filled.NightlightRound,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.caption),
                                tint = tint,
                            )
                        },
                        label = "Сон",
                        minutes = settings.bedtimeMinutes,
                        onClick = { bedtimePicker = true },
                    )
                    Spacer(Modifier.height(Space.s))
                    Text(
                        text = if (plan.schedule.times.isEmpty()) {
                            "При таком подъёме и сне окна для приёмов нет"
                        } else {
                            "Последний приём до " +
                                CaffeineLogic.timeOf(plan.schedule.lastAllowedMinutes).format(TIME_FORMAT) +
                                " · за ${CaffeineLogic.FREE_HOURS_BEFORE_SLEEP} ч до сна"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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

/** Строка «Подъём 10:00»: слева иконка и подпись, справа время, которое меняется тапом. */
@Composable
private fun TimeRow(
    icon: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
    label: String,
    minutes: Int,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon(tint)
        Spacer(Modifier.width(Space.s))
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(
            text = CaffeineLogic.timeOf(minutes).format(TIME_FORMAT),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        },
    )
}
