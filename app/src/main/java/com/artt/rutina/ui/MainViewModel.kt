package com.artt.rutina.ui

import android.content.Context
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
import com.artt.rutina.data.Habit
import com.artt.rutina.data.Repo
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

    fun saveHabit(existing: Habit?, name: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            val id = if (existing == null) {
                repo.create(name, hour, minute)
            } else {
                repo.update(existing.copy(name = name.trim(), hour = hour, minute = minute))
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
