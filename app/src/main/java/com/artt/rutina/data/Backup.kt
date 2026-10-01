package com.artt.rutina.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate

/** Формат переноса независим от версии SQLite. Временные таймеры не переносятся. */
data class BackupData(
    val habits: List<Habit>,
    val records: List<Record>,
    val intakes: List<CaffeineIntake>,
    val settings: CaffeineSettings,
    val exportedAt: Long = System.currentTimeMillis(),
) {
    fun validate() {
        try {
            val ids = habits.map { it.id }.toSet()
            require(ids.size == habits.size && ids.all { it in 1..Int.MAX_VALUE.toLong() })
            habits.forEach {
                require(it.name.isNotBlank() && it.name.length <= 10_000)
                require(it.hour in -1..23 && it.minute in 0..59 && it.sortOrder >= 0)
                require(it.durationDays in 0..3650 && validTime(it.createdAt))
                require(it.finishedAt == null || (validTime(it.finishedAt) && it.finishedAt >= it.createdAt))
            }
            require(records.map { it.habitId to it.day }.toSet().size == records.size)
            records.forEach { require(it.habitId in ids && validDay(it.day) && validTime(it.doneAt)) }
            require(intakes.map { it.id }.toSet().size == intakes.size && intakes.all { it.id > 0 })
            intakes.groupBy { it.day }.values.forEach { day ->
                val slots = day.mapNotNull { it.slot }
                require(slots.toSet().size == slots.size)
                require(slots.all { it in 0..maxOf(2, day.size - 1) })
            }
            intakes.forEach {
                require(validDay(it.day) && validTime(it.at))
                require(it.mg in 50..CaffeineLogic.MAX_INTAKE_MG && it.mg % 50 == 0)
            }
            require(settings.id == 1 && settings.targetMg in CaffeineLogic.MIN_TARGET..CaffeineLogic.MAX_TARGET && settings.targetMg % 50 == 0)
            require(settings.wakeMinutes in 0..1439 && settings.bedtimeMinutes in 0..1439)
            require(validTime(exportedAt))
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Файл содержит некорректные данные. Текущие данные не изменены.", e)
        }
    }

    fun json(): String {
        validate()
        return JSONObject().put("format", "rutina-backup").put("version", 1).put("exportedAt", exportedAt)
            .put("habits", JSONArray().apply { habits.forEach { h -> put(JSONObject()
                .put("id", h.id).put("name", h.name).put("hour", h.hour).put("minute", h.minute)
                .put("active", h.active).put("sortOrder", h.sortOrder).put("createdAt", h.createdAt)
                .put("durationDays", h.durationDays).put("finishedAt", h.finishedAt ?: JSONObject.NULL)) } })
            .put("records", JSONArray().apply { records.forEach { r -> put(JSONObject()
                .put("habitId", r.habitId).put("day", r.day).put("doneAt", r.doneAt)) } })
            .put("caffeineIntakes", JSONArray().apply { intakes.forEach { i -> put(JSONObject()
                .put("id", i.id).put("day", i.day).put("at", i.at).put("mg", i.mg)
                .put("slot", i.slot ?: JSONObject.NULL).put("recorded", i.recorded)) } })
            .put("caffeineSettings", JSONObject().put("enabled", settings.enabled).put("targetMg", settings.targetMg)
                .put("wakeMinutes", settings.wakeMinutes).put("bedtimeMinutes", settings.bedtimeMinutes)).toString(2)
    }

    companion object {
        private const val MAX_BYTES = 10 * 1024 * 1024
        fun read(input: InputStream): BackupData {
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(bytes.size() + count <= MAX_BYTES) { "Файл слишком большой: максимум 10 МБ." }
                bytes.write(buffer, 0, count)
            }
            return parse(bytes.toString("UTF-8"))
        }

        fun parse(text: String): BackupData {
            val root = JSONObject(text)
            require(root.getString("format") == "rutina-backup") { "Это не резервная копия Рутины." }
            require(root.get("version") == 1) { "Версия файла не поддерживается. Обновите приложение." }
            val hs = root.getJSONArray("habits")
            val rs = root.getJSONArray("records")
            val ins = root.getJSONArray("caffeineIntakes")
            val s = root.getJSONObject("caffeineSettings")
            return BackupData(
                List(hs.length()) { index -> hs.getJSONObject(index).let { h -> Habit(
                    id = h.getLong("id"), name = h.getString("name"), hour = h.getInt("hour"), minute = h.getInt("minute"),
                    active = h.getBoolean("active"), sortOrder = h.getInt("sortOrder"), createdAt = h.getLong("createdAt"),
                    durationDays = h.getInt("durationDays"), finishedAt = if (h.isNull("finishedAt")) null else h.getLong("finishedAt")) } },
                List(rs.length()) { index -> rs.getJSONObject(index).let { r -> Record(r.getLong("habitId"), r.getString("day"), r.getLong("doneAt")) } },
                List(ins.length()) { index -> ins.getJSONObject(index).let { i -> CaffeineIntake(
                    id = i.getLong("id"), day = i.getString("day"), at = i.getLong("at"), mg = i.getInt("mg"),
                    slot = if (i.isNull("slot")) null else i.getInt("slot"), recorded = i.getBoolean("recorded")) } },
                CaffeineSettings(enabled = s.getBoolean("enabled"), targetMg = s.getInt("targetMg"),
                    wakeMinutes = s.getInt("wakeMinutes"), bedtimeMinutes = s.getInt("bedtimeMinutes")),
                root.getLong("exportedAt"),
            ).also { it.validate() }
        }

        private fun validTime(time: Long) = time in 0..253402300799999L
        private fun validDay(day: String) = runCatching {
            val date = LocalDate.parse(day)
            date.year in 1970..9999 && date.toString() == day
        }.getOrDefault(false)
    }
}
