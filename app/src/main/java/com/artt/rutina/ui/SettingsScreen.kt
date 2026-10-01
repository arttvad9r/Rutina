package com.artt.rutina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.artt.rutina.data.CaffeineLogic
import com.artt.rutina.data.CaffeineSettings

/**
 * Настройки приложения и перенос данных. Трекер кофеина нужен
 * не всегда, поэтому по умолчанию выключен и не занимает место на главном экране.
 * Норма, подъём и сон задаются на самом трекере: они нужны, только когда он включён.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: CaffeineSettings,
    onBack: () -> Unit,
    onSetCaffeineEnabled: (Boolean) -> Unit,
    onOpenCaffeineSettings: () -> Unit,
) {
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
                title = { Text("Настройки", style = MaterialTheme.typography.titleMedium) },
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
        ) {
            Text("Дополнительные функции", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Space.l, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Только название и выключатель: норма и расписание принадлежат
                // настройкам самого трекера — здесь они пересказывались второй раз.
                Column(Modifier.weight(1f)) {
                    Text("Трекер кофеина", style = MaterialTheme.typography.bodyLarge)
                    Text("Приёмы, дневной план и таймер", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(Space.m))
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onSetCaffeineEnabled,
                    colors = rutinaSwitchColors(),
                )
            }
            if (settings.enabled) OutlinedButton(onClick = onOpenCaffeineSettings,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = Radius.field) {
                Text("Настроить трекер")
            }
            Text("При выключении трекер скрывается с главной. Настройки и история сохраняются.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp))
            BackupControls()
        }
    }
}
