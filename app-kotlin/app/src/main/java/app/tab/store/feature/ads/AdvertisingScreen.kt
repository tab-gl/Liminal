package app.tab.store.feature.ads

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.Campaign
import app.tab.store.di.ServiceGraph
import app.tab.store.ui.components.ButtonSize
import app.tab.store.ui.components.ButtonVariant
import app.tab.store.ui.components.EmptyState
import app.tab.store.ui.components.SproutlyButton
import app.tab.store.ui.components.SproutlyCard
import app.tab.store.ui.components.SproutlyPill
import app.tab.store.ui.components.SproutlyTextButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class AdsUiState(
    val campaigns: List<Campaign> = emptyList(),
    val apps: List<AppListing> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class AdvertisingViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdsUiState())
    val state: StateFlow<AdsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        val uid = ServiceGraph.auth.uid ?: return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                ServiceGraph.campaigns.mine(uid) to ServiceGraph.publishing.myApps(uid)
            }.onSuccess { (campaigns, apps) ->
                _state.update { it.copy(campaigns = campaigns, apps = apps, loading = false, error = null) }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "Could not load") }
            }
        }
    }

    fun create(listing: AppListing, name: String, budgetRupees: Long, headline: String) {
        val uid = ServiceGraph.auth.uid ?: return
        viewModelScope.launch {
            runCatching {
                ServiceGraph.campaigns.create(
                    uid = uid,
                    listing = listing,
                    name = name,
                    budgetPaise = budgetRupees * 100,
                    placements = listOf("discover_feed"),
                    categoryIds = listOf(listing.categoryId),
                    headline = headline,
                )
            }.onSuccess { refresh() }
        }
    }

    fun togglePause(campaign: Campaign) {
        viewModelScope.launch {
            val next = if (campaign.campaignStatus == app.tab.store.data.model.CampaignStatus.ACTIVE) "paused" else "active"
            runCatching { ServiceGraph.campaigns.setStatus(campaign.id, next) }.onSuccess { refresh() }
        }
    }
}

/**
 * Advertising.
 *
 * A campaign is created in `pending_approval` and only starts delivering once a
 * moderator approves it. Delivery, pacing and spend are computed server-side in
 * `serveAds`, which is why nothing here can inflate a number.
 */
@Composable
fun AdvertisingScreen(
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: AdvertisingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                SproutlyTextButton(label = "Back", onClick = onBack)
                Text("Advertising", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Paid placements, always labelled Sponsored",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.error != null) {
            item {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            SproutlyButton(
                label = if (showCreate) "Cancel" else "New campaign",
                variant = if (showCreate) ButtonVariant.Ghost else ButtonVariant.Primary,
                fillWidth = true,
                onClick = { showCreate = !showCreate },
            )
        }

        if (showCreate) {
            item {
                NewCampaignCard(
                    apps = state.apps,
                    onCreate = { listing, name, budget, headline ->
                        viewModel.create(listing, name, budget, headline)
                        showCreate = false
                    },
                )
            }
        }

        if (state.campaigns.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "No campaigns yet",
                    message = "Promote one of your listings on the Discover feed. You set the " +
                        "budget and can pause at any time.",
                )
            }
        }

        items(state.campaigns, key = { it.id }) { campaign ->
            CampaignCard(campaign = campaign, onToggle = { viewModel.togglePause(campaign) })
        }
    }
}

@Composable
private fun NewCampaignCard(
    apps: List<AppListing>,
    onCreate: (AppListing, String, Long, String) -> Unit,
) {
    var selected by remember { mutableStateOf(apps.firstOrNull()) }
    var name by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("500") }
    var headline by remember { mutableStateOf("") }

    SproutlyCard {
        Text("New campaign", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = selected?.let { "Promoting: ${it.name}" } ?: "Publish an app first",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (apps.size > 1) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                apps.take(3).forEach { app ->
                    SproutlyTextButton(label = app.name, onClick = { selected = app })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Campaign name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = headline,
            onValueChange = { headline = it },
            label = { Text("Headline shown in the ad") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = budget,
            onValueChange = { budget = it.filter { char -> char.isDigit() } },
            label = { Text("Budget (INR)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        SproutlyButton(
            label = "Create campaign",
            fillWidth = true,
            enabled = selected != null && name.isNotBlank() && (budget.toLongOrNull() ?: 0) >= 100,
            onClick = {
                selected?.let { onCreate(it, name, budget.toLongOrNull() ?: 0, headline) }
            },
        )
    }
}

@Composable
private fun CampaignCard(campaign: Campaign, onToggle: () -> Unit) {
    val money = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    SproutlyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(campaign.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth(0.6f))
            SproutlyPill(label = campaign.campaignStatus.label)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${campaign.impressions} impressions · ${campaign.clicks} clicks · " +
                "${(campaign.ctr * 100).toInt()}% CTR",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { campaign.progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${money.format(campaign.spendRupees)} spent of ${money.format(campaign.budgetRupees)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (campaign.campaignStatus == app.tab.store.data.model.CampaignStatus.ACTIVE ||
            campaign.campaignStatus == app.tab.store.data.model.CampaignStatus.PAUSED
        ) {
            Spacer(Modifier.height(10.dp))
            SproutlyButton(
                label = if (campaign.campaignStatus == app.tab.store.data.model.CampaignStatus.ACTIVE) "Pause" else "Resume",
                variant = ButtonVariant.Outline,
                size = ButtonSize.Small,
                onClick = onToggle,
            )
        }
    }
}
