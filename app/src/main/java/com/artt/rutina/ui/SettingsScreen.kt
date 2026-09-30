package com.artt.rutina.ui

import androidx.compose.foundation.background
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
 * Настройки приложения. Пока здесь одно: включение трекера кофеина — он нужен
 * не всегда, поэтому по умолчанию выключен и не занимает место на главном экране.
 * Норма, подъём и сон задаются на самом трекере: они нужны, только когда он включён.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: CaffeineSettings,
    onBack: () -> Unit,
    onSetCaffeineEnabled: (Boolean) -> Unit,
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
                .padding(horizontal = Space.screen)
                .padding(top = Space.xs, bottom = Space.m),
        ) {
            // Без общей outline-карточки: это обычная settings-строка, как в системных
            // настройках. Пустота ниже честнее искусственных секций — настройка одна.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radius.card)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Space.l, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Трекер кофеина", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (settings.enabled) {
                            "${settings.targetMg} мг в день"
                        } else {
                            "Дневная норма и напоминания о времени приёма"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(Space.m))
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onSetCaffeineEnabled,
                    colors = rutinaSwitchColors(),
                )
            }
        }
    }
}
