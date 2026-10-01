package com.artt.rutina.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import java.time.LocalDate

/** Всё, что нужно экранам: привычки + их отметки. */
data class Snapshot(
    val habits: List<Habit>,
    val records: Map<Long, Set<String>>,
)

class Repo(private val db: RutinaDb) {

    suspend fun backup(): BackupData = db.withTransaction {
        BackupData(db.habits().all(), db.records().all(), db.caffeine().all(),
            caffeineSettingsNow().copy(timerEndMs = null, timerSlot = null))
    }

    suspend fun restore(backup: BackupData): List<Habit> {
        backup.validate()
        return db.withTransaction {
            val previous = db.habits().all()
            db.clearAllTables()
            db.habits().insertAll(backup.habits)
            db.records().insertAll(backup.records)
            db.caffeine().insertAll(backup.intakes)
            saveCaffeineSettings(backup.settings.copy(timerEndMs = null, timerSlot = null))
            previous
        }
    }

    val habits: Flow<List<Habit>> = db.habits().observeAll()

    val snapshot: Flow<Snapshot> = combine(habits, db.records().observeAll()) { hs, rs ->
        Snapshot(hs, rs.groupBy({ it.habitId }, { it.day }).mapValues { it.value.toSet() })
    }

    fun observeHabit(id: Long): Flow<List<String>> =
        db.records().observeForHabit(id).map { list -> list.map { it.day } }

    suspend fun habit(id: Long): Habit? = db.habits().byId(id)

    suspend fun create(name: String, hour: Int, minute: Int, durationDays: Int = NO_LIMIT): Long =
        db.habits().insert(
            Habit(
                name = name.trim(),
                hour = hour,
                minute = minute,
                sortOrder = db.habits().all().size,
                durationDays = durationDays,
            ),
        )

    suspend fun update(habit: Habit) = db.habits().update(habit)

    suspend fun delete(id: Long) {
        db.records().forgetHabit(id)
        db.habits().deleteById(id)
    }

    suspend fun isDone(habitId: Long, day: LocalDate): Boolean =
        db.records().isDone(habitId, day.toString()) > 0

    suspend fun toggle(habitId: Long, day: LocalDate) {
        if (isDone(habitId, day)) {
            db.records().unmark(habitId, day.toString())
        } else {
            db.records().mark(Record(habitId, day.toString()))
        }
    }

    suspend fun setDone(habitId: Long, day: LocalDate, done: Boolean) {
        if (done) db.records().mark(Record(habitId, day.toString()))
        else db.records().unmark(habitId, day.toString())
    }

    // --- Трекер кофеина ---

    val caffeineIntakes: Flow<List<CaffeineIntake>> = db.caffeine().observeAll()

    /** Настройки трекера; пока строки нет, отдаём значения по умолчанию (выключен). */
    val caffeineSettings: Flow<CaffeineSettings> =
        db.caffeine().observeSettings().map { it ?: CaffeineSettings() }

    suspend fun caffeineSettingsNow(): CaffeineSettings =
        db.caffeine().settings() ?: CaffeineSettings()

    suspend fun saveCaffeineSettings(settings: CaffeineSettings) =
        db.caffeine().saveSettings(settings.copy(id = 1))

    suspend fun addCaffeine(day: LocalDate, mg: Int, at: Long = System.currentTimeMillis()) =
        db.caffeine().insert(CaffeineIntake(day = day.toString(), at = at, mg = mg))

    suspend fun removeCaffeine(id: Long) = db.caffeine().deleteById(id)

    /** Номер приёма сохраняется: снятие первой отметки не сдвигает вторую и третью. */
    suspend fun toggleCaffeine(day: LocalDate, slot: Int, mg: Int) = db.withTransaction {
        val dao = db.caffeine()
        val existing = dao.dayIntakes(day.toString()).firstOrNull { it.slot == slot }
        if (existing == null) {
            dao.insert(CaffeineIntake(day = day.toString(), mg = mg, slot = slot))
        } else {
            dao.update(existing.copy(recorded = !existing.recorded,
                at = if (existing.recorded) existing.at else System.currentTimeMillis()))
        }
    }

    suspend fun editCaffeineDose(day: LocalDate, slot: Int, mg: Int, at: Long?) = db.withTransaction {
        val dao = db.caffeine()
        val existing = dao.dayIntakes(day.toString()).firstOrNull { it.slot == slot }
        if (existing == null) {
            dao.insert(CaffeineIntake(day = day.toString(), mg = mg, slot = slot, recorded = false))
        } else {
            dao.update(existing.copy(mg = mg, at = at ?: existing.at))
        }
    }

    suspend fun setCaffeineTarget(targetMg: Int) = db.withTransaction {
        val settings = caffeineSettingsNow()
        if (settings.targetMg != targetMg) db.caffeine().clearPlannedDoses()
        saveCaffeineSettings(settings.copy(targetMg = targetMg))
    }
}
