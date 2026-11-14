package my.slotify.app.core.designsystem.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/** Every in-app string must be filled in for both languages (the single localisation system). */
class SlotifyStringsTest {

    private fun textFields(strings: AppStrings): Map<String, String> =
        AppStrings::class.java.declaredFields
            .filter { it.type == String::class.java }
            .associate { f -> f.isAccessible = true; f.name to (f.get(strings) as String) }

    @Test
    fun bmAndEn_haveNoBlankStrings() {
        listOf("BM" to BmStrings, "EN" to EnStrings).forEach { (lang, table) ->
            textFields(table).forEach { (name, value) ->
                assertTrue("$lang.$name is blank", value.isNotBlank())
            }
        }
    }

    @Test
    fun formattedStrings_includeTheirArguments() {
        listOf(BmStrings, EnStrings).forEach { s ->
            assertTrue(s.pinAttemptsLeft(2).contains("2"))
            assertTrue(s.pinLockedOut(30).contains("30"))
            assertTrue(s.bagItemsProgress(3, 7).let { it.contains("3") && it.contains("7") })
            assertTrue(s.redeemNotEnoughStars("Ais Krim").contains("Ais Krim"))
        }
    }
}
