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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    /**
     * Сколько дней длится дело (курс), 0 = без ограничений.
     * Считается от дня создания: курс на 30 дней, начатый 1-го, идёт по 30-е.
     */
    val durationDays: Int = NO_LIMIT,
)

/** Отметка о выполнении за конкретный день (день в формате ISO yyyy-MM-dd). */
@Entity(tableName = "records", primaryKeys = ["habitId", "day"])
data class Record(
    val habitId: Long,
    val day: String,
    val doneAt: Long = System.currentTimeMillis(),
)

/**
 * Приём кофеина: момент и доза в мг.
 * Храним именно мг, а не «номер таблетки»: норму можно менять, а прошлые приёмы
 * должны остаться такими, какими их отметили.
 */
@Entity(tableName = "caffeine_intakes")
data class CaffeineIntake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** День в формате ISO yyyy-MM-dd — по нему считаются суммы за день. */
    val day: String,
    /** Время приёма, мс от эпохи — для истории и порядка в дне. */
    val at: Long = System.currentTimeMillis(),
    val mg: Int,
)

/** Настройки трекера кофеина: выключен, пока пользователь его не включит. */
@Entity(tableName = "caffeine_settings")
data class CaffeineSettings(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    /** Дневная норма, мг. */
    val targetMg: Int = CaffeineLogic.DEFAULT_TARGET,
    /** Время сна в минутах от полуночи. */
    val bedtimeMinutes: Int = CaffeineLogic.DEFAULT_BEDTIME_MINUTES,
    /** Время подъёма в минутах от полуночи — от него считается расписание приёмов. */
    val wakeMinutes: Int = CaffeineLogic.DEFAULT_WAKE_MINUTES,
    /** Идущий таймер: момент окончания, мс от эпохи. null — таймер не запущен. */
    val timerEndMs: Long? = null,
    /** Какой приём обслуживает таймер (индекс), чтобы подписать «после 1-го приёма». */
    val timerSlot: Int? = null,
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

@Dao
interface CaffeineDao {
    @Query("SELECT * FROM caffeine_intakes ORDER BY at DESC")
    fun observeAll(): Flow<List<CaffeineIntake>>

    @Query("SELECT * FROM caffeine_intakes WHERE day = :day ORDER BY at")
    fun observeDay(day: String): Flow<List<CaffeineIntake>>

    @Insert
    suspend fun insert(intake: CaffeineIntake): Long

    @Query("DELETE FROM caffeine_intakes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM caffeine_settings WHERE id = 1")
    fun observeSettings(): Flow<CaffeineSettings?>

    @Query("SELECT * FROM caffeine_settings WHERE id = 1")
    suspend fun settings(): CaffeineSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: CaffeineSettings)
}

@Database(
    entities = [Habit::class, Record::class, CaffeineIntake::class, CaffeineSettings::class],
    version = 3,
    exportSchema = false,
)
abstract class RutinaDb : RoomDatabase() {
    abstract fun habits(): HabitDao
    abstract fun records(): RecordDao
    abstract fun caffeine(): CaffeineDao

    companion object {
        @Volatile
        private var instance: RutinaDb? = null

        /**
         * Версия 1 → 2: у дела появилась длительность курса.
         * Существующие дела становятся бессрочными — их поведение не меняется.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE habits ADD COLUMN durationDays INTEGER NOT NULL DEFAULT $NO_LIMIT",
                )
            }
        }

        /**
         * Версия 2 → 3: трекер кофеина. Прежние таблицы не трогаем — только добавляем
         * новые, поэтому дела и отметки остаются как были.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `caffeine_intakes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `day` TEXT NOT NULL,
                        `at` INTEGER NOT NULL,
                        `mg` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `caffeine_settings` (
                        `id` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `targetMg` INTEGER NOT NULL,
                        `bedtimeMinutes` INTEGER NOT NULL,
                        `wakeMinutes` INTEGER NOT NULL,
                        `timerEndMs` INTEGER,
                        `timerSlot` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): RutinaDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RutinaDb::class.java,
                    "rutina.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}
