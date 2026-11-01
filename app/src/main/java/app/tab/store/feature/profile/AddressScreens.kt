package app.tab.store.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tab.store.core.designsystem.EmptyState
import app.tab.store.core.designsystem.SproutlyButton
import app.tab.store.core.designsystem.SproutlyRadii
import app.tab.store.core.designsystem.SproutlyTag
import app.tab.store.core.designsystem.SproutlyTextField
import app.tab.store.core.designsystem.SproutlyTopBar
import app.tab.store.domain.model.Address

@Composable
fun AddressListScreen(
    onBack: () -> Unit,
    onEdit: (String?) -> Unit,
    viewModel: AddressListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SproutlyTopBar(
                title = "Addresses",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { onEdit(null) }) {
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                },
            )
            if (!state.isLoading && state.addresses.isEmpty()) {
                EmptyState(
                    title = "No saved addresses",
                    message = "Add an address so checkout is one tap next time.",
                    icon = Icons.Outlined.LocationOn,
                    actionLabel = "Add address",
                    onAction = { onEdit(null) },
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.addresses, key = { it.id }) { address ->
                        AddressCard(
                            address = address,
                            onEdit = { onEdit(address.id) },
                            onDelete = { viewModel.delete(address.id) },
                            onSetDefault = { viewModel.setDefault(address.id) },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun AddressCard(
    address: Address,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
) {
    Surface(
        shape = SproutlyRadii.card,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(address.fullName, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                SproutlyTag(text = address.label.uppercase())
                if (address.isDefault) {
                    Spacer(Modifier.width(6.dp))
                    SproutlyTag(
                        text = "DEFAULT",
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                address.oneLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                address.phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                if (!address.isDefault) {
                    TextButton(onClick = onSetDefault) { Text("Set default") }
                }
            }
        }
    }
}

@Composable
fun AddressEditScreen(
    addressId: String?,
    onBack: () -> Unit,
    viewModel: AddressEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        SproutlyTopBar(
            title = if (addressId == null) "Add address" else "Edit address",
            onBack = onBack,
        )
        Column(Modifier.padding(horizontal = 24.dp)) {
            SproutlyTextField(
                value = state.address.fullName,
                onValueChange = { v -> viewModel.update { it.copy(fullName = v) } },
                label = "Full name",
            )
            SproutlyTextField(
                value = state.address.phone,
                onValueChange = { v ->
                    viewModel.update { it.copy(phone = v.filter { c -> c.isDigit() }.take(13)) }
                },
                label = "Phone",
                keyboardType = KeyboardType.Phone,
            )
            SproutlyTextField(
                value = state.address.line1,
                onValueChange = { v -> viewModel.update { it.copy(line1 = v) } },
                label = "Flat / house, building",
            )
            SproutlyTextField(
                value = state.address.line2,
                onValueChange = { v -> viewModel.update { it.copy(line2 = v) } },
                label = "Area, landmark (optional)",
            )
            SproutlyTextField(
                value = state.address.city,
                onValueChange = { v -> viewModel.update { it.copy(city = v) } },
                label = "City",
            )
            SproutlyTextField(
                value = state.address.state,
                onValueChange = { v -> viewModel.update { it.copy(state = v) } },
                label = "State",
            )
            SproutlyTextField(
                value = state.address.pincode,
                onValueChange = { v ->
                    viewModel.update { it.copy(pincode = v.filter { c -> c.isDigit() }.take(6)) }
                },
                label = "PIN code",
                keyboardType = KeyboardType.Number,
            )
            SproutlyTextField(
                value = state.address.label,
                onValueChange = { v -> viewModel.update { it.copy(label = v) } },
                label = "Label",
                helperText = "Home, Work, Studio…",
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { viewModel.update { it.copy(isDefault = !it.isDefault) } },
                    shape = SproutlyRadii.pill,
                    color = if (state.address.isDefault) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        if (state.address.isDefault) "✓ Default address" else "Set as default",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            if (state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            SproutlyButton(
                text = "Save address",
                onClick = viewModel::save,
                enabled = state.canSave,
                loading = state.isSaving,
                icon = Icons.Filled.Add,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
