package app.tab.store.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.tab.store.core.kit.SkProgress
import app.tab.store.core.kit.SkSpinner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The Material 3 surface this app used, reimplemented.
 *
 * Every function here keeps the signature the screens already call, so the
 * migration is an import change, but the body is ours: rounded by construction,
 * drawn with Compose Foundation, and reading [Tokens] rather than MaterialTheme.
 */

// ---------------------------------------------------------------------------
// Text
// ---------------------------------------------------------------------------

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = TextStyle.Default,
) {
    val resolved = (if (style == TextStyle.Default) DefaultTextStyle else style).copy(
        color = if (color == Color.Unspecified) LocalContentColor.current else color,
        fontSize = if (fontSize.isSpecified) fontSize else style.fontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        fontStyle = fontStyle ?: style.fontStyle,
        textAlign = textAlign ?: style.textAlign,
        lineHeight = if (lineHeight.isSpecified) lineHeight else style.lineHeight,
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = resolved,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = (if (style == TextStyle.Default) DefaultTextStyle else style).copy(
            color = if (color == Color.Unspecified) LocalContentColor.current else color,
        ),
        overflow = overflow,
        maxLines = maxLines,
    )
}

// ---------------------------------------------------------------------------
// Surfaces and buttons
// ---------------------------------------------------------------------------

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Tokens.shapes.medium.dp),
    color: Color = Tokens.colorScheme.surface,
    contentColor: Color = Tokens.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.988f else 1f,
        animationSpec = androidx.compose.animation.core.tween(90),
        label = "surfacePress",
    )

    Column(
        modifier = modifier
            .androidx.compose.ui.draw.scale(scale)
            .clip(shape)
            .background(color)
            .then(
                if (border != null) Modifier.border(border, shape) else Modifier,
            )
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor,
        ) { content() }
    }
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) colors.containerColor else colors.disabledContainerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor,
        ) { content() }
    }
}

data class ButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

object ButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = Tokens.colorScheme.primary,
        contentColor: Color = Tokens.colorScheme.onPrimary,
        disabledContainerColor: Color = Tokens.colorScheme.surfaceContainerHigh,
        disabledContentColor: Color = Tokens.colorScheme.onSurfaceVariant,
    ) = ButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)

    @Composable
    fun textButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = Tokens.colorScheme.primary,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = Tokens.colorScheme.onSurfaceVariant,
    ) = ButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor)
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---------------------------------------------------------------------------
// Bars, dividers, badges
// ---------------------------------------------------------------------------

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = Tokens.colorScheme.outlineVariant,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}

@Composable
fun Badge(
    modifier: Modifier = Modifier,
    containerColor: Color = Tokens.colorScheme.primary,
    contentColor: Color = Tokens.colorScheme.onPrimary,
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (content != null) {
                Row(content = content)
            } else {
                Text(
                    "",
                    style = Tokens.typography.labelSmall,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        content()
        Box(Modifier.align(Alignment.TopEnd)) { badge() }
    }
}

// ---------------------------------------------------------------------------
// Progress
// ---------------------------------------------------------------------------

@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Tokens.colorScheme.primary,
    trackColor: Color = Tokens.colorScheme.surfaceContainerHigh,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    SkProgress(progress = null, modifier = modifier)
}

@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Tokens.colorScheme.primary,
    trackColor: Color = Tokens.colorScheme.surfaceContainerHigh,
    strokeCap: StrokeCap = StrokeCap.Round,
) {
    SkProgress(progress = progress(), modifier = modifier)
}

/** The shape-morphing loader, now a hand-drawn ring. */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier, color: Color = Tokens.colorScheme.primary) {
    SkSpinner(size = 22.dp, color = color)
}

// ---------------------------------------------------------------------------
// Snackbar
// ---------------------------------------------------------------------------

class SnackbarHostState {
    private val _message = mutableStateOf<String?>(null)
    val message: String? get() = _message.value

    suspend fun showSnackbar(message: String) {
        _message.value = message
        delay(3_200)
        if (_message.value == message) _message.value = null
    }

    fun dismiss() {
        _message.value = null
    }
}

@Composable
fun SnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val message = hostState.message
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(CircleShape)
                .background(Tokens.colorScheme.inverseSurface)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                message.orEmpty(),
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.inverseOnSurface,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Dialogs and sheets
// ---------------------------------------------------------------------------

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(32.dp),
    containerColor: Color = Tokens.colorScheme.surface,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Tokens.colorScheme.scrim)
                .clickable(onClick = onDismissRequest),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .clip(shape)
                    .background(containerColor)
                    .clickable(enabled = false) {}
                    .padding(22.dp),
            ) {
                title?.let {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalContentColor provides Tokens.colorScheme.onSurface,
                    ) { it() }
                    Spacer(Modifier.height(8.dp))
                }
                text?.let {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalContentColor provides Tokens.colorScheme.onSurfaceVariant,
                    ) { it() }
                    Spacer(Modifier.height(18.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.let {
                        it()
                        Spacer(Modifier.width(4.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}

/** Sheet state, kept because call sites remember one. */
class SheetState internal constructor() {
    var isVisible by mutableStateOf(true)
        internal set
}

@Composable
fun rememberModalBottomSheetState(skipPartiallyExpanded: Boolean = false): SheetState =
    remember { SheetState() }

@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
    containerColor: Color = Tokens.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Tokens.colorScheme.scrim)
                .clickable(onClick = onDismissRequest),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(containerColor)
                    .clickable(enabled = false) {}
                    .padding(top = 10.dp),
            ) {
                Box(
                    Modifier
                        .padding(bottom = 6.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Tokens.colorScheme.outlineVariant),
                )
                content()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Menus
// ---------------------------------------------------------------------------

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Tokens.colorScheme.scrim)
                .clickable(onClick = onDismissRequest),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth(0.86f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Tokens.colorScheme.surface)
                    .clickable(enabled = false) {}
                    .padding(vertical = 8.dp),
                content = content,
            )
        }
    }
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides Tokens.colorScheme.onSurface,
        ) { text() }
    }
}

// ---------------------------------------------------------------------------
// Text fields
// ---------------------------------------------------------------------------

data class TextFieldColors(
    val focusedContainerColor: Color,
    val unfocusedContainerColor: Color,
    val focusedIndicatorColor: Color,
    val unfocusedIndicatorColor: Color,
    val focusedTextColor: Color,
    val unfocusedTextColor: Color,
    val cursorColor: Color,
    val focusedPlaceholderColor: Color,
    val unfocusedPlaceholderColor: Color,
    val focusedLabelColor: Color,
    val unfocusedLabelColor: Color,
)

object OutlinedTextFieldDefaults {
    @Composable
    fun colors(
        focusedContainerColor: Color = Color.Transparent,
        unfocusedContainerColor: Color = Color.Transparent,
        focusedIndicatorColor: Color = Tokens.colorScheme.primary,
        unfocusedIndicatorColor: Color = Tokens.colorScheme.outline,
        focusedTextColor: Color = Tokens.colorScheme.onSurface,
        unfocusedTextColor: Color = Tokens.colorScheme.onSurface,
        cursorColor: Color = Tokens.colorScheme.primary,
        focusedPlaceholderColor: Color = Tokens.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor: Color = Tokens.colorScheme.onSurfaceVariant,
        focusedLabelColor: Color = Tokens.colorScheme.primary,
        unfocusedLabelColor: Color = Tokens.colorScheme.onSurfaceVariant,
    ) = TextFieldColors(
        focusedContainerColor, unfocusedContainerColor, focusedIndicatorColor, unfocusedIndicatorColor,
        focusedTextColor, unfocusedTextColor, cursorColor,
        focusedPlaceholderColor, unfocusedPlaceholderColor, focusedLabelColor, unfocusedLabelColor,
    )
}

object TextFieldDefaults {
    @Composable
    fun colors(
        focusedContainerColor: Color = Tokens.colorScheme.surfaceContainerHigh,
        unfocusedContainerColor: Color = Tokens.colorScheme.surfaceContainerHigh,
        focusedIndicatorColor: Color = Color.Transparent,
        unfocusedIndicatorColor: Color = Color.Transparent,
        focusedTextColor: Color = Tokens.colorScheme.onSurface,
        unfocusedTextColor: Color = Tokens.colorScheme.onSurface,
        cursorColor: Color = Tokens.colorScheme.primary,
        focusedPlaceholderColor: Color = Tokens.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor: Color = Tokens.colorScheme.onSurfaceVariant,
        focusedLabelColor: Color = Tokens.colorScheme.primary,
        unfocusedLabelColor: Color = Tokens.colorScheme.onSurfaceVariant,
    ) = TextFieldColors(
        focusedContainerColor, unfocusedContainerColor, focusedIndicatorColor, unfocusedIndicatorColor,
        focusedTextColor, unfocusedTextColor, cursorColor,
        focusedPlaceholderColor, unfocusedPlaceholderColor, focusedLabelColor, unfocusedLabelColor,
    )
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    shape: Shape = RoundedCornerShape(24.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    isError: Boolean = false,
) {
    Column(modifier) {
        label?.let {
            Box(Modifier.padding(start = 6.dp, bottom = 6.dp)) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalContentColor provides colors.unfocusedLabelColor,
                ) { it() }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.unfocusedContainerColor)
                .border(
                    width = if (isError) 2.dp else 1.dp,
                    color = if (isError) Tokens.colorScheme.error else colors.unfocusedIndicatorColor,
                    shape = shape,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalContentColor provides colors.unfocusedPlaceholderColor,
                    ) { placeholder() }
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = Tokens.typography.bodyMedium.copy(color = colors.unfocusedTextColor),
                    cursorBrush = SolidColor(colors.cursorColor),
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            trailingIcon?.let {
                Spacer(Modifier.width(8.dp))
                it()
            }
        }
    }
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    shape: Shape = CircleShape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.unfocusedContainerColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let {
            it()
            Spacer(Modifier.width(10.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty() && placeholder != null) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalContentColor provides colors.unfocusedPlaceholderColor,
                ) { placeholder() }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = Tokens.typography.bodyMedium.copy(color = colors.unfocusedTextColor),
                cursorBrush = SolidColor(colors.cursorColor),
                keyboardOptions = keyboardOptions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailingIcon?.let {
            Spacer(Modifier.width(8.dp))
            it()
        }
    }
}

// ---------------------------------------------------------------------------
// App bar and scaffold
// ---------------------------------------------------------------------------

data class TopAppBarColors(
    val containerColor: Color,
    val titleContentColor: Color,
    val navigationIconContentColor: Color,
    val actionIconContentColor: Color,
)

object TopAppBarDefaults {
    @Composable
    fun topAppBarColors(
        containerColor: Color = Color.Transparent,
        scrolledContainerColor: Color = Tokens.colorScheme.surface,
        titleContentColor: Color = Tokens.colorScheme.onSurface,
        navigationIconContentColor: Color = Tokens.colorScheme.onSurface,
        actionIconContentColor: Color = Tokens.colorScheme.onSurfaceVariant,
    ) = TopAppBarColors(containerColor, titleContentColor, navigationIconContentColor, actionIconContentColor)
}

@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.containerColor)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalContentColor provides colors.navigationIconContentColor,
        ) { navigationIcon() }
        Box(Modifier.weight(1f)) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContentColor provides colors.titleContentColor,
            ) { title() }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalContentColor provides colors.actionIconContentColor,
            ) { actions() }
        }
    }
}

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = Tokens.colorScheme.background,
    contentWindowInsets: Any? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor),
    ) {
        Column(Modifier.fillMaxSize()) {
            topBar()
            content(PaddingValues(0.dp))
        }
        Box(Modifier.align(Alignment.BottomCenter)) { bottomBar() }
        Box(Modifier.align(Alignment.BottomCenter)) { snackbarHost() }
    }
}
