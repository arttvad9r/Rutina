package com.artt.rutina.data

import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {
    private val sample = BackupData(
        listOf(Habit(id = 1, name = "Чтение", createdAt = 1000)),
        listOf(Record(1, "2026-10-01", 2000)),
        listOf(CaffeineIntake(id = 1, day = "2026-10-01", at = 2000, mg = 100, slot = 2)),
        CaffeineSettings(), exportedAt = 3000)

    @Test fun acceptsCompleteAndEmptyCopies() {
        sample.validate()
        BackupData(emptyList(), emptyList(), emptyList(), CaffeineSettings()).validate()
    }
    @Test fun rejectsOrphanRecords() {
        assertThrows(IllegalArgumentException::class.java) { sample.copy(records = listOf(Record(2, "2026-10-01"))).validate() }
    }
    @Test fun rejectsDuplicateHabitIds() {
        assertThrows(IllegalArgumentException::class.java) { sample.copy(habits = sample.habits + sample.habits).validate() }
    }
    @Test fun rejectsDuplicateDays() {
        assertThrows(IllegalArgumentException::class.java) { sample.copy(records = sample.records + sample.records).validate() }
    }
    @Test fun rejectsInvalidDates() {
        assertThrows(IllegalArgumentException::class.java) { sample.copy(records = listOf(Record(1, "2026-02-30"))).validate() }
    }
    @Test fun rejectsDuplicateSlotsAndHugeSlots() {
        assertThrows(IllegalArgumentException::class.java) { sample.copy(intakes = sample.intakes + sample.intakes[0].copy(id = 2)).validate() }
        assertThrows(IllegalArgumentException::class.java) { sample.copy(intakes = listOf(sample.intakes[0].copy(slot = Int.MAX_VALUE))).validate() }
    }
    @Test fun rejectsInvalidDoseAndSettings() {
        assertThrows(IllegalArgumentException::class.java) { sample.copy(intakes = listOf(sample.intakes[0].copy(mg = 75))).validate() }
        assertThrows(IllegalArgumentException::class.java) { sample.copy(settings = CaffeineSettings(targetMg = 75)).validate() }
        assertThrows(IllegalArgumentException::class.java) { sample.copy(settings = CaffeineSettings(wakeMinutes = 1440)).validate() }
    }
    @Test fun rejectsBrokenReminderAndCourse() {
        assertThrows(IllegalArgumentException::class.java) { sample.copy(habits = listOf(sample.habits[0].copy(hour = 24))).validate() }
        assertThrows(IllegalArgumentException::class.java) { sample.copy(habits = listOf(sample.habits[0].copy(durationDays = -1))).validate() }
    }
}
