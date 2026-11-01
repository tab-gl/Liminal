package app.tab.store.feature.detail

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tab.store.ads.AdMobBanner
import app.tab.store.data.model.categoryById
import app.tab.store.install.InstallManager
import app.tab.store.ui.components.AppImage
import app.tab.store.ui.components.ButtonSize
import app.tab.store.ui.components.ButtonVariant
import app.tab.store.ui.components.EmptyState
import app.tab.store.ui.components.RatingStars
import app.tab.store.ui.components.ScreenLoader
import app.tab.store.ui.components.SproutlyButton
import app.tab.store.ui.components.SproutlyCard
import app.tab.store.ui.components.SproutlyPill
import app.tab.store.ui.components.SproutlyTextButton
import app.tab.store.ui.components.StatusChip
import app.tab.store.di.ServiceGraph
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    appId: String,
    onBack: () -> Unit,
) {
    val factory = remember(appId) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DetailViewModel(appId) as T
        }
    }
    val viewModel: DetailViewModel = viewModel(key = appId, factory = factory)
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showReview by remember { mutableStateOf(false) }

    if (state.loading) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) { ScreenLoader(label = "Loading listing") }
        return
    }

    val listing = state.listing
    if (listing == null) {
        EmptyState(
            title = "This app is not available",
            message = state.error ?: "It may have been removed by its developer.",
            actionLabel = "Back",
            onAction = onBack,
        )
        return
    }

    val category = categoryById(listing.categoryId)

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                SproutlyTextButton(label = "Back", onClick = onBack)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppImage(url = listing.iconUrl, size = 72.dp, contentDescription = listing.name)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(listing.name, style = MaterialTheme.typography.headlineSmall)
                        if (listing.tagline.isNotBlank()) {
                            Text(
                                text = listing.tagline,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SproutlyPill(label = category.label)
                    SproutlyPill(label = if (listing.isFree) "Free" else listing.pricing.label)
                    if (listing.isOpenSource) SproutlyPill(label = "Open source")
                    if (listing.appStatus != app.tab.store.data.model.AppStatus.APPROVED) {
                        StatusChip(listing.appStatus)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (listing.reviewCount > 0) {
                        RatingStars(rating = listing.rating, count = listing.reviewCount)
                    } else {
                        Text("No ratings yet", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${listing.viewCount} views",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SproutlyButton(
                        label = when {
                            listing.primaryActionUrl == null -> "No link yet"
                            listing.primaryActionUrl!!.endsWith(".apk", true) -> "Install app"
                            listing.primaryActionUrl!!.contains("play.google.com") -> "Get the app"
                            else -> "Open app"
                        },
                        onClick = {
                            viewModel.recordClick(listing.primaryActionUrl.orEmpty())
                            when (val outcome = InstallManager.install(context, listing.primaryActionUrl)) {
                                is InstallManager.Outcome.Launched ->
                                    Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
                                is InstallManager.Outcome.NeedsPermission -> {
                                    Toast.makeText(context, outcome.message, Toast.LENGTH_LONG).show()
                                    InstallManager.openInstallSettings(context)
                                }
                                is InstallManager.Outcome.Failed ->
                                    Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        fillWidth = true,
                        size = ButtonSize.Large,
                        enabled = listing.primaryActionUrl != null,
                    )
                }
                Spacer(Modifier.height(10.dp))
                SproutlyButton(
                    label = if (state.bookmarked) "Saved" else "Save",
                    onClick = { viewModel.toggleBookmark() },
                    variant = if (state.bookmarked) ButtonVariant.Tonal else ButtonVariant.Outline,
                    fillWidth = true,
                )
            }
        }

        if (listing.description.isNotBlank()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("About this app", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(listing.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (listing.features.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    listing.features.forEach { feature ->
                        Text("•  $feature", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reviews", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    SproutlyTextButton(label = "Write a review", onClick = { showReview = true })
                }
            }
        }

        if (state.reviews.isEmpty()) {
            item {
                Text(
                    text = "No reviews yet. Reviews appear here once people start using the app.",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.reviews, key = { it.id }) { review ->
                SproutlyCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(review.userName, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(8.dp))
                        RatingStars(rating = review.rating.toDouble())
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(review.body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            AdMobBanner(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
        }
    }

    if (showReview) {
        ReviewDialog(
            onDismiss = { showReview = false },
            onSubmit = { rating, body ->
                val uid = ServiceGraph.auth.uid
                val user = ServiceGraph.auth.currentUser
                if (uid == null) {
                    Toast.makeText(context, "Sign in to review.", Toast.LENGTH_SHORT).show()
                    return@ReviewDialog
                }
                scope.launch {
                    runCatching {
                        ServiceGraph.reviews.upsert(
                            applicationId = listing.id,
                            uid = uid,
                            userName = user?.displayName ?: "Sproutly member",
                            photoUrl = user?.photoUrl?.toString(),
                            rating = rating,
                            body = body,
                        )
                    }.onSuccess {
                        Toast.makeText(context, "Thanks for the review.", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, it.message ?: "Could not save the review.", Toast.LENGTH_LONG).show()
                    }
                }
                showReview = false
            },
        )
    }
}

@Composable
private fun ReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit,
) {
    var rating by remember { mutableIntStateOf(5) }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write a review") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        SproutlyTextButton(
                            label = if (star <= rating) "★" else "☆",
                            onClick = { rating = star },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("What should other people know?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "One review per app. You can edit it later.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        confirmButton = {
            SproutlyTextButton(
                label = "Post",
                onClick = { if (body.isNotBlank()) onSubmit(rating, body) },
            )
        },
        dismissButton = { SproutlyTextButton(label = "Cancel", onClick = onDismiss) },
    )
    }
}
