package com.shrug.shrugtv.ui.tv.settings

import androidx.compose.foundation.background
import com.shrug.shrugtv.ui.tv.TvShapes
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.R
import com.shrug.shrugtv.ui.tv.Inter
import com.shrug.shrugtv.ui.tv.TvColors

/**
 * About, Changes, Privacy, Licences and Imprint as the right-hand page of the settings
 *; before, three WebView activities with their own purple style. Texts
 * live in strings_info.xml (values/ English, values-de/ German), edition texts
 * in src/<edition>/res; the changelog content itself is English in every UI language.
 * The imprint exists only in the public edition (BuildConfig.EDITION "oss").
 */
enum class InfoPage(private val titleRes: Int) {
    ABOUT(R.string.info_title_about), CHANGES(R.string.info_title_changes), PRIVACY(R.string.info_title_privacy),
    LICENCES(R.string.info_title_licences), IMPRINT(R.string.info_title_imprint);

    val title: String @Composable get() = stringResource(titleRes)

    companion object {
        /** The pages this edition shows, in order. */
        val visible: List<InfoPage> = entries.filter { it != IMPRINT || BuildConfig.EDITION == "oss" }
    }
}

/** Privacy sections as (heading, text); the proxy section only where the proxy exists. */
private val privacySections = listOfNotNull(
    R.string.info_privacy_h_controller to R.string.info_privacy_controller,
    R.string.info_privacy_h_device to R.string.info_privacy_device,
    R.string.info_privacy_h_twitch to R.string.info_privacy_twitch,
    R.string.info_privacy_h_emotes to R.string.info_privacy_emotes,
    R.string.info_privacy_h_optional to R.string.info_privacy_optional,
    if (BuildConfig.AD_FREE) R.string.info_privacy_h_proxy to R.string.info_privacy_proxy else null,
    R.string.info_privacy_h_rights to R.string.info_privacy_rights,
)

@Composable
fun InfoPageContent(page: InfoPage, version: String, changes: () -> List<Changelog.Version>, focus: FocusRequester) {
    when (page) {
        InfoPage.ABOUT -> TextCard(
            listOf(stringResource(R.string.info_about_1), stringResource(R.string.info_about_2)),
            footer = stringResource(R.string.info_about_footer, version),
        )
        InfoPage.PRIVACY -> SectionList(
            privacySections.map { (h, t) -> stringResource(h) to stringResource(t) },
            focus, intro = stringResource(R.string.info_privacy_intro),
        )
        InfoPage.LICENCES -> SectionList(
            Licences.components.map { c -> c.name to listOfNotNull(c.licence, c.note).joinToString("\n") },
            focus, intro = stringResource(R.string.info_licences_intro),
        )
        InfoPage.IMPRINT -> SectionList(
            listOf(
                stringResource(R.string.info_imprint_h_provider) to stringResource(R.string.imprint_provider),
                stringResource(R.string.info_imprint_h_contact) to stringResource(R.string.imprint_contact),
                stringResource(R.string.info_imprint_h_note) to stringResource(R.string.info_imprint_note),
            ),
            focus,
        )
        InfoPage.CHANGES -> {
            val versions = remember { changes() }
            if (versions.isEmpty()) {
                TextCard(listOf(stringResource(R.string.info_changes_empty)))
            } else {
                LazyColumn(
                    Modifier.focusRequester(focus),
                    contentPadding = PaddingValues(bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(versions, key = { it.number }) { VersionCard(it) }
                }
            }
        }
    }
}

@Composable
private fun TextCard(paragraphs: List<String>, footer: String? = null) {
    Column(
        Modifier.widthIn(max = 760.dp).clip(TvShapes.r(12.dp)).background(TvColors.Surface).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        paragraphs.forEach { Text(it, fontFamily = Inter, fontSize = 13.sp, lineHeight = 19.sp, color = TvColors.TextMedium) }
        if (footer != null) Text(footer, fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Longer texts as focusable cards (heading + text), so the page scrolls with the arrow keys. */
@Composable
private fun SectionList(sections: List<Pair<String, String>>, focus: FocusRequester, intro: String? = null) {
    LazyColumn(
        Modifier.focusRequester(focus),
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (intro != null) item { TextCard(listOf(intro)) }
        items(sections, key = { it.first }) { (heading, text) -> SectionCard(heading, text) }
    }
}

@Composable
private fun SectionCard(heading: String, text: String) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(
        Modifier.fillMaxWidth().widthIn(max = 900.dp)
            .clip(TvShapes.r(12.dp))
            .background(if (focused) TvColors.SurfaceHigh else TvColors.Surface)
            .border(2.dp, if (focused) TvColors.Focus else Color.Transparent, TvShapes.r(12.dp))
            .focusable(interactionSource = source)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(heading, fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TvColors.Text)
        Text(text, fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp, color = TvColors.TextMedium)
    }
}

/** One version as a card; focusable so the list scrolls with the arrow keys. */
@Composable
private fun VersionCard(v: Changelog.Version) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(
        Modifier.fillMaxWidth().widthIn(max = 900.dp)
            .clip(TvShapes.r(12.dp))
            .background(if (focused) TvColors.SurfaceHigh else TvColors.Surface)
            .border(2.dp, if (focused) TvColors.Focus else Color.Transparent, TvShapes.r(12.dp))
            .focusable(interactionSource = source)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(v.number, fontFamily = Inter, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TvColors.Text)
            v.date?.let { Text(it, fontFamily = Inter, fontSize = 11.sp, color = TvColors.TextMuted, modifier = Modifier.padding(bottom = 2.dp)) }
        }
        v.sections.forEach { a ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Brand(a.title)
                a.bullets.forEach { p ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", fontFamily = Inter, fontSize = 12.sp, color = TvColors.TextMuted)
                        Text(marked(p, TvColors.Text, TvColors.AccentLight), fontFamily = Inter, fontSize = 12.sp, lineHeight = 17.sp, color = TvColors.TextMedium)
                    }
                }
            }
        }
    }
}

/** Badge per changelog section: colour by the English section name, label from the resources. */
@Composable
private fun Brand(title: String) {
    val color = when (title) {
        "New" -> TvColors.Accent
        "Fixed" -> Color(0xFF00A86B)
        "Changed" -> Color(0xFF3A3A44)
        else -> Color(0xFF2A2A31)
    }
    val label = when (title) {
        "New" -> stringResource(R.string.info_badge_new)
        "Changed" -> stringResource(R.string.info_badge_changed)
        "Fixed" -> stringResource(R.string.info_badge_fixed)
        "Removed" -> stringResource(R.string.info_badge_removed)
        else -> title
    }
    Text(
        label.uppercase(), fontFamily = Inter, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White,
        modifier = Modifier.clip(TvShapes.r(6.dp)).background(color).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun marked(text: String, bold: Color, code: Color): AnnotatedString = buildAnnotatedString {
    for (t in Changelog.parts(text)) when (t.kind) {
        Changelog.Kind.NORMAL -> append(t.text)
        Changelog.Kind.BOLD -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = bold)) { append(t.text) }
        Changelog.Kind.CODE -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = code)) { append(t.text) }
    }
}
