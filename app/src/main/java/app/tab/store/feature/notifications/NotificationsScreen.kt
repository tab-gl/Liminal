package app.tab.store.feature.notifications

import app.tab.store.core.ui.Tokens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import app.tab.store.core.ui.Surface
import app.tab.store.core.ui.Text
import app.tab.store.core.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.SproutlyImage
import app.tab.store.core.designsystem.PhosphorIcons
import app.tab.store.core.designsystem.SproutlyIcon
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.core.utils.DateFormatters
import app.tab.store.domain.model.AppNotification
import app.tab.store.domain.model.NotificationType
import app.tab.store.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    /** Apps this person installed recently — the useful half of this screen. */
    val installs: List<app.tab.store.domain.model.Order> = emptyList(),
    val unread: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val orderRepository: app.tab.store.domain.repository.OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationRepository.observeNotifications().collect { items ->
                _uiState.value = _uiState.value.copy(
                    // Install notices are shown as "Recently installed" above;
                    // repeating them as updates reads like a second inbox.
                    items = items.filter { it.type != NotificationType.ORDER },
                    unread = items.count { !it.read },
                    isLoading = false,
                )
            }
        }
        // What you installed, newest first — a live feed rather than a list of
        // "orders" that never described anything.
        viewModelScope.launch {
            orderRepository.observeBuyerOrders().collect { orders ->
                val recent = orders
                    .sortedByDescending { it.placedAt }
                    .distinctBy { it.items.firstOrNull()?.productId ?: it.id }
                    .take(6)
                _uiState.value = _uiState.value.copy(installs = recent)
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead() }
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SproutlyTopBar(
            title = "Notifications",
            subtitle = if (state.unread > 0) "${state.unread} unread" else null,
            onBack = onBack,
            actions = {
                if (state.unread > 0) {
                    TextButton(onClick = viewModel::markAllRead) {
                        Text("Mark all read", style = Tokens.typography.labelMedium)
                    }
                }
            },
        )
        if (!state.isLoading && state.items.isEmpty() && state.installs.isEmpty()) {
            EmptyState(
                title = "Nothing yet",
                message = "Install an app and it shows up here.",
                icon = PhosphorIcons.Notification,
            )
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.installs.isNotEmpty()) {
                    item {
                        Text(
                            "Recently installed",
                            style = Tokens.typography.titleSmall,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                        )
                    }
                    items(state.installs, key = { "install-${it.id}" }) { order ->
                        val app = order.items.firstOrNull()
                        InstallRow(
                            title = app?.title ?: "An app",
                            imageUrl = app?.imageUrl,
                            version = app?.versionName.orEmpty(),
                            when_ = order.placedAt,
                            onClick = { app?.let { onOpen("product/${it.productId}") } },
                        )
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Updates",
                            style = Tokens.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                items(state.items, key = { it.id }) { item ->
                    NotificationRow(
                        item = item,
                        onClick = {
                            viewModel.markRead(item.id)
                            item.deepLink?.let(onOpen)
                        },
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: AppNotification, onClick: () -> Unit) {
    val icon: Int = when (item.type) {
        NotificationType.ORDER -> PhosphorIcons.Receipt
        NotificationType.STORE -> PhosphorIcons.Store
        NotificationType.PROMO -> PhosphorIcons.Notification
        NotificationType.SYSTEM -> PhosphorIcons.Notification
    }
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = if (item.read) Tokens.colorScheme.surface
        else Tokens.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Tokens.colorScheme.surface.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                SproutlyIcon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = Tokens.typography.titleSmall)
                Text(
                    item.body,
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    DateFormatters.relative(item.createdAt),
                    style = Tokens.typography.labelSmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One recently installed app, with its version and when. */
@Composable
private fun InstallRow(
    title: String,
    imageUrl: String?,
    version: String,
    when_: Long,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = SproutlyRadii.card,
        color = Tokens.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SproutlyImage(
                url = imageUrl,
                contentDescription = title,
                modifier = Modifier.size(44.dp),
                corner = 14.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = Tokens.typography.titleSmall)
                Text(
                    listOfNotNull(
                        version.takeIf { it.isNotBlank() }?.let { "v$it" },
                        DateFormatters.relative(when_),
                    ).joinToString(" · "),
                    style = Tokens.typography.bodySmall,
                    color = Tokens.colorScheme.onSurfaceVariant,
                )
            }
            SproutlyIcon(
                PhosphorIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Tokens.colorScheme.onSurfaceVariant,
            )
        }
    }
}
