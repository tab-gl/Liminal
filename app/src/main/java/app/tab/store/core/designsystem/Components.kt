package app.tab.store.core.designsystem

import androidx.annotation.DrawableRes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import app.tab.store.core.ui.ExperimentalMaterial3ExpressiveApi
import app.tab.store.core.ui.LoadingIndicator
import app.tab.store.core.ui.ExperimentalMaterial3Api
import app.tab.store.core.ui.IconButton
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import app.tab.store.core.ui.TextButton
import app.tab.store.core.ui.TopAppBar
import app.tab.store.core.ui.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tab.store.domain.model.Product
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.util.Locale

// ---------------------------------------------------------------------------
// Formatting helpers used by the UI layer
// ---------------------------------------------------------------------------

private val inr: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

fun formatPrice(paise: Long): String = inr.format(paise / 100.0)

fun formatCompact(value: Long): String = when {
    value >= 10_000_000 -> "₹${"%.2f".format(value / 10_000_000.0)}Cr"
    value >= 100_000 -> "₹${"%.2f".format(value / 100_000.0)}L"
    value >= 1_000 -> "₹${"%.1f".format(value / 1_000.0)}K"
    else -> "₹$value"
}

// ---------------------------------------------------------------------------
// App bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SproutlyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Tokens.colorScheme.background,
            titleContentColor = Tokens.colorScheme.onBackground,
        ),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    SproutlyIcon(PhosphorIcons.ChevronLeft, contentDescription = "Back")
                }
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = Tokens.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = Tokens.typography.bodySmall,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = { actions() },
    )
}

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SproutlyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
) {
    Surface(
        onClick = { if (enabled && !loading) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled && !loading,
        shape = SproutlyRadii.pill,
        color = if (enabled) Tokens.colorScheme.primary
        else Tokens.colorScheme.surfaceContainerHigh,
        contentColor = if (enabled) Tokens.colorScheme.onPrimary
        else Tokens.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                // M3 Expressive shape-morph loader, with the label kept beside it:
                // a spinner alone says nothing about what is happening.
                LoadingIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Tokens.colorScheme.onPrimary,
                )
                if (text.isNotBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Text(text, style = Tokens.typography.labelLarge)
                }
            } else {
                if (icon != null) {
                    SproutlyIcon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, style = Tokens.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SproutlySecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        enabled = enabled,
        shape = SproutlyRadii.pill,
        color = Tokens.colorScheme.primaryContainer,
        contentColor = Tokens.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                SproutlyIcon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = Tokens.typography.labelLarge)
        }
    }
}

// ---------------------------------------------------------------------------
// Tags, chips, pills
// ---------------------------------------------------------------------------

@Composable
fun SproutlyTag(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = Tokens.colorScheme.primaryContainer,
    content: Color = Tokens.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = modifier
            .clip(SproutlyRadii.pill)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = Tokens.typography.labelSmall, color = content)
    }
}

@Composable
fun SproutlyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = SproutlyRadii.pill,
        color = if (selected) Tokens.colorScheme.primary else Tokens.colorScheme.surface,
        contentColor = if (selected) Tokens.colorScheme.onPrimary
        else Tokens.colorScheme.onSurface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            1.dp, Tokens.colorScheme.outline,
        ),
    ) {
        Text(
            text = text,
            style = Tokens.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun RatingBadge(rating: Double, count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        SproutlyIcon(PhosphorIcons.Star,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = LocalSproutlyExtra.current.ratingGold,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "%.1f".format(rating),
            style = Tokens.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        if (count > 0) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = "($count)",
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PriceText(
    paise: Long,
    modifier: Modifier = Modifier,
    compareAt: Long? = null,
    large: Boolean = false,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formatPrice(paise),
            style = if (large) Tokens.typography.headlineMedium
            else Tokens.typography.titleLarge,
            color = Tokens.colorScheme.onSurface,
        )
        if (compareAt != null && compareAt > paise) {
            Spacer(Modifier.width(7.dp))
            Text(
                text = formatPrice(compareAt),
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
            )
            val off = ((compareAt - paise) * 100 / compareAt).toInt()
            Spacer(Modifier.width(7.dp))
            SproutlyTag(
                text = "$off% off",
                container = Tokens.colorScheme.tertiaryContainer,
                content = Tokens.colorScheme.onTertiaryContainer,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Images
// ---------------------------------------------------------------------------

@Composable
fun SproutlyImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Tokens.colorScheme.surfaceContainerHigh),
    ) {
        if (url.isNullOrBlank()) {
            // No artwork yet: an initial, not a broken-image glyph.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Tokens.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    contentDescription?.trim()?.firstOrNull()?.uppercase()?.toString() ?: "·",
                    style = Tokens.typography.headlineSmall,
                    color = Tokens.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(180)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Loading / empty / error states
// ---------------------------------------------------------------------------

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    val base = LocalSproutlyExtra.current.skeleton
    val highlight = LocalSproutlyExtra.current.skeletonHighlight
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    colors = listOf(base, highlight, base),
                    start = androidx.compose.ui.geometry.Offset(offset * 900f - 400f, 0f),
                    end = androidx.compose.ui.geometry.Offset(offset * 900f, 220f),
                ),
            ),
    )
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    @DrawableRes icon: Int? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Tokens.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                SproutlyIcon(
                    icon,
                    contentDescription = null,
                    tint = Tokens.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = title,
            style = Tokens.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = Tokens.typography.bodyMedium,
            color = Tokens.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            SproutlySecondaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        title = "Something went wrong",
        message = message,
        modifier = modifier,
        actionLabel = "Try again",
        onAction = onRetry,
    )
}

/**
 * The Material 3 Expressive loading indicator: a single shape that morphs
 * through the Material shape set with spring motion. Used everywhere the app
 * is genuinely waiting on data, so loading always looks like Sproutly rather
 * than like a generic spinner.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SproutlyLoader(
    modifier: Modifier = Modifier,
    label: String? = null,
    size: Dp = 44.dp,
    color: Color = Tokens.colorScheme.primary,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingIndicator(
            modifier = Modifier.size(size),
            color = color,
        )
        if (label != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = label,
                style = Tokens.typography.bodySmall,
                color = Tokens.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LoadingColumn(modifier: Modifier = Modifier, label: String? = null) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        contentAlignment = Alignment.Center,
    ) {
        SproutlyLoader(label = label)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = Tokens.typography.headlineMedium)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    actionLabel,
                    style = Tokens.typography.labelLarge,
                    color = Tokens.colorScheme.primary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Quantity stepper
// ---------------------------------------------------------------------------

@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    max: Int = 99,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier,
        shape = SproutlyRadii.pill,
        color = Tokens.colorScheme.surfaceContainerHigh,
        contentColor = Tokens.colorScheme.onSurface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease, enabled = enabled) {
                SproutlyIcon(PhosphorIcons.Close, contentDescription = "Decrease", modifier = Modifier.size(17.dp))
            }
            Text(
                text = quantity.toString(),
                style = Tokens.typography.titleMedium,
                modifier = Modifier.width(22.dp),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = onIncrease, enabled = enabled && quantity < max) {
                SproutlyIcon(PhosphorIcons.Plus, contentDescription = "Increase", modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Product card
// ---------------------------------------------------------------------------

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    /** Position on a chart; null when the card is not part of a ranking. */
    rank: Int? = null,
) {
    // A short press-scale makes the grid feel like it responds to the thumb
    // rather than waiting for the tap to complete.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 1600f),
        label = "cardScale",
    )

    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            Tokens.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
        interactionSource = interaction,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        Column(Modifier.padding(8.dp)) {
            Box {
                SproutlyImage(
                    url = product.primaryImage,
                    contentDescription = product.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    corner = 18.dp,
                )
                if (rank != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    ) {
                        SproutlyTag(text = "#$rank")
                    }
                } else if (product.discountPercent >= 20) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    ) {
                        SproutlyTag(text = "${product.discountPercent}% OFF")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 3.dp),
            ) {
                Text(
                    text = product.title,
                    style = Tokens.typography.titleSmall,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (product.official) {
                    Spacer(Modifier.width(6.dp))
                    OfficialBadge()
                }
            }
            Text(
                text = product.storeName.ifBlank { product.categoryLabel },
                style = Tokens.typography.labelSmall,
                color = Tokens.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 3.dp),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (product.isFree) "Free" else formatPrice(product.price),
                    style = Tokens.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                if (product.rating > 0) {
                    SproutlyIcon(
                        PhosphorIcons.Star,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Tokens.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "%.1f".format(product.rating),
                        style = Tokens.typography.labelSmall,
                        color = Tokens.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
fun ProductRowCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surface,
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            SproutlyImage(
                url = product.primaryImage,
                contentDescription = product.title,
                modifier = Modifier.size(78.dp),
                corner = 14.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    product.storeName,
                    style = Tokens.typography.labelSmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
                Text(
                    product.title,
                    style = Tokens.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatPrice(product.price),
                    style = Tokens.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            if (trailing != null) {
                Box(Modifier.padding(start = 8.dp), contentAlignment = Alignment.Center) {
                    trailing()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Brand
// ---------------------------------------------------------------------------

@Composable
fun BrandMark(
    size: Dp = 28.dp,
    modifier: Modifier = Modifier,
    /**
     * The leaf breathes while the app is open, and gives a small nod when the tab
     * changes. Static everywhere else.
     */
    animateLeaf: Boolean = false,
    /** Changes when the tab changes; the mark nods each time it does. */
    leafKey: Any? = null,
) {
    val breath by rememberInfiniteTransition(label = "leafBreath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "leafBreathValue",
    )
    val nod by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 400f),
        label = "leafNod",
    )
    val rotation = if (animateLeaf) -3f + breath * 6f else 0f
    val scale = if (animateLeaf) 1f + breath * 0.04f else 1f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(SproutlyPalette.Lime),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = app.tab.store.R.drawable.ic_sproutly_mark,
            ),
            contentDescription = "Sproutly",
            modifier = Modifier
                .fillMaxSize(0.86f)
                .graphicsLayer {
                    rotationZ = rotation + nod
                    scaleX = scale
                    scaleY = scale
                },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    markSize: Dp = 30.dp,
    animateLeaf: Boolean = true,
    leafKey: Any? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        BrandMark(size = markSize, animateLeaf = animateLeaf, leafKey = leafKey)
        Spacer(Modifier.width(9.dp))
        Text(
            text = "Sproutly",
            style = Tokens.typography.headlineMedium,
            color = Tokens.colorScheme.onBackground,
        )
    }
}

@Composable
fun Avatar(
    url: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Tokens.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Text(
                text = name.trim().take(1).uppercase().ifBlank { "S" },
                style = Tokens.typography.titleMedium,
                color = Tokens.colorScheme.onPrimaryContainer,
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(180)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Thin determinate bar used for Cloudinary upload progress. */
@Composable
fun UploadProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = fraction < 1f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(SproutlyRadii.pill)
                .background(Tokens.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(SproutlyRadii.pill)
                    .background(Tokens.colorScheme.primary),
            )
        }
    }
}

@Composable
fun SuccessCheck(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Tokens.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        SproutlyIcon(PhosphorIcons.Tick,
            contentDescription = null,
            tint = Tokens.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size / 2),
        )
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, SproutlyRadii.card, clip = false)
            .clip(SproutlyRadii.card)
            .background(Tokens.colorScheme.surface)
            .padding(16.dp),
        content = content,
    )
}

/**
 * Sprout Safe's risk meter.
 *
 * A number and a bar instead of a wall of bullet points: the score says how much
 * the build can reach, the colour says whether that is ordinary, and the two
 * strongest reasons sit underneath.
 */
@Composable
fun RiskMeter(
    score: Int,
    reasons: List<String>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val clamped = score.coerceIn(0, 100)
    // Nothing under 61 is treated as a warning. An app that asks for a camera and
    // notifications is ordinary; colouring that amber taught people to ignore it.
    val (container, content) = if (clamped > 60) {
        Tokens.colorScheme.errorContainer to Tokens.colorScheme.onErrorContainer
    } else {
        Tokens.colorScheme.primaryContainer to Tokens.colorScheme.onPrimaryContainer
    }
    val label = if (clamped > 60) "High reach" else "Low reach"

    Surface(
        shape = SproutlyRadii.card,
        color = container,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SproutlyIcon(
                    PhosphorIcons.Verified,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = content,
                )
                Spacer(Modifier.width(6.dp))
                Text("Sprout Safe", style = Tokens.typography.titleSmall, color = content)
                Spacer(Modifier.weight(1f))
                Text(
                    "$clamped / 100 · $label",
                    style = Tokens.typography.labelMedium,
                    color = content,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(SproutlyRadii.pill)
                    .background(content.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clamped / 100f)
                        .height(8.dp)
                        .clip(SproutlyRadii.pill)
                        .background(content),
                )
            }
            if (!compact && reasons.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                reasons.take(2).forEach { reason ->
                    Text("· $reason", style = Tokens.typography.bodySmall, color = content)
                }
            }
        }
    }
}

/** Marks a listing published by the store's own studio. */
@Composable
fun OfficialBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = SproutlyRadii.pill,
        color = Tokens.colorScheme.primary,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SproutlyIcon(
                PhosphorIcons.Verified,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Tokens.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Official",
                style = Tokens.typography.labelSmall,
                color = Tokens.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * Stand-in artwork for a listing with no image yet.
 *
 * A grey rectangle with a broken-image glyph reads as an error. The app's initial
 * on a tinted field reads as "no picture", which is the truth.
 */
@Composable
fun InitialArt(
    title: String,
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
) {
    val letter = title.trim().firstOrNull()?.uppercase() ?: "·"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(Tokens.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            style = Tokens.typography.displaySmall,
            color = Tokens.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
        )
    }
}
