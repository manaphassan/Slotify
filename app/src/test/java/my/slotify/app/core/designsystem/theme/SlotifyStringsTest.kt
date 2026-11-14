package my.slotify.app.core.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every in-app string must be filled in for both languages (the single localisation system). */
class SlotifyStringsTest {

    /** All String fields, recursing into the per-feature groups (e.g. `logisticsUi.mapLoading`). */
    private fun textFields(table: Any, prefix: String = ""): Map<String, String> =
        table.javaClass.declaredFields
            .filterNot { it.isSynthetic || java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .flatMap { f ->
                f.isAccessible = true
                val value = f.get(table)
                when {
                    value is String -> listOf("$prefix${f.name}" to value)
                    value != null && value.javaClass.name.endsWith("Strings") ->
                        textFields(value, "$prefix${f.name}.").toList()

                    else -> emptyList()
                }
            }
            .toMap()

    @Test
    fun bmAndEn_haveNoBlankStrings() {
        listOf("BM" to BmStrings, "EN" to EnStrings).forEach { (lang, table) ->
            textFields(table).forEach { (name, value) ->
                assertTrue("$lang.$name is blank", value.isNotBlank())
            }
        }
    }

    @Test
    fun featureGroups_areIncluded() {
        val names = textFields(BmStrings).keys
        listOf(
            "logisticsUi.",
            "dashboardUi.",
            "profileUi.",
            "timetableUi.",
            "financeUi.",
            "shellUi.",
            "locationPickerUi."
        ).forEach { group ->
            assertTrue("no strings found in $group", names.any { it.startsWith(group) })
        }
        assertEquals(textFields(BmStrings).keys, textFields(EnStrings).keys)
    }

    @Test
    fun stringsFor_selectsTable() {
        assertEquals(EnStrings, stringsFor(AppLanguage.EN))
        assertEquals(BmStrings, stringsFor(AppLanguage.BM))
    }

    @Test
    fun formattedStrings_includeTheirArguments() {
        listOf(BmStrings, EnStrings).forEach { s ->
            assertTrue(s.pinAttemptsLeft(2).contains("2"))
            assertTrue(s.pinLockedOut(30).contains("30"))
            assertTrue(s.bagItemsProgress(3, 7).let { it.contains("3") && it.contains("7") })
            assertTrue(s.redeemNotEnoughStars("Ais Krim").contains("Ais Krim"))
            assertTrue(s.todayDaySuffix("Rabu").contains("Rabu"))
            assertTrue(s.dashboardUi.prayerCountdown(45).contains("45"))
            assertTrue(s.dashboardUi.prayerCountdown(125).let { it.contains("2") && it.contains("5") })
        }
    }

    @Test
    fun prayerCountdown_matchesLegacyBmFormat() {
        val c = BmStrings.dashboardUi.prayerCountdown
        assertEquals("Lagi 45 min", c(45))
        assertEquals("Lagi 2 jam", c(120))
        assertEquals("Lagi 2j 5m", c(125))
    }

    @Test
    fun attireGuide_isBilingual() {
        (1..5).forEach { day ->
            val bm = my.slotify.app.core.model.AttireGuide.label(day, AppLanguage.BM)
            val en = my.slotify.app.core.model.AttireGuide.label(day, AppLanguage.EN)
            assertTrue(bm.isNotBlank() && en.isNotBlank() && bm != en)
        }
        assertEquals("Isnin", my.slotify.app.core.model.AttireGuide.dayName(1))
    }
}
