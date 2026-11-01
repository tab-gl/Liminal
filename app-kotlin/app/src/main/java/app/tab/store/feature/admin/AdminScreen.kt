package app.tab.store.feature.admin

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.Campaign
import app.tab.store.data.model.Report
import app.tab.store.data.model.UserProfile
import app.tab.store.di.ServiceGraph
import app.tab.store.ui.components.AppCardSkeleton
import app.tab.store.ui.components.AppListingCard
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

data class AdminUiState(
    val applications: List<AppListing> = emptyList(),
    val campaigns: List<Campaign> = emptyList(),
    val reports: List<Report> = emptyList(),
    val users: List<UserProfile> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
)

class AdminViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val apps = runCatching { ServiceGraph.admin.pendingApplications() }.getOrDefault(emptyList())
            val campaigns = runCatching { ServiceGraph.admin.pendingCampaigns() }.getOrDefault(emptyList())
            val reports = runCatching { ServiceGraph.admin.openReports() }.getOrDefault(emptyList())
            val users = runCatching { ServiceGraph.admin.recentUsers() }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    applications = apps,
                    campaigns = campaigns,
                    reports = reports,
                    users = users,
                    loading = false,
                )
            }
        }
    }

    fun moderateApp(appId: String, approve: Boolean, note: String?) {
        viewModelScope.launch {
            runCatching { ServiceGraph.admin.moderateApplication(appId, approve, note) }
                .onSuccess { _state.update { s -> s.copy(message = if (approve) "Approved" else "Sent back") }; refresh() }
                .onFailure { e -> _state.update { s -> s.copy(message = e.message) } }
        }
    }

    fun moderateCampaign(campaignId: String, approve: Boolean) {
        viewModelScope.launch {
            runCatching { ServiceGraph.admin.moderateCampaign(campaignId, approve) }
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { s -> s.copy(message = e.message) } }
        }
    }

    fun resolveReport(reportId: String, outcome: String) {
        viewModelScope.launch {
            runCatching { ServiceGraph.admin.resolveReport(reportId, outcome) }
                .onSuccess { refresh() }
        }
    }

    fun setRole(uid: String, role: String) {
        viewModelScope.launch {
            runCatching { ServiceGraph.admin.setRole(uid, role) }.onSuccess { refresh() }
        }
    }
}

/**
 * The admin console.
 *
 * Every action here calls a Cloud Function that re-checks the admin claim - the
 * UI gate is only cosmetic. Rejection requires a note, because the developer sees
 * it verbatim.
 */
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    bottomPadding: Dp = 0.dp,
    viewModel: AdminViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Queue" to state.applications.size,
        "Ads" to state.campaigns.size,
        "Reports" to state.reports.size,
        "People" to 0,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                SproutlyTextButton(label = "Back", onClick = onBack)
                Text("Moderation", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Oldest first, so nothing starves",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabs.forEachIndexed { index, (label, count) ->
                    SproutlyTextButton(
                        label = if (count > 0) "$label ($count)" else label,
                        onClick = { tab = index },
                    )
                }
            }
        }

        if (state.message != null) {
            item { Text(state.message!!, style = MaterialTheme.typography.labelSmall) }
        }

        when (tab) {
            0 -> {
                if (state.loading) {
                    items(2) { AppCardSkeleton() }
                } else if (state.applications.isEmpty()) {
                    item { EmptyState(title = "Queue is clear", message = "Nothing waiting for review.") }
                } else {
                    items(state.applications, key = { it.id }) { listing ->
                        AppListingCard(listing = listing, onClick = {}, showDeveloper = true)
                        ModerationActions(
                            onApprove = { viewModel.moderateApp(listing.id, true, null) },
                            onReject = { note -> viewModel.moderateApp(listing.id, false, note) },
                        )
                    }
                }
            }
            1 -> {
                if (state.campaigns.isEmpty()) {
                    item { EmptyState(title = "No campaigns waiting", message = "Approved campaigns deliver automatically.") }
                } else {
                    items(state.campaigns, key = { it.id }) { campaign ->
                        SproutlyCard {
                            Text(campaign.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${campaign.applicationName} · ₹${campaign.budgetRupees} budget",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SproutlyButton(
                                    label = "Approve",
                                    size = ButtonSize.Small,
                                    onClick = { viewModel.moderateCampaign(campaign.id, true) },
                                )
                                SproutlyButton(
                                    label = "Reject",
                                    size = ButtonSize.Small,
                                    variant = ButtonVariant.Outline,
                                    onClick = { viewModel.moderateCampaign(campaign.id, false) },
                                )
                            }
                        }
                    }
                }
            }
            2 -> {
                if (state.reports.isEmpty()) {
                    item { EmptyState(title = "No open reports", message = "Reported content shows up here.") }
                } else {
                    items(state.reports, key = { it.id }) { report ->
                        SproutlyCard {
                            Text("${report.targetType}: ${report.reason}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = report.details ?: "No details provided",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SproutlyButton(
                                    label = "Action taken",
                                    size = ButtonSize.Small,
                                    onClick = { viewModel.resolveReport(report.id, "actioned") },
                                )
                                SproutlyButton(
                                    label = "Dismiss",
                                    size = ButtonSize.Small,
                                    variant = ButtonVariant.Outline,
                                    onClick = { viewModel.resolveReport(report.id, "dismissed") },
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                items(state.users, key = { it.uid }) { user ->
                    SproutlyCard {
                        Row {
                            Column(Modifier.fillMaxWidth(0.62f)) {
                                Text(user.displayName.ifBlank { "Unnamed" }, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${user.publishedAppCount} apps · role ${user.role}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            SproutlyPill(label = if (user.isDeveloper) "Verified" else "Member")
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SproutlyButton(
                                label = "Verify developer",
                                size = ButtonSize.Small,
                                onClick = { viewModel.setRole(user.uid, "developer") },
                            )
                            if (user.role != "admin") {
                                SproutlyButton(
                                    label = "Make admin",
                                    size = ButtonSize.Small,
                                    variant = ButtonVariant.Outline,
                                    onClick = { viewModel.setRole(user.uid, "admin") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationActions(onApprove: () -> Unit, onReject: (String?) -> Unit) {
    var note by remember { mutableStateOf("") }
    var rejecting by remember { mutableStateOf(false) }

    Column(Modifier.padding(bottom = 6.dp)) {
        if (rejecting) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Reason (the developer sees this)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SproutlyButton(label = "Approve", size = ButtonSize.Small, onClick = onApprove)
            SproutlyButton(
                label = if (rejecting) "Send reason" else "Request changes",
                size = ButtonSize.Small,
                variant = ButtonVariant.Outline,
                enabled = !rejecting || note.isNotBlank(),
                onClick = {
                    if (rejecting) onReject(note.ifBlank { null }) else rejecting = true
                },
            )
        }
    }
}
