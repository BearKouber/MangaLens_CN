package app.mangalens.translate

import app.mangalens.ocr.Script
import app.mangalens.settings.TargetLang

/**
 * Rejects translations that would read as garbage on the page: empty results
 * and source echoed back untranslated. Better an untouched bubble than junk.
 *
 * Romanized kana is not gated. The model is told to skip what it cannot
 * read rather than romanize it, and a shouted name (カナタ! -> "Kanata!") is
 * legitimate romaji that a romanization check would throw away.
 */
object JunkFilter {

    /** Returns null when the translation should not be rendered at all. */
    fun accept(source: String, translated: String, target: TargetLang = TargetLang.EN): String? {
        val out = translated.trim()
        if (out.isEmpty()) return null
        // Untranslated echo: painting the original text over itself helps nobody.
        if (isEcho(source, out, target)) return null
        // The Latin flavour of the same failure — a Spanish source coming
        // back as the same Spanish. Short identical answers are left alone:
        // "No!" translates to "No!" legitimately.
        if (Script.cjkCount(source) == 0) {
            val a = source.lowercase().filter { it.isLetterOrDigit() }
            if (a.length >= 12 && a == out.lowercase().filter { it.isLetterOrDigit() }) return null
        }
        // One Han character is a whole reply ("好"); one Latin letter is not.
        val letters = out.count { it.isLetter() }
        if (letters < if (target.cjk) 1 else 2) return null
        return out
    }

    /**
     * True when [out] is [source] handed back untranslated. For a Latin or
     * Cyrillic target any CJK-heavy answer is an echo; for a CJK target the
     * output is CJK by design, so the echo is the same text, or a script
     * the target does not write in (kana left in Chinese, Hangul in Japanese).
     */
    fun isEcho(source: String, out: String, target: TargetLang): Boolean {
        if (!target.cjk) return Script.cjkCount(out) > out.length * 0.4f
        val squash = { s: String -> s.filter { it.isLetterOrDigit() } }
        if (squash(out).isNotEmpty() && squash(out) == squash(source)) return true
        if (target != TargetLang.JA && Script.kanaCount(out) > out.length * 0.3f) return true
        if (target != TargetLang.KO && Script.hangulCount(out) > out.length * 0.3f) return true
        return false
    }
}
