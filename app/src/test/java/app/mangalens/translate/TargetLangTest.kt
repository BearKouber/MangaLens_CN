package app.mangalens.translate

import app.mangalens.overlay.TypeSet
import app.mangalens.settings.AppSettings
import app.mangalens.settings.TargetLang
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Output languages other than English: line breaking, junk gating, cache isolation. */
class TargetLangTest {

    private val unit: (String) -> Float = { it.length.toFloat() }

    @Test
    fun chineseBreaksBetweenCharactersButKeepsPunctuationAttached() {
        val words = TypeSet.Words("「你这家伙！」竟然敢——")
        assertEquals(listOf("「你", "这", "家", "伙！」", "竟", "然", "敢——"), words.list)
        val lines = TypeSet.breakLines("你这家伙竟然敢这样对我说话！", unit, 6f)
        assertTrue(lines.size > 1)
        assertTrue(lines.all { it.length <= 6 })
        assertTrue(lines.none { it.startsWith("！") })
        assertEquals("你这家伙竟然敢这样对我说话！", lines.joinToString(""))
    }

    @Test
    fun spacedScriptsStillBreakAtSpacesOnly() {
        assertEquals(listOf("You", "bastard!"), TypeSet.Words("You  bastard!").list)
        assertEquals(listOf("괜찮아.", "내가"), TypeSet.Words("괜찮아. 내가").list)
        // Mixed text keeps its spaces where it had them.
        assertEquals("OK 了吗", TypeSet.Words("OK 了吗").line(0, 3))
    }

    @Test
    fun junkFilterAcceptsCjkOutputForCjkTargets() {
        assertEquals("没关系，我会保护你的。",
            JunkFilter.accept("괜찮아. 내가 지켜줄게.", "没关系，我会保护你的。", TargetLang.ZH_HANS))
        assertEquals("好", JunkFilter.accept("좋아", "好", TargetLang.ZH_HANS))
        // English target keeps rejecting CJK echoes.
        assertNull(JunkFilter.accept("괜찮아. 내가 지켜줄게.", "괜찮아. 내가 지켜줄게.", TargetLang.EN))
    }

    @Test
    fun junkFilterRejectsEchoesForCjkTargets() {
        assertNull(JunkFilter.accept("你好吗", "你好吗", TargetLang.ZH_HANS))
        // Kana left in a Chinese answer is untranslated Japanese.
        assertNull(JunkFilter.accept("ありがとう", "ありがとう", TargetLang.ZH_HANS))
        assertNull(JunkFilter.accept("괜찮아", "괜찮아요", TargetLang.JA))
        assertEquals("ありがとう", JunkFilter.accept("谢谢", "ありがとう", TargetLang.JA))
    }

    @Test
    fun cacheNamespacesDifferByTarget() {
        val en = AppSettings(apiKey = "k", targetLang = TargetLang.EN)
        val zh = en.copy(targetLang = TargetLang.ZH_HANS)
        assertNotEquals(LlmEngine(en).cacheNamespace, LlmEngine(zh).cacheNamespace)
    }

    @Test
    fun englishOnlyLeavesJapaneseAndKoreanAlone() {
        assertEquals("OKだよ", PageReader.englishOnly("OKだよ", TargetLang.JA))
        assertEquals("OK", PageReader.englishOnly("OKっ", TargetLang.EN))
    }

    @Test
    fun promptMovesOutputLanguageOnlyWhenNotEnglish() {
        assertEquals(LlmEngine.SYSTEM_PROMPT, LlmEngine.systemPrompt(LlmEngine.SYSTEM_PROMPT, TargetLang.EN))
        val zh = LlmEngine.systemPrompt(LlmEngine.SYSTEM_PROMPT, TargetLang.ZH_HANS)
        assertTrue(zh.contains("already Simplified Chinese"))
        assertTrue(zh.contains("OUTPUT LANGUAGE"))
    }

    @Test
    fun defaultTargetFollowsPhoneLanguage() {
        assertEquals(TargetLang.ZH_HANS, TargetLang.forLocale(Locale.SIMPLIFIED_CHINESE))
        assertEquals(TargetLang.ZH_HANT, TargetLang.forLocale(Locale.TRADITIONAL_CHINESE))
        assertEquals(TargetLang.ZH_HANT, TargetLang.forLocale(Locale.forLanguageTag("zh-Hant-HK")))
        assertEquals(TargetLang.EN, TargetLang.forLocale(Locale.GERMANY))
    }
}
