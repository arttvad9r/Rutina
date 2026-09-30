package com.artt.rutina.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Всё, что нужно экранам: привычки + их отметки. */
data class Snapshot(
    val habits: List<Habit>,
    val records: Map<Long, Set<String>>,
)

class Repo(private val db: RutinaDb) {

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
}
