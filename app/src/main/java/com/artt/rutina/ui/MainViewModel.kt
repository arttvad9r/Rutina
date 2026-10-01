package com.artt.rutina.ui

import android.content.Context
import android.net.Uri
import android.app.NotificationManager
import com.artt.rutina.data.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.IOException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artt.rutina.RutinaApp
import com.artt.rutina.data.CaffeineSettings
import com.artt.rutina.data.CaffeineLogic
import com.artt.rutina.data.Habit
import com.artt.rutina.data.Repo
import com.artt.rutina.data.habitOnDay
import com.artt.rutina.notif.CaffeineTimer
import com.artt.rutina.notif.Notifier
import com.artt.rutina.notif.Reminders
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(
    private val repo: Repo,
    private val context: Context,
) : ViewModel() {

    var backupBusy by mutableStateOf(false)
        private set
    var backupMessage by mutableStateOf<String?>(null)
        private set
    var pendingBackup by mutableStateOf<BackupData?>(null)
        private set

    fun exportBackup(uri: Uri) {
        if (backupBusy) return
        backupBusy = true
        backupMessage = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val text = repo.backup().json()
                    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException()
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                }
                backupMessage = "Резервная копия сохранена."
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { backupMessage = "Не удалось сохранить файл. Выберите другое место и повторите." }
            finally { backupBusy = false }
        }
    }

    fun prepareImport(uri: Uri) {
        if (backupBusy) return
        backupBusy = true
        backupMessage = null
        viewModelScope.launch {
            try {
                pendingBackup = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri) ?: throw IOException()
                    stream.use(BackupData::read)
                }
            } catch (e: CancellationException) { throw e
            } catch (e: IllegalArgumentException) {
                backupMessage = e.message ?: "Файл содержит некорректные данные. Текущие данные не изменены."
            } catch (_: Exception) {
                backupMessage = "Не удалось прочитать резервную копию. Проверьте файл. Текущие данные не изменены."
            } finally { backupBusy = false }
        }
    }

    fun dismissImport() { if (!backupBusy) pendingBackup = null }

    fun confirmImport() {
        val backup = pendingBackup ?: return
        if (backupBusy) return
        backupBusy = true
        viewModelScope.launch {
            try {
                val previous = withContext(Dispatchers.IO) { repo.restore(backup) }
                previous.forEach { Reminders.cancel(context, it) }
                CaffeineTimer.cancel(context)
                context.getSystemService(NotificationManager::class.java)?.cancelAll()
                Reminders.rescheduleAll(context, backup.habits)
                backToToday()
                pendingBackup = null
                backupMessage = "Данные восстановлены из резервной копии."
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                pendingBackup = null
                backupMessage = "Не удалось завершить импорт. Проверьте данные и повторите."
            } finally { backupBusy = false }
        }
    }

    /** null, пока база ещё не прочитана — экран показывает пустое состояние. */
    val snapshot = repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), null)

    /** День, за который показываются отметки (по умолчанию сегодня). */
    var day by mutableStateOf(LocalDate.now())
        private set

    fun goToDay(date: LocalDate) {
        if (date.isAfter(LocalDate.now())) return
        day = date
    }

    fun shiftDay(delta: Long) {
        goToDay(day.plusDays(delta))
    }

    fun backToToday() {
        day = LocalDate.now()
    }

    fun toggle(habitId: Long) {
        val d = day
        viewModelScope.launch {
            repo.toggle(habitId, d)
            if (repo.isDone(habitId, d)) Notifier.clear(context, habitId)
        }
    }

    fun toggleOnDay(habit: Habit, date: LocalDate) {
        if (!com.artt.rutina.data.canMarkDay(habit, date, LocalDate.now())) return
        viewModelScope.launch {
            repo.toggle(habit.id, date)
            if (date == LocalDate.now() && repo.isDone(habit.id, date)) Notifier.clear(context, habit.id)
        }
    }

    fun finishHabit(habit: Habit) {
        viewModelScope.launch {
            repo.update(habit.copy(active = false, finishedAt = System.currentTimeMillis()))
            Reminders.cancel(context, habit)
            Notifier.clear(context, habit.id)
        }
    }

    fun resumeHabit(habit: Habit) {
        viewModelScope.launch {
            val expired = com.artt.rutina.data.courseEnd(habit)?.isBefore(LocalDate.now()) == true
            val updated = habit.copy(active = true, finishedAt = null,
                durationDays = if (expired) com.artt.rutina.data.NO_LIMIT else habit.durationDays)
            repo.update(updated)
            Reminders.schedule(context, updated)
        }
    }

    fun saveHabit(existing: Habit?, name: String, hour: Int, minute: Int, durationDays: Int) {
        viewModelScope.launch {
            val id = if (existing == null) {
                repo.create(name, hour, minute, durationDays)
            } else {
                repo.update(
                    existing.copy(
                        name = name.trim(),
                        hour = hour,
                        minute = minute,
                        durationDays = durationDays,
                    ),
                )
                existing.id
            }
            repo.habit(id)?.let { Reminders.schedule(context, it) }
        }
    }

    fun setActive(habit: Habit, active: Boolean) {
        viewModelScope.launch {
            val updated = habit.copy(active = active)
            repo.update(updated)
            Reminders.schedule(context, updated)
            if (!active) Notifier.clear(context, habit.id)
        }
    }

    /**
     * Дела, актуальные для показанного дня: созданные не позже этого дня и с незаконченным
     * курсом. Завершённые курсы сюда не попадают — поэтому дело с истёкшим сроком само
     * исчезает из списка, а его история остаётся доступной в разделе «Завершённые».
     */
    fun habitsForDay(day: LocalDate): List<Habit> =
        (snapshot.value?.habits ?: emptyList()).filter { habitOnDay(it, day) }

    fun delete(habitId: Long) {
        viewModelScope.launch {
            repo.habit(habitId)?.let { Reminders.cancel(context, it) }
            Notifier.clear(context, habitId)
            repo.delete(habitId)
        }
    }

    /** Показать напоминание прямо сейчас — проверить, что уведомления доходят. */
    fun testNotification(habit: Habit) {
        Notifier.post(context, habit, LocalDate.now(), force = true)
    }

    // --- Трекер кофеина ---

    val caffeineSettings = repo.caffeineSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), CaffeineSettings())

    val caffeineIntakes = repo.caffeineIntakes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), emptyList())

    /** Включить или выключить трекер кофеина (переключатель в настройках). */
    fun setCaffeineEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val s = repo.caffeineSettingsNow()
            repo.saveCaffeineSettings(s.copy(enabled = enabled,
                timerEndMs = if (enabled) s.timerEndMs else null,
                timerSlot = if (enabled) s.timerSlot else null))
            if (!enabled) CaffeineTimer.cancel(context)
        }
    }

    fun setCaffeineTarget(targetMg: Int) {
        viewModelScope.launch {
            repo.setCaffeineTarget(targetMg.coerceIn(CaffeineLogic.MIN_TARGET, CaffeineLogic.MAX_TARGET))
        }
    }

    fun setCaffeineBedtime(minutes: Int) {
        viewModelScope.launch {
            val s = repo.caffeineSettingsNow()
            repo.saveCaffeineSettings(s.copy(bedtimeMinutes = minutes))
        }
    }

    fun setCaffeineWake(minutes: Int) {
        viewModelScope.launch {
            val s = repo.caffeineSettingsNow()
            repo.saveCaffeineSettings(s.copy(wakeMinutes = minutes))
        }
    }

    /** Отметить приём кофеина: доза идёт по порядку из разбивки нормы. */
    fun addCaffeine(mg: Int) {
        viewModelScope.launch {
            repo.addCaffeine(LocalDate.now(), mg)
        }
    }

    fun removeCaffeine(id: Long) {
        viewModelScope.launch { repo.removeCaffeine(id) }
    }

    fun toggleCaffeine(slot: Int, mg: Int) {
        viewModelScope.launch { repo.toggleCaffeine(LocalDate.now(), slot, mg) }
    }

    fun editCaffeineDose(slot: Int, mg: Int, at: Long?) {
        viewModelScope.launch { repo.editCaffeineDose(LocalDate.now(), slot, mg, at) }
    }

    /** Запустить таймер до следующего приёма. */
    fun startCaffeineTimer(minutes: Int, slot: Int?) {
        viewModelScope.launch {
            val end = System.currentTimeMillis() + minutes * 60_000L
            val s = repo.caffeineSettingsNow()
            repo.saveCaffeineSettings(s.copy(timerEndMs = end, timerSlot = slot))
            CaffeineTimer.schedule(context, end)
        }
    }

    fun cancelCaffeineTimer() {
        viewModelScope.launch {
            val s = repo.caffeineSettingsNow()
            repo.saveCaffeineSettings(s.copy(timerEndMs = null, timerSlot = null))
            CaffeineTimer.cancel(context)
        }
    }
}

@Composable
fun mainViewModel(): MainViewModel {
    val context = LocalContext.current.applicationContext
    val app = context as RutinaApp
    return viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(app.repo, app) as T
    })
}
