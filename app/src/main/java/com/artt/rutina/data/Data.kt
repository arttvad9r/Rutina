package com.artt.rutina.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Привычка / дело в рутине. */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** Час напоминания, -1 = без напоминания. */
    val hour: Int = -1,
    val minute: Int = 0,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Отметка о выполнении за конкретный день (день в формате ISO yyyy-MM-dd). */
@Entity(tableName = "records", primaryKeys = ["habitId", "day"])
data class Record(
    val habitId: Long,
    val day: String,
    val doneAt: Long = System.currentTimeMillis(),
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY (CASE WHEN hour < 0 THEN 24 ELSE hour END), minute, sortOrder, id")
    fun observeAll(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY id")
    suspend fun all(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun byId(id: Long): Habit?

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Delete
    suspend fun delete(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records WHERE day BETWEEN :from AND :to")
    fun observeRange(from: String, to: String): Flow<List<Record>>

    @Query("SELECT * FROM records")
    fun observeAll(): Flow<List<Record>>

    @Query("SELECT * FROM records WHERE habitId = :habitId ORDER BY day DESC")
    fun observeForHabit(habitId: Long): Flow<List<Record>>

    @Query("SELECT COUNT(*) FROM records WHERE habitId = :habitId AND day = :day")
    suspend fun isDone(habitId: Long, day: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun mark(record: Record)

    @Query("DELETE FROM records WHERE habitId = :habitId AND day = :day")
    suspend fun unmark(habitId: Long, day: String)

    @Query("DELETE FROM records WHERE habitId = :habitId")
    suspend fun forgetHabit(habitId: Long)
}

@Database(entities = [Habit::class, Record::class], version = 1, exportSchema = false)
abstract class RutinaDb : RoomDatabase() {
    abstract fun habits(): HabitDao
    abstract fun records(): RecordDao

    companion object {
        @Volatile
        private var instance: RutinaDb? = null

        fun get(context: Context): RutinaDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RutinaDb::class.java,
                    "rutina.db",
                ).build().also { instance = it }
            }
    }
}
