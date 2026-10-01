package com.artt.rutina.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun BackupControls(vm: MainViewModel = mainViewModel()) {
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportBackup)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::prepareImport)
    }
    HorizontalDivider(Modifier.padding(top = 28.dp, bottom = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Text("Резервная копия", style = MaterialTheme.typography.titleMedium)
    Text("Сохраните привычки, отметки, записи кофеина и настройки в файл, чтобы восстановить их или перенести на другой телефон.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
    OutlinedButton(onClick = { export.launch("Rutina-${LocalDate.now()}.json") }, enabled = !vm.backupBusy,
        modifier = Modifier.fillMaxWidth(), shape = Radius.field) { Text("Экспортировать данные") }
    OutlinedButton(onClick = { import.launch(arrayOf("*/*")) }, enabled = !vm.backupBusy,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = Radius.field) { Text("Импортировать данные") }
    Text("Импорт заменяет текущие данные. Запущенный таймер не переносится.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp))
    if (vm.backupBusy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 16.dp))
    vm.backupMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp).semantics { liveRegion = LiveRegionMode.Polite }) }
    vm.pendingBackup?.let { backup ->
        val date = Instant.ofEpochMilli(backup.exportedAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru")))
        AlertDialog(onDismissRequest = vm::dismissImport, shape = Radius.card,
            title = { Text("Восстановить данные?") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text("Копия от $date.\n\nПривычки: ${backup.habits.size}\nОтметки: ${backup.records.size}\nПриёмы кофеина: ${backup.intakes.count { it.recorded }}\n\nТекущие привычки, история и настройки будут полностью заменены. Если они нужны, сначала сохраните их экспортом.") } },
            confirmButton = { TextButton(onClick = vm::confirmImport, enabled = !vm.backupBusy) { Text("Заменить данные") } },
            dismissButton = { TextButton(onClick = vm::dismissImport, enabled = !vm.backupBusy) { Text("Отмена") } })
    }
}
