package app.tab.store.feature.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tab.store.domain.model.Order
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val cancellingId: String? = null,
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.observeBuyerOrders().collect { orders ->
                _uiState.value = OrdersUiState(orders = orders, isLoading = false)
            }
        }
    }

    fun cancel(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingId = orderId) }
            val res = orderRepository.cancelOrder(orderId)
            _uiState.update {
                it.copy(
                    cancellingId = null,
                    message = when (res) {
                        is DataResult.Failure -> res.message
                        is DataResult.Success -> "Install cancelled"
                    },
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = true,
    val message: String? = null,
    val isCancelling: Boolean = false,
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.observeOrder(orderId).collect { order ->
                _uiState.value = _uiState.value.copy(
                    order = order,
                    isLoading = order == null,
                )
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true) }
            val res = orderRepository.cancelOrder(orderId)
            _uiState.update {
                it.copy(
                    isCancelling = false,
                    message = when (res) {
                        is DataResult.Failure -> res.message
                        is DataResult.Success -> "Install cancelled"
                    },
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }
}

data class OrderPlacedUiState(val order: Order? = null)

@HiltViewModel
class OrderPlacedViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    orderRepository: OrderRepository,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()
    val orderIdValue: String get() = orderId

    private val _uiState = MutableStateFlow(OrderPlacedUiState())
    val uiState: StateFlow<OrderPlacedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.observeOrder(orderId).collect { order ->
                _uiState.value = OrderPlacedUiState(order)
            }
        }
    }
}
