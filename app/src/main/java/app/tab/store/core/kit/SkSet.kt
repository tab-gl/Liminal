package app.tab.store.core.kit

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * The rest of the component set, in the spirit of composables.com: each one is a
 * small self-contained piece you could copy into a screen, with no library behind
 * it and no Material underneath.
 *
 * Everything here is rounded: circles for actions and tags, 28dp for cards, 40dp
 * for sheets.
 */

/** An icon, tinted, sized, from a drawable. */
@Composable
fun SkIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Sk.colors.onSurface,
) {
    Image(
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun SkIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Sk.colors.onSurface,
    background: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SkIcon(icon, contentDescription, tint = tint)
    }
}

@Composable
fun SkTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Sk.colors.olive) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        SkText(text, style = Sk.type.label, color = color, maxLines = 1)
    }
}

/** App bar: a back chevron, a title, and whatever actions a screen needs. */
@Composable
fun SkTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes backIcon: Int? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScopeMarker.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null && backIcon != null) {
            SkIconButton(backIcon, "Back", onBack)
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            SkText(title, style = Sk.type.title, maxLines = 1)
            if (!subtitle.isNullOrBlank()) {
                SkText(subtitle, style = Sk.type.small, color = Sk.colors.onSurfaceMuted, maxLines = 1)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { RowScopeMarker.actions() }
    }
}

/** A no-op receiver so an app bar's actions can be written as a lambda block. */
object RowScopeMarker

/** Screen scaffold: content clears the system bars and nothing else. */
@Composable
fun SkScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Sk.colors.background),
    ) {
        topBar()
        Column(Modifier.weight(1f), content = content)
        Box(Modifier.navigationBarsPadding()) { bottomBar() }
    }
}

/** A centred dialog with rounded corners and a scrim. */
@Composable
fun SkDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(SkHeroRadius))
                    .background(Sk.colors.surface)
                    .clickable(enabled = false) {}
                    .padding(20.dp),
                content = content,
            )
        }
    }
}

/** A sheet from the bottom, rounded at the top, dismissed by tapping the scrim. */
@Composable
fun SkSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = SkSheetRadius, topEnd = SkSheetRadius))
                    .background(Sk.colors.surface)
                    .clickable(enabled = false) {}
                    .navigationBarsPadding()
                    .padding(20.dp),
                content = content,
            )
        }
    }
}

/** A toast that appears over the content and leaves on its own. */
@Composable
fun SkToast(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(CircleShape)
            .background(Sk.colors.espresso)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        SkText(message, style = Sk.type.small, color = Sk.colors.cream, maxLines = 2)
    }
}

/** A count on an icon. */
@Composable
fun SkBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Sk.colors.olive)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        SkText(
            if (count > 99) "99+" else count.toString(),
            style = Sk.type.eyebrow,
            color = Sk.colors.lime,
        )
    }
}

/** Segmented control: the selected segment slides into the accent colour. */
@Composable
fun SkSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Sk.colors.surfaceHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            val weight by animateFloatAsState(if (active) 1f else 0f, spring(), label = "seg")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(Sk.colors.olive.copy(alpha = weight))
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                SkText(
                    label,
                    style = Sk.type.label,
                    color = if (active) Sk.colors.lime else Sk.colors.onSurfaceMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A number with a label under it — the console's stat tile. */
@Composable
fun SkStatCard(value: String, label: String, modifier: Modifier = Modifier, detail: String? = null) {
    SkSurface(modifier = modifier, color = Sk.colors.surface, radius = 24.dp) {
        Column(Modifier.padding(14.dp)) {
            SkText(value, style = Sk.type.title)
            SkText(label, style = Sk.type.small, color = Sk.colors.onSurfaceMuted)
            if (!detail.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                SkText(detail, style = Sk.type.small, color = Sk.colors.onSurfaceMuted, maxLines = 1)
            }
        }
    }
}

/** A search field with a magnifier and a clear button. */
@Composable
fun SkSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(Sk.colors.surfaceHigh)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkText("⌕", style = Sk.type.title, color = Sk.colors.onSurfaceMuted)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                SkText(placeholder, style = Sk.type.body, color = Sk.colors.onSurfaceMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = Sk.type.body.copy(color = Sk.colors.onSurface),
                cursorBrush = SolidColor(Sk.colors.olive),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            SkTextButton("Clear", onClick = { onValueChange("") })
        }
    }
}

/** A divider that reads as a rule, not a hairline. */
@Composable
fun SkRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Sk.colors.espresso.copy(alpha = 0.12f)),
    )
}

/** A small floating pill bar, used for tabs inside a console. */
@Composable
fun SkFloatingBar(modifier: Modifier = Modifier, content: @Composable RowScopeMarker.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(36.dp))
            .background(Sk.colors.surface)
            .border(1.dp, Sk.colors.outline.copy(alpha = 0.5f), RoundedCornerShape(36.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { RowScopeMarker.content() }
    }
}

/** One item of [SkFloatingBar]: icon, and a label when selected. */
@Composable
fun SkBarItem(
    @DrawableRes icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Sk.colors.olive else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkIcon(
            icon,
            contentDescription = label,
            tint = if (selected) Sk.colors.lime else Sk.colors.onSurfaceMuted,
            size = 18.dp,
        )
        if (selected) {
            Spacer(Modifier.width(6.dp))
            SkText(label, style = Sk.type.label, color = Sk.colors.lime, maxLines = 1)
        }
    }
}
