package my.slotify.app.core.database

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object IcsParser {

    fun parse(inputStream: InputStream, sessionId: String): List<TimetableSlotEntity> {
        val lines = inputStream.bufferedReader().readLines()
        val slots = mutableListOf<TimetableSlotEntity>()

        var inEvent = false
        var summary = ""
        var startTime = ""
        var endTime = ""
        var dayOfWeek: Int? = null

        val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

        for (line in lines) {
            when {
                line.startsWith("BEGIN:VEVENT") -> {
                    inEvent = true
                    summary = ""
                    startTime = ""
                    endTime = ""
                    dayOfWeek = null
                }

                line.startsWith("SUMMARY:") && inEvent -> {
                    summary = line.removePrefix("SUMMARY:")
                }

                line.startsWith("DTSTART") && inEvent -> {
                    val dateStr = line.substringAfter(":")
                    try {
                        val date = dateFormat.parse(dateStr)
                        if (date != null) {
                            val cal =
                                Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur")).apply { time = date }
                            val h = cal.get(Calendar.HOUR_OF_DAY)
                            val m = cal.get(Calendar.MINUTE)
                            startTime = String.format(Locale.US, "%02d:%02d", h, m)

                            // Map Calendar day to our dayOfWeek (1=Isnin..5=Jumaat); weekend events are skipped
                            dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.MONDAY -> 1
                                Calendar.TUESDAY -> 2
                                Calendar.WEDNESDAY -> 3
                                Calendar.THURSDAY -> 4
                                Calendar.FRIDAY -> 5
                                else -> null
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                line.startsWith("DTEND") && inEvent -> {
                    val dateStr = line.substringAfter(":")
                    try {
                        val date = dateFormat.parse(dateStr)
                        if (date != null) {
                            val cal =
                                Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur")).apply { time = date }
                            val h = cal.get(Calendar.HOUR_OF_DAY)
                            val m = cal.get(Calendar.MINUTE)
                            endTime = String.format(Locale.US, "%02d:%02d", h, m)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                line.startsWith("END:VEVENT") -> {
                    inEvent = false
                    // Filter out exam schedules that don't repeat, or just add everything
                    // In this context, we just want the weekly recurring classes if possible,
                    // but for now we'll ingest all subjects that have times.
                    val day = dayOfWeek
                    if (summary.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty() && day != null) {
                        val code = mapSummaryToSubjectCode(summary)
                        slots.add(
                            TimetableSlotEntity(
                                id = UUID.randomUUID().toString(),
                                sessionId = sessionId,
                                dayOfWeek = day,
                                periodIndex = 0, // temporary, will re-index below
                                startTime = startTime,
                                endTime = endTime,
                                subjectCode = code,
                                subjectName = summary,
                                teacherName = null,
                                isRehat = code == "REHAT",
                                requiresTextbook = code != "REHAT" && code != "UNKNOWN",
                                requiresWorkbook = code != "REHAT" && code != "UNKNOWN"
                            )
                        )
                    }
                }
            }
        }

        // Re-index periods per day based on start time and remove duplicates (since ICS might have multiple weeks of same events)
        return slots.distinctBy { "${it.dayOfWeek}-${it.startTime}-${it.subjectCode}" }
            .groupBy { it.dayOfWeek }
            .flatMap { (_, daySlots) ->
                daySlots.sortedBy { it.startTime }.mapIndexed { index, slot ->
                    slot.copy(periodIndex = index + 1)
                }
            }
    }

    private fun mapSummaryToSubjectCode(summary: String): String {
        val s = summary.uppercase()
        return when {
            s.contains("REHAT") -> "REHAT"
            s.contains("MELAYU") -> "BM"
            s.contains("INGGERIS") || s.contains("ENGLISH") -> "BI"
            s.contains("MATEMATIK") -> "MT"
            s.contains("SAINS") -> "SN"
            s.contains("PEND. ISLAM") || s.contains("JAWI") || s.contains("AQ") || s.contains("ULUM") -> "PI"
            s.contains("ARAB") -> "BA"
            s.contains("SEJARAH") -> "SEJ"
            s.contains("RBT") -> "RBT"
            s.contains("MUZIK") -> "MZ"
            s.contains("SENI") || s.contains("PSV") -> "PSV"
            s.contains("JASMANI") || s.contains("KESIHATAN") || s.contains("PJPK") -> "PJ"
            s.contains("PERHIMPUNAN") -> "PERHIMPUNAN"
            s.contains("TASMIK") -> "TASMIK"
            else -> "UNKNOWN"
        }
    }
}
