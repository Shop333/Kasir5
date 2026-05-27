package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionWithItems
import com.example.data.model.CartItem
import com.example.data.repository.PosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ProfitLossSummary(
    val revenue: Double = 0.0,    // Total Penjualan
    val costOfGoods: Double = 0.0, // Total HPP (Harga Beli)
    val netProfit: Double = 0.0    // Laba Bersih
)

data class TopProductStats(
    val productName: String,
    val category: String,
    val unitsSold: Int,
    val revenue: Double
)

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    // --- State: Product Management ---
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<ProductEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allProducts
            } else {
                repository.searchProducts(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- State: Shopping Cart POS ---
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Automatic Real-Time Calculations!
    val totalCartPrice: StateFlow<Double> = _cartItems
        .map { list -> list.sumOf { it.subTotalListPrice } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCartItemsCount: StateFlow<Int> = _cartItems
        .map { list -> list.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addToCart(product: ProductEntity) {
        val currentList = _cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }

        if (existingIndex != -1) {
            val existingItem = currentList[existingIndex]
            if (existingItem.quantity < product.stock) {
                currentList[existingIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
            }
        } else {
            if (product.stock > 0) {
                currentList.add(CartItem(product = product, quantity = 1))
            }
        }
        _cartItems.value = currentList
    }

    fun removeOneFromCart(product: ProductEntity) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }

        if (index != -1) {
            val item = currentList[index]
            if (item.quantity > 1) {
                currentList[index] = item.copy(quantity = item.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
        }
        _cartItems.value = currentList
    }

    fun removeFromCartFully(product: ProductEntity) {
        _cartItems.value = _cartItems.value.filterNot { it.product.id == product.id }
    }

    fun updateCartQuantity(product: ProductEntity, quantity: Int) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }

        if (index != -1) {
            if (quantity <= 0) {
                currentList.removeAt(index)
            } else {
                val finalQty = if (quantity > product.stock) product.stock else quantity
                currentList[index] = currentList[index].copy(quantity = finalQty)
            }
        }
        _cartItems.value = currentList
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // --- Barcode Scanner Lookup ---
    private val _barcodeScanResult = MutableSharedFlow<String>(replay = 0)
    val barcodeScanResult: SharedFlow<String> = _barcodeScanResult.asSharedFlow()

    private val _barcodeErrorMessage = MutableStateFlow<String?>(null)
    val barcodeErrorMessage: StateFlow<String?> = _barcodeErrorMessage.asStateFlow()

    fun scanBarcode(barcode: String): Boolean {
        _barcodeErrorMessage.value = null
        var found = false
        viewModelScope.launch {
            val matchedProduct = repository.getProductByBarcode(barcode)
            if (matchedProduct != null) {
                if (matchedProduct.stock > 0) {
                    addToCart(matchedProduct)
                    _barcodeScanResult.emit("Berhasil menambah: ${matchedProduct.name}")
                    found = true
                } else {
                    _barcodeErrorMessage.value = "Stok untuk ${matchedProduct.name} sedang habis!"
                }
            } else {
                _barcodeErrorMessage.value = "Produk dengan barcode $barcode tidak ditemukan!"
            }
        }
        return found
    }

    fun dismissBarcodeError() {
        _barcodeErrorMessage.value = null
    }

    // --- State: Transactions & Reporting ---
    val allTransactions: StateFlow<List<TransactionWithItems>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered / Aggregated Stats for Profit and Loss (Laporan Laba Rugi)
    val profitLossSummary: StateFlow<ProfitLossSummary> = repository.allTransactions
        .map { transactions ->
            var totalRevenue = 0.0
            var totalHPP = 0.0 // Cost of goods sold (buyPrice * quantity)
            
            for (txWithItems in transactions) {
                totalRevenue += txWithItems.transaction.totalPrice
                for (item in txWithItems.items) {
                    totalHPP += (item.buyPrice * item.quantity)
                }
            }
            ProfitLossSummary(
                revenue = totalRevenue,
                costOfGoods = totalHPP,
                netProfit = totalRevenue - totalHPP
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfitLossSummary())

    // Daily Sales Stats
    val dailyRevenueToday: StateFlow<Double> = repository.allTransactions
        .map { transactions ->
            val calendar = Calendar.getInstance()
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
            val todayYear = calendar.get(Calendar.YEAR)

            transactions.filter { tx ->
                val txCalendar = Calendar.getInstance().apply { timeInMillis = tx.transaction.timestamp }
                txCalendar.get(Calendar.DAY_OF_YEAR) == todayDay && txCalendar.get(Calendar.YEAR) == todayYear
            }.sumOf { it.transaction.totalPrice }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val dailyTransactionsTodayCount: StateFlow<Int> = repository.allTransactions
        .map { transactions ->
            val calendar = Calendar.getInstance()
            val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
            val todayYear = calendar.get(Calendar.YEAR)

            transactions.filter { tx ->
                val txCalendar = Calendar.getInstance().apply { timeInMillis = tx.transaction.timestamp }
                txCalendar.get(Calendar.DAY_OF_YEAR) == todayDay && txCalendar.get(Calendar.YEAR) == todayYear
            }.size
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Top Selling Products (Laporan Produk Terlaris)
    val topSellingProducts: StateFlow<List<TopProductStats>> = combine(
        repository.allTransactions,
        repository.allProducts
    ) { transactions, products ->
        val salesMap = mutableMapOf<Int, Int>() // ProductId -> Units Sold
        val revenueMap = mutableMapOf<Int, Double>() // ProductId -> Revenue
        val productMap = products.associateBy { it.id }

        for (txWithItems in transactions) {
            for (item in txWithItems.items) {
                val pId = item.productId
                salesMap[pId] = (salesMap[pId] ?: 0) + item.quantity
                revenueMap[pId] = (revenueMap[pId] ?: 0.0) + (item.sellPrice * item.quantity)
            }
        }

        salesMap.entries.map { entry ->
            val pId = entry.key
            val product = productMap[pId]
            val productName = product?.name ?: "Barang #${pId}"
            val category = product?.category ?: "Umum"
            TopProductStats(
                productName = productName,
                category = category,
                unitsSold = entry.value,
                revenue = revenueMap[pId] ?: 0.0
            )
        }.sortedByDescending { it.unitsSold }.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Last Checkout Receipt Print Preview State
    private val _lastCompletedTransaction = MutableStateFlow<TransactionWithItems?>(null)
    val lastCompletedTransaction: StateFlow<TransactionWithItems?> = _lastCompletedTransaction.asStateFlow()

    fun dismissReceiptAndResetCart() {
        _lastCompletedTransaction.value = null
        clearCart()
    }

    // --- CRUD Operations called by UI ---
    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            if (product.id == 0) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // --- Checkout Logic ---
    fun submitCheckout(paymentAmount: Double, cashierName: String = "Kasir Utama") {
        val total = _cartItems.value.sumOf { it.subTotalListPrice }
        val change = paymentAmount - total
        if (change < 0) return

        viewModelScope.launch {
            val cartListSnapshot = _cartItems.value
            val txId = repository.insertCheckoutTransaction(
                totalPrice = total,
                paymentAmount = paymentAmount,
                changeAmount = change,
                cartItems = cartListSnapshot,
                cashierName = cashierName
            )

            if (txId != -1L) {
                // Construct a temporary TransactionWithItems for immediate receipt preview
                val completedTx = TransactionWithItems(
                    transaction = com.example.data.local.TransactionEntity(
                        id = txId,
                        totalPrice = total,
                        paymentAmount = paymentAmount,
                        changeAmount = change,
                        cashierName = cashierName
                    ),
                    items = cartListSnapshot.map { item ->
                        com.example.data.local.TransactionItemEntity(
                            transactionId = txId,
                            productId = item.product.id,
                            productName = item.product.name,
                            sellPrice = item.product.sellPrice,
                            buyPrice = item.product.buyPrice,
                            quantity = item.quantity
                        )
                    }
                )
                _lastCompletedTransaction.value = completedTx
            }
        }
    }
}
