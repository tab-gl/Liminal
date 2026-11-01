package app.tab.store.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.Cart
import app.tab.store.domain.repository.CartRepository
import app.tab.store.domain.repository.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartUiState(
    val cart: Cart = Cart(),
    val isBusy: Boolean = false,
    val message: String? = null,
) {
    val shipping: Long
        get() = when {
            cart.isEmpty -> 0L
            cart.subtotal >= FREE_SHIPPING_THRESHOLD -> 0L
            else -> FLAT_SHIPPING
        }
    val total: Long get() = cart.subtotal + shipping
    val remainingForFreeShipping: Long
        get() = (FREE_SHIPPING_THRESHOLD - cart.subtotal).coerceAtLeast(0L)

    companion object {
        const val FREE_SHIPPING_THRESHOLD = 99900L
        const val FLAT_SHIPPING = 4900L
    }
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.observeCart().collect { cart ->
                _uiState.update { it.copy(cart = cart) }
            }
        }
    }

    fun increment(productId: String) {
        val line = _uiState.value.cart.lines.firstOrNull { it.productId == productId } ?: return
        change(productId, line.quantity + 1)
    }

    fun decrement(productId: String) {
        val line = _uiState.value.cart.lines.firstOrNull { it.productId == productId } ?: return
        change(productId, line.quantity - 1)
    }

    private fun change(productId: String, quantity: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val res = cartRepository.setQuantity(productId, quantity)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = (res as? DataResult.Failure)?.message,
                )
            }
        }
    }

    fun remove(productId: String) {
        viewModelScope.launch {
            val res = cartRepository.remove(productId)
            _uiState.update {
                it.copy(message = (res as? DataResult.Failure)?.message ?: "Removed from cart")
            }
        }
    }

    fun clear() {
        viewModelScope.launch { cartRepository.clear() }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}
