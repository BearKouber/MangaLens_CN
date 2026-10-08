package app.mangalens

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class TrTest {

    private val saved = Locale.getDefault()

    @After
    fun restore() = Locale.setDefault(saved)

    @Test
    fun englishPhoneKeepsTheEnglish() {
        Locale.setDefault(Locale.US)
        assertEquals("Tweaks", tr("Tweaks"))
        assertEquals("Open Brave ↗", tr("Open %s ↗", "Brave"))
    }

    @Test
    fun chinesePhoneGetsChineseAndFillsTemplates() {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        assertEquals("设置", tr("Tweaks"))
        assertEquals("打开 Brave ↗", tr("Open %s ↗", "Brave"))
        assertEquals("已完成 1/2", tr("%d of %d done", 1, 2))
        // A line missing from the table shows in English rather than blank.
        assertEquals("HTTP 503", tr("HTTP 503"))
    }
}
