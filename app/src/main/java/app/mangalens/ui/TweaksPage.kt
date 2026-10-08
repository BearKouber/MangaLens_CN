package app.mangalens.ui

import app.mangalens.tr
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mangalens.scroll.ScrollPace
import app.mangalens.gaps.ShadeLevel
import app.mangalens.settings.AiReasoning
import app.mangalens.settings.AiVisionMode
import app.mangalens.settings.AppSettings
import app.mangalens.settings.CaptureMode
import app.mangalens.settings.LlmProvider
import app.mangalens.settings.SourceLang
import app.mangalens.settings.TargetLang
import app.mangalens.translate.LlmHttp
import app.mangalens.translate.ModelCatalog
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Where Tweaks opens: at the top, at the Your AI card (a missing or refused
 * key, a server to set up), at the other providers, unfolded under More
 * options, or at Auto-scroll (its switch in Accessibility is off).
 */
internal enum class TweaksTarget { TOP, AI, OTHER_AI, SCROLL }

/**
 * Tweaks, kept to what a reader actually changes: the language, hands-free
 * or tap, auto-scroll and how it goes, the size of the lettering and
 * whether the original may show through it, and whether the AI's key
 * works. That is all the page shows. Everything else —
 * timing, data, the thinking setting, the model, other AI providers,
 * diagnostics — has defaults that are right for nearly everyone, and waits
 * folded under More options, where it can be found without being in the
 * way.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TweaksPage(
    settings: AppSettings,
    sink: SettingsSink,
    drafts: AiDrafts,
    sayHi: SayHi,
    target: TweaksTarget,
    versionName: String,
    solidOffered: Boolean,
    solidLettering: Boolean,
    ghostsOffered: Boolean,
    onTurnOnSolid: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onClose: () -> Unit,
    autoScrollOn: Boolean,
    onTurnOnAutoScroll: () -> Unit,
) {
    val pop = LocalPop.current
    var moreOpen by rememberSaveable { mutableStateOf(target == TweaksTarget.OTHER_AI) }
    val scrollRequester = remember { BringIntoViewRequester() }
    val aiRequester = remember { BringIntoViewRequester() }
    val otherAiRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(target) {
        when (target) {
            TweaksTarget.TOP -> Unit
            TweaksTarget.AI -> {
                delay(250)
                aiRequester.bringIntoView()
            }
            TweaksTarget.OTHER_AI -> {
                moreOpen = true
                delay(250)
                otherAiRequester.bringIntoView()
            }
            TweaksTarget.SCROLL -> {
                delay(250)
                scrollRequester.bringIntoView()
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val narrow = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tr("Tweaks"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = pop.ink,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = tr("Close tweaks"), tint = pop.ink)
                    }
                }
                Text(
                    tr("The defaults are good. Most people never need more than this."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = pop.inkSoft,
                )

                ReadingSection(settings, sink, columns = if (narrow) 2 else 4)
                AutoScrollSection(
                    settings, sink, autoScrollOn, onTurnOnAutoScroll, onOpenAppInfo,
                    stacked = narrow, modifier = Modifier.bringIntoViewRequester(scrollRequester),
                )
                TextSizeSection(settings, sink)
                if (solidOffered) SolidLetteringRow(solidLettering, onTurnOnSolid, onOpenAppInfo, stacked = narrow)
                if (ghostsOffered && !solidLettering) NoGhostsRow(settings, sink)
                YourAiSection(drafts, sayHi, Modifier.bringIntoViewRequester(aiRequester))

                Spacer(Modifier.height(28.dp))
                MoreOptions(
                    settings, sink, drafts,
                    open = moreOpen,
                    onToggle = { moreOpen = !moreOpen },
                    otherAi = Modifier.bringIntoViewRequester(otherAiRequester),
                )

                Spacer(Modifier.height(32.dp))
                Text(
                    listOf("MangaLens", versionName).filter { it.isNotEmpty() }.joinToString(" ") +
                        tr(" · Loving a raw? Support the official release when it exists."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = pop.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = LocalPop.current.inkSoft,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(8.dp))
}

/** A heading inside More options: smaller than a section, to keep the fold one piece. */
@Composable
private fun GroupTitle(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = LocalPop.current.ink,
        modifier = Modifier.semantics { heading() },
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Helper(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = LocalPop.current.inkSoft, modifier = modifier)
}

@Composable
private fun ReadingSection(settings: AppSettings, sink: SettingsSink, columns: Int) {
    val pop = LocalPop.current
    SectionTitle(tr("Reading"))
    Text(tr("Language on the page"), style = MaterialTheme.typography.titleMedium, color = pop.ink)
    Spacer(Modifier.height(8.dp))
    PopTiles(
        listOf(
            PopTile(SourceLang.AUTO, tr("Auto"), glyph = "★"),
            PopTile(SourceLang.KO, tr("Korean"), glyph = "한"),
            PopTile(SourceLang.JA, tr("Japanese"), glyph = "日"),
            PopTile(SourceLang.ZH, tr("Chinese"), glyph = "中"),
        ),
        selected = settings.sourceLang,
        onSelect = sink::setSourceLang,
        columns = columns,
    )
    Spacer(Modifier.height(8.dp))
    Helper(
        when (settings.sourceLang) {
            SourceLang.AUTO -> tr("I spot the language on every page.")
            SourceLang.KO -> tr("Reading Korean (manhwa).")
            SourceLang.JA -> tr("Reading Japanese (manga).")
            SourceLang.ZH -> tr("Reading Chinese (manhua).")
        }
    )
    Spacer(Modifier.height(12.dp))
    Text(tr("Translate into"), style = MaterialTheme.typography.titleMedium, color = pop.ink)
    Spacer(Modifier.height(8.dp))
    PopTiles(
        listOf(
            PopTile(TargetLang.EN, tr("English"), glyph = "A"),
            PopTile(TargetLang.ZH_HANS, "简体中文", glyph = "简"),
            PopTile(TargetLang.ZH_HANT, "繁體中文", glyph = "繁"),
            PopTile(TargetLang.JA, "日本語", glyph = "あ"),
            PopTile(TargetLang.KO, "한국어", glyph = "가"),
            PopTile(TargetLang.RU, "Русский", glyph = "Я"),
        ),
        selected = settings.targetLang,
        onSelect = sink::setTargetLang,
        columns = columns,
        leadRow = 2,
    )
    Spacer(Modifier.height(12.dp))
    val handsFree = settings.mode == CaptureMode.AUTO
    PopToggleRow(
        tr("Hands-free"),
        if (handsFree) tr("I translate each page as soon as you stop scrolling.")
        else tr("I wait until you tap the $MARK bubble. Saves data."),
        handsFree,
        { sink.setMode(if (it) CaptureMode.AUTO else CaptureMode.MANUAL) },
    )
    Spacer(Modifier.height(12.dp))
    PopToggleRow(
        tr("Dark gaps"),
        if (settings.darkGaps) tr("The white between a manhwa's panels is painted dark. The art and the lettering are left exactly as drawn. Dark and Black need Solid lettering; without it Android keeps every overlay a little see-through.")
        else tr("Paint the white between a manhwa's panels dark, for night reading. Also in the $MARK button's long-press menu."),
        settings.darkGaps,
        sink::setDarkGaps,
    )
    if (settings.darkGaps) {
        Spacer(Modifier.height(8.dp))
        PopTiles(
            listOf(
                PopTile(ShadeLevel.DIM, tr("Dim"), glyph = "◐"),
                PopTile(ShadeLevel.DARK, tr("Dark"), glyph = "◑"),
                PopTile(ShadeLevel.BLACK, tr("Black"), glyph = "●"),
            ),
            selected = settings.gapShade,
            onSelect = sink::setGapShade,
            columns = columns,
        )
    }
}

/**
 * Auto-scroll: whether its switch in Accessibility is on — only an
 * accessibility service may move another app's page — and when it is not,
 * the way there, with App info a tap away for a switch Android has greyed
 * out; then how fast it goes, whether it slows down for balloons, and
 * whether its button sits beside 文A.
 */
@Composable
private fun AutoScrollSection(
    settings: AppSettings,
    sink: SettingsSink,
    on: Boolean,
    onTurnOn: () -> Unit,
    onOpenAppInfo: () -> Unit,
    stacked: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionTitle(tr("Auto-scroll"))
        val pop = LocalPop.current
        PopSurface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = pop.surface) {
            val words: @Composable (Modifier) -> Unit = { m ->
                Column(m) {
                    Text(
                        if (on) tr("Auto-scroll · ready ✓") else tr("Auto-scroll · one switch to go"),
                        style = MaterialTheme.typography.titleMedium,
                        color = pop.ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    // With Scroll button off there is no ▼ to tap: the words
                    // point to the long-press, as that row below does.
                    Helper(
                        when {
                            !on -> tr("Only an Accessibility switch lets me move another app's page. Switch on \"MangaLens auto-scroll\".")
                            settings.autoScrollButton -> tr("Tap ▼ beside the $MARK bubble and I scroll the page for you, in any app.")
                            else -> tr("Long-press the $MARK bubble → Auto-scroll the page, in any app.")
                        }
                    )
                }
            }
            val button: @Composable (Modifier) -> Unit = { m ->
                StickerButton(
                    tr("Turn on ↗"), onTurnOn, modifier = m, style = StickerStyle.Surface, height = 48.dp,
                    fillWidth = stacked, contentDescription = tr("Turn on auto-scroll in Accessibility"),
                )
            }
            when {
                on -> words(Modifier.fillMaxWidth().padding(16.dp))
                stacked -> Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    words(Modifier)
                    Spacer(Modifier.height(12.dp))
                    button(Modifier)
                }
                else -> Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    words(Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    button(Modifier)
                }
            }
        }
        if (!on && Build.VERSION.SDK_INT >= 33) {
            Spacer(Modifier.height(4.dp))
            TextLink(tr("Auto-scroll greyed out? Allow it in App info ↗"), onOpenAppInfo)
        }
        Spacer(Modifier.height(16.dp))
        PopSlider(
            tr("Speed"),
            settings.scrollLevel.toFloat(),
            ScrollPace.MIN_LEVEL.toFloat()..ScrollPace.MAX_LEVEL.toFloat(),
            { "${it.roundToInt()}" },
            { sink.setScrollLevel(it.roundToInt()) },
            startLabel = "slow",
            endLabel = "fast",
        )
        Spacer(Modifier.height(12.dp))
        PopToggleRow(
            tr("Slow down for big balloons"),
            if (settings.smartScroll) tr("I watch the page and slow right down while a big balloon goes by, and hurry through empty gaps.")
            else tr("One steady speed, whatever is on the page."),
            settings.smartScroll,
            sink::setSmartScroll,
        )
        Spacer(Modifier.height(12.dp))
        PopToggleRow(
            tr("Scroll button"),
            if (settings.autoScrollButton) tr("▼ sits beside the $MARK bubble. Touch the screen to pause the scroll.")
            else tr("Hidden. Long-press the $MARK bubble for Auto-scroll instead."),
            settings.autoScrollButton,
            sink::setAutoScrollButton,
        )
        Spacer(Modifier.height(6.dp))
        Helper(
            tr("Translation napping: the page glides without stopping. Translation awake: I stop at each page, translate it, and give you time to read before I move on.")
        )
    }
}

@Composable
private fun TextSizeSection(settings: AppSettings, sink: SettingsSink) {
    val pop = LocalPop.current
    SectionTitle(tr("Look"))
    PopSlider(
        tr("Text size"),
        settings.textScale,
        0.8f..1.5f,
        { "${(it * 100).toInt()}%" },
        sink::setTextScale,
        below = { v ->
            SpeechBalloon(pop.surface, tail = Tail.Start, tailAt = 0.5f) {
                Text(
                    tr("Hey! Over here!"),
                    fontFamily = ComicNeue,
                    fontWeight = FontWeight.Bold,
                    fontSize = (16 * v).sp,
                    lineHeight = (20 * v).sp,
                    color = pop.ink,
                )
            }
        },
    )
    Spacer(Modifier.height(6.dp))
    Helper(tr("How big I letter the English on the page."))
}

/**
 * Under Look: whether the page is veiled so nothing of the original shows
 * through the English, at the price of a page a fifth darker while
 * MangaLens is awake. Android draws the lettering at no more than 80%.
 */
@Composable
private fun NoGhostsRow(settings: AppSettings, sink: SettingsSink) {
    Spacer(Modifier.height(16.dp))
    PopToggleRow(
        tr("No ghosts"),
        if (settings.noGhosts) tr("Nothing of the original shows under my English. The page is a little darker while I'm awake.")
        else tr("Full brightness, but the original shows faintly under my English."),
        settings.noGhosts,
        sink::setNoGhosts,
    )
}

/**
 * Under Look: whether the lettering is solid, and when it is not, the way
 * to switch it on — the same trip to Accessibility as the optional setup
 * step, with App info a tap away for a switch Android has greyed out.
 * [stacked] puts the button under the words, for a narrow screen or large
 * text.
 */
@Composable
private fun SolidLetteringRow(on: Boolean, onTurnOn: () -> Unit, onOpenAppInfo: () -> Unit, stacked: Boolean) {
    val pop = LocalPop.current
    Spacer(Modifier.height(16.dp))
    PopSurface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = pop.surface) {
        val words: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                Text(
                    if (on) tr("Solid lettering · on ✓") else tr("Solid lettering · off"),
                    style = MaterialTheme.typography.titleMedium,
                    color = pop.ink,
                )
                Spacer(Modifier.height(2.dp))
                Helper(
                    if (on) tr("Nothing of the original shows through my English.")
                    else tr("The original shows faintly under my English. Switch MangaLens on in Accessibility.")
                )
            }
        }
        val button: @Composable (Modifier) -> Unit = { m ->
            StickerButton(
                tr("Turn on ↗"), onTurnOn, modifier = m, style = StickerStyle.Surface, height = 48.dp,
                fillWidth = stacked, contentDescription = tr("Turn on solid lettering in Accessibility"),
            )
        }
        when {
            on -> words(Modifier.fillMaxWidth().padding(16.dp))
            stacked -> Column(Modifier.fillMaxWidth().padding(16.dp)) {
                words(Modifier)
                Spacer(Modifier.height(12.dp))
                button(Modifier)
            }
            else -> Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                words(Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                button(Modifier)
            }
        }
    }
    if (!on && Build.VERSION.SDK_INT >= 33) {
        Spacer(Modifier.height(4.dp))
        TextLink(tr("Greyed out? Allow it in App info ↗"), onOpenAppInfo)
    }
}

/**
 * "Gemini · key saved ✓", or what is still missing, as the Your AI card
 * and the home screen say it. [refused] is whether the provider turned
 * away the key held now: setup then asks for a new key, and a line still
 * ticking it off would contradict it.
 */
internal fun aiBrainSummary(s: AppSettings, refused: Boolean = false): Pair<String, Boolean> {
    val label = LlmHttp.providerLabel(s)
    val custom = s.provider == LlmProvider.CUSTOM
    return when {
        LlmHttp.setupNeeded(s) != null -> (if (custom) tr("%s · no server URL yet", label) else tr("%s · no key yet", label)) to false
        refused -> (if (custom) tr("%s · token refused", label) else tr("%s · key refused", label)) to false
        else -> (if (custom) tr("%s · server set ✓", label) else tr("%s · key saved ✓", label)) to true
    }
}

/**
 * The AI, reduced to what a reader needs to know: is the key in, and does
 * it work. A saved key shows as a line with Test it and Change key; the
 * key box itself appears only when there is no key yet, the key was
 * refused, or the reader asked to change it. A custom server's URL is
 * asked for here too, since nothing works without it.
 */
@Composable
private fun YourAiSection(drafts: AiDrafts, sayHi: SayHi, modifier: Modifier) {
    val pop = LocalPop.current
    val uri = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val view = LocalView.current
    val reduced = LocalReducedMotion.current
    val scope = rememberCoroutineScope()
    val pasteWiggle = rememberWiggle()
    val s = drafts.settings
    val provider = s.provider
    val (summary, ready) = aiBrainSummary(s, sayHi.rejects(s))
    var changing by remember(provider) { mutableStateOf(false) }
    var notice by remember(provider) { mutableStateOf<String?>(null) }
    val phase = sayHi.phase
    LaunchedEffect(phase) {
        when {
            // A refused key can only be fixed by a new one: open the box and point at Paste.
            phase is SayHi.Phase.Failed && phase.rejected -> {
                changing = true
                view.buzz(Buzz.REJECT)
                pasteWiggle.play(reduced)
            }
            phase is SayHi.Phase.Ok -> changing = false
        }
    }

    SectionTitle(tr("Your AI"))
    PopSurface(modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = pop.surface) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                summary,
                style = MaterialTheme.typography.titleMedium,
                color = if (ready) pop.ink else pop.punchText,
            )
            if (provider == LlmProvider.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                SecretField(
                    value = drafts.customUrl,
                    onValueChange = drafts::editCustomUrl,
                    label = tr("Server URL (chat completions)"),
                    secret = false,
                    keyboardType = KeyboardType.Uri,
                    isError = drafts.customUrl.isBlank(),
                    placeholder = "https://host/v1/chat/completions",
                )
            }
            if (ready && !changing) {
                Spacer(Modifier.height(12.dp))
                val testing = phase == SayHi.Phase.Running
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StickerButton(
                        if (testing) tr("Testing…") else tr("Test it"),
                        { if (!testing && !sayHi.runIfReady(s)) view.buzz(Buzz.REJECT) },
                        modifier = Modifier.weight(1f),
                        style = StickerStyle.Surface,
                        height = 48.dp,
                        contentDescription = if (provider == LlmProvider.CUSTOM) tr("Test the server on a line") else tr("Test the key on a line"),
                    )
                    StickerButton(
                        if (provider == LlmProvider.CUSTOM) tr("Change token") else tr("Change key"),
                        { changing = true },
                        modifier = Modifier.weight(1f),
                        style = StickerStyle.Surface,
                        height = 48.dp,
                    )
                }
            } else if (provider != LlmProvider.CUSTOM || drafts.customUrl.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                    SecretField(
                        value = drafts.key,
                        onValueChange = {
                            notice = null
                            drafts.editKey(it)
                        },
                        label = apiKeyLabel(provider),
                        modifier = Modifier.weight(1f),
                        showLabel = false,
                        placeholder = apiKeyLabel(provider),
                    )
                    Spacer(Modifier.width(10.dp))
                    StickerButton(
                        tr("Paste"),
                        {
                            notice = null
                            val check = drafts.submitKey(clipboard.getText()?.text.orEmpty(), sayHi)
                            pasteNotice(check)?.let {
                                notice = it
                                view.buzz(Buzz.REJECT)
                                scope.launch { pasteWiggle.play(reduced) }
                            }
                        },
                        modifier = Modifier.fillMaxHeight(),
                        style = StickerStyle.Zap,
                        height = 48.dp,
                        fillWidth = false,
                        wiggle = pasteWiggle,
                        contentDescription = tr("Paste key from clipboard"),
                    )
                }
                notice?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = pop.punchText)
                }
                providerKeyHelp(provider).url?.let { url ->
                    Spacer(Modifier.height(4.dp))
                    TextLink(providerKeyHelp(provider).linkLabel, { uri.openUri(url) })
                }
            }
        }
    }
    SayHiResult(phase, onRetry = { sayHi.runIfReady(s) })
}

/**
 * Everything whose default is right for nearly everyone, folded: timing
 * and data, how the AI works, which AI, and troubleshooting.
 */
@Composable
private fun MoreOptions(
    settings: AppSettings,
    sink: SettingsSink,
    drafts: AiDrafts,
    open: Boolean,
    onToggle: () -> Unit,
    otherAi: Modifier,
) {
    val pop = LocalPop.current
    val reduced = LocalReducedMotion.current
    val interaction = remember { MutableInteractionSource() }
    val sunk = rememberSink(interaction)
    val chevron = animateFloatAsState(if (open) 180f else 0f, if (reduced) snap() else tween(200), label = "chevron")
    Column(Modifier.fillMaxWidth()) {
        PopSurface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = pop.surface, sunk = { sunk.value }) {
            Row(
                Modifier
                    .clickable(interaction, indication = null, role = Role.Button, onClick = onToggle)
                    .semantics(mergeDescendants = true) {
                        stateDescription = if (open) tr("Expanded") else tr("Collapsed")
                    }
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        tr("More options"),
                        style = MaterialTheme.typography.titleLarge,
                        color = pop.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        tr("Timing, data, model, other AIs. Leave these alone if things work."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = pop.inkSoft,
                    )
                }
                Text(
                    "▾",
                    style = MaterialTheme.typography.titleLarge,
                    color = pop.ink,
                    modifier = Modifier
                        .clearAndSetSemantics { }
                        .graphicsLayer { rotationZ = chevron.value },
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (reduced) Modifier else Modifier.animateContentSize())
        ) {
            if (open) MoreOptionsBody(settings, sink, drafts, otherAi)
        }
    }
}

@Composable
private fun MoreOptionsBody(settings: AppSettings, sink: SettingsSink, drafts: AiDrafts, otherAi: Modifier) {
    val pop = LocalPop.current
    val provider = settings.provider

    GroupTitle(tr("Timing and data"))
    PopSlider(
        tr("Reaction time"),
        settings.stabilityMs.toFloat(),
        200f..900f,
        { "${it.toInt()} ms" },
        { sink.setStabilityMs(it.toInt()) },
        startLabel = tr("Snappy"),
        endLabel = tr("Patient"),
    )
    Helper(tr("How long a page sits still before I read it."))
    Spacer(Modifier.height(16.dp))
    PopSlider(
        tr("Skip the top of the screen"),
        settings.ignoreTopPct,
        0f..0.15f,
        { "${(it * 100).toInt()}%" },
        sink::setIgnoreTopPct,
    )
    Helper(tr("Keeps me off your browser's address bar."))
    Spacer(Modifier.height(16.dp))
    PopToggleRow(
        tr("Data saver"),
        tr("Smaller page uploads. Tiny text may read a little less sharply."),
        settings.dataSaver,
        sink::setDataSaver,
    )

    GroupTitle(tr("How the AI works"))
    Text(tr("Thinking time"), style = MaterialTheme.typography.bodyLarge, color = pop.ink)
    Spacer(Modifier.height(8.dp))
    PopTiles(
        listOf(
            PopTile(AiReasoning.FAST, tr("Fast")),
            PopTile(AiReasoning.BALANCED, tr("Balanced")),
            PopTile(AiReasoning.THOROUGH, tr("Thorough")),
        ),
        selected = settings.aiReasoning,
        onSelect = sink::setAiReasoning,
        columns = 3,
        minHeight = 56.dp,
    )
    Spacer(Modifier.height(8.dp))
    Helper(
        when (settings.aiReasoning) {
            AiReasoning.FAST -> tr("Quickest first line. Recommended for comics.")
            AiReasoning.BALANCED -> tr("A little more care on hard pages, a little slower.")
            AiReasoning.THOROUGH -> tr("Most careful with who's speaking and wild lettering, but slower.")
        }
    )
    Spacer(Modifier.height(16.dp))
    ModelChoice(drafts)
    if (provider == LlmProvider.GEMINI) {
        Spacer(Modifier.height(16.dp))
        PopToggleRow(
            tr("AI art redraw"),
            tr("Asks an image model to repaint busy art under text. Often refused on explicit pages; off is fine."),
            settings.aiCleanup,
            sink::setAiCleanup,
        )
    } else {
        Spacer(Modifier.height(16.dp))
        val sees = settings.aiVision != AiVisionMode.OFF
        PopToggleRow(
            tr("Let the AI see the page"),
            if (sees) tr("Sends the page image, so handwriting and wild lettering are read too.")
            else tr("Sends only the text your phone reads. Uses less data; fancy lettering may be missed."),
            sees,
            { sink.setAiVision(if (it) AiVisionMode.AUTO else AiVisionMode.OFF) },
        )
    }

    Column(otherAi) {
        GroupTitle(tr("Use a different AI"))
        Helper(tr("Gemini is the fastest here and its key is free. Pick another only if you already pay for it."))
    }
    Spacer(Modifier.height(8.dp))
    ProviderTiles(provider, sink::setProvider)
    Spacer(Modifier.height(8.dp))
    Helper(tr("The key and, for Custom, the server URL go in Your AI above."))

    GroupTitle(tr("Troubleshooting"))
    PopToggleRow(
        tr("Diagnostics"),
        tr("Outlines what I find on the page and shows a status line, to see why a balloon was missed."),
        settings.diagnostics,
        sink::setDiagnostics,
    )
}

/** The AIs to choose from, Gemini first: shared by setup and Tweaks. */
@Composable
internal fun ProviderTiles(selected: LlmProvider, onSelect: (LlmProvider) -> Unit) {
    PopTiles(
        listOf(
            PopTile(LlmProvider.GEMINI, "Gemini", caption = tr("Free · fastest")),
            PopTile(LlmProvider.ANTHROPIC, "Claude", caption = tr("by Anthropic")),
            PopTile(LlmProvider.OPENAI, "OpenAI", caption = tr("Paid key")),
            PopTile(LlmProvider.OPENROUTER, "OpenRouter", caption = tr("Many models")),
            PopTile(LlmProvider.DEEPSEEK, "DeepSeek", caption = tr("The big fish")),
            PopTile(LlmProvider.CUSTOM, tr("Custom"), caption = tr("Your own server")),
        ),
        selected = selected,
        onSelect = onSelect,
        columns = 2,
        minHeight = 56.dp,
    )
}

/**
 * The model: automatic — the provider's default, for Gemini the newest
 * Flash — unless the reader picked one. Every provider offers its account's
 * live list, fetched once as soon as a key is in; typing stays available
 * for models or servers that don't list.
 */
@Composable
private fun ModelChoice(drafts: AiDrafts) {
    val pop = LocalPop.current
    val s = drafts.settings
    Text(tr("Model"), style = MaterialTheme.typography.bodyLarge, color = pop.ink)
    Spacer(Modifier.height(6.dp))
    val pinned = drafts.model.isNotBlank()
    val auto = if (s.provider == LlmProvider.GEMINI) tr("Automatic: the newest Gemini Flash. Best for almost everyone.")
    else tr("Blank = %s", s.copy(model = "").effectiveModel().ifEmpty { tr("your server's default") })
    Helper(if (pinned) drafts.model else auto)
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        ModelPicker(s, onPick = drafts::editModel, modifier = Modifier.weight(1f))
        if (pinned) {
            StickerButton(
                tr("Automatic"),
                { drafts.editModel("") },
                modifier = Modifier.weight(1f),
                style = StickerStyle.Surface,
                height = 48.dp,
                contentDescription = tr("Use the automatic model"),
            )
        }
    }
    if (s.provider != LlmProvider.GEMINI) {
        Spacer(Modifier.height(8.dp))
        SecretField(
            value = drafts.model,
            onValueChange = drafts::editModel,
            label = tr("Model"),
            secret = false,
            showLabel = false,
            placeholder = tr("Or type a model name"),
        )
    }
}

/**
 * Offers the provider's live model list as a menu, so the picker shows
 * models released long after this build shipped. The list is fetched once
 * a key is in (Custom servers may need none), and again on tap if that
 * failed. The copy names Gemini, never its maker: to a reader who found the
 * old machine translation useless, the maker's name reads as that engine
 * coming back.
 */
@Composable
private fun ModelPicker(settings: AppSettings, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val pop = LocalPop.current
    val scope = rememberCoroutineScope()
    val apiKey = settings.apiKey
    val canAsk = apiKey.isNotBlank() || settings.provider == LlmProvider.CUSTOM && settings.customUrl.isNotBlank()
    var open by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    // The reader tapped and is waiting: only then does the button say so.
    // The silent fetch after a key goes in leaves the button alone, so it
    // doesn't flash.
    var waiting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var width by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    var models by remember(settings.provider, apiKey, settings.customUrl) { mutableStateOf<List<ModelCatalog.LiveModel>>(emptyList()) }
    suspend fun fetch() {
        loading = true
        try {
            models = ModelCatalog.list(settings)
            error = if (models.isEmpty()) tr("No usable models came back.") else null
            open = waiting && models.isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = tr("Couldn't fetch models: %s", e.message ?: tr("network error"))
        } finally {
            loading = false
            waiting = false
        }
    }
    // Once per provider and key: the list is ready by the time the menu is tapped.
    LaunchedEffect(settings.provider, apiKey, settings.customUrl) {
        error = null
        if (canAsk) {
            delay(600)
            fetch()
        }
    }
    // The error sits above the button, not beside it, so the button keeps its size.
    Column(modifier) {
        error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = pop.punchText)
            Spacer(Modifier.height(6.dp))
        }
        Box(Modifier.fillMaxWidth().onSizeChanged { width = with(density) { it.width.toDp() } }) {
            StickerButton(
                if (waiting) tr("Asking %s…", LlmHttp.providerLabel(settings)) else tr("Choose a model ▾"),
                {
                    if (!canAsk) {
                        error = tr("Add your key first. The list comes from your account.")
                        return@StickerButton
                    }
                    if (models.isNotEmpty()) {
                        open = true
                        return@StickerButton
                    }
                    if (waiting) return@StickerButton
                    waiting = true
                    if (loading) return@StickerButton // the silent fetch opens the menu when it lands
                    error = null
                    scope.launch { fetch() }
                },
                style = StickerStyle.Surface,
                height = 48.dp,
            )
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = pop.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(2.dp, pop.stroke),
                modifier = Modifier.width(width),
            ) {
                models.forEach { m ->
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(m.label, style = MaterialTheme.typography.titleMedium, color = pop.ink)
                                if (m.label != m.id) Text(m.id, style = MonoStyle.copy(fontSize = 14.sp), color = pop.inkSoft)
                            }
                        },
                        onClick = {
                            open = false
                            onPick(m.id)
                        },
                    )
                }
            }
        }
    }
}
