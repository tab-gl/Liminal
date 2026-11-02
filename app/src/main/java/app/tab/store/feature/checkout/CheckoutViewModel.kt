package app.tab.store.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.Address
import app.tab.store.domain.model.Cart
import app.tab.store.domain.repository.AddressRepository
import app.tab.store.domain.repository.CartRepository
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val cart: Cart = Cart(),
    val addresses: List<Address> = emptyList(),
    val selectedAddressId: String? = null,
    val note: String = "",
    val isPlacingOrder: Boolean = false,
    val error: String? = null,
    val placedOrderId: String? = null,
) {
    val selectedAddress: Address?
        get() = addresses.firstOrNull { it.id == selectedAddressId }
            ?: addresses.firstOrNull { it.isDefault }
            ?: addresses.firstOrNull()

    val shipping: Long
        get() = if (cart.isEmpty) 0L else if (cart.subtotal >= 99900L) 0L else 4900L
    val total: Long get() = cart.subtotal + shipping
    val canPlaceOrder: Boolean
        get() = cart.lines.any { it.isAvailable } && selectedAddress != null && !isPlacingOrder
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val addressRepository: AddressRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.observeCart().collect { cart ->
                _uiState.update { it.copy(cart = cart) }
            }
        }
        viewModelScope.launch {
            addressRepository.observeAddresses().collect { addresses ->
                _uiState.update { state ->
                    val stillValid = addresses.any { it.id == state.selectedAddressId }
                    state.copy(
                        addresses = addresses,
                        selectedAddressId = if (stillValid) state.selectedAddressId
                        else addresses.firstOrNull { it.isDefault }?.id ?: addresses.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun selectAddress(id: String) = _uiState.update { it.copy(selectedAddressId = id) }

    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value.take(240)) }

    fun placeOrder() {
        val state = _uiState.value
        val address = state.selectedAddress ?: return
        if (!state.canPlaceOrder) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPlacingOrder = true, error = null) }
            when (val res = orderRepository.placeOrder(address.id, state.note)) {
                is DataResult.Failure -> _uiState.update {
                    it.copy(isPlacingOrder = false, error = res.message)
                }
                is DataResult.Success -> _uiState.update {
                    it.copy(isPlacingOrder = false, placedOrderId = res.value)
                }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
}
