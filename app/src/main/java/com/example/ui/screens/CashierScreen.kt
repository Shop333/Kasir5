package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProductEntity
import com.example.data.local.TransactionWithItems
import com.example.data.model.CartItem
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.LowStockWarmAmber
import com.example.ui.viewmodel.PosViewModel
import com.example.utils.FormatUtils

@Composable
fun CashierScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val totalCartPrice by viewModel.totalCartPrice.collectAsStateWithLifecycle()
    val totalCartItemsCount by viewModel.totalCartItemsCount.collectAsStateWithLifecycle()
    val lastCompletedTx by viewModel.lastCompletedTransaction.collectAsStateWithLifecycle()
    val barcodeErr by viewModel.barcodeErrorMessage.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("Semua") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showBarcodeHelpDialog by remember { mutableStateOf(false) }

    // Observe scan events to make a nice Toast
    LaunchedEffect(viewModel.barcodeScanResult) {
        viewModel.barcodeScanResult.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Filter results locally by category
    val categorisedProducts = remember(searchResults, selectedCategory) {
        if (selectedCategory == "Semua") {
            searchResults
        } else {
            searchResults.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    // Display Low Stock toast if scan error happens
    LaunchedEffect(barcodeErr) {
        barcodeErr?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.dismissBarcodeError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ================= LEFT PANEL: Catalog + Filter Chips =================
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                // Search & Barcode Quick Field Card
                SearchBarAndScannerRow(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onBarcodeScan = { barcode ->
                        val success = viewModel.scanBarcode(barcode)
                        if (!success) {
                            // error is handled by the state observer
                        }
                    },
                    onScannerHelp = { showBarcodeHelpDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Group of Categories Chips
                CategoryChipsSelector(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Catalog Grid
                if (categorisedProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tidak ada produk ditemukan.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categorisedProducts, key = { it.id }) { product ->
                            CatalogProductItemCard(
                                product = product,
                                onClick = { viewModel.addToCart(product) }
                            )
                        }
                    }
                }
            }

            // ================= RIGHT PANEL: Basket Checkout Panel =================
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Keranjang Belanja",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Basket Empty",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Keranjang kosong",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Pilih barang sedia atau scan barcode di sebelah kiri.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cartItems, key = { it.product.id }) { item ->
                            CartItemRow(
                                item = item,
                                onIncrement = { viewModel.addToCart(item.product) },
                                onDecrement = { viewModel.removeOneFromCart(item.product) },
                                onDelete = { viewModel.removeFromCartFully(item.product) }
                            )
                        }
                    }
                }

                // Billing Segment which computes total automatically!
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total ($totalCartItemsCount barang):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatRupiah(totalCartPrice),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showCheckoutDialog = true },
                            enabled = cartItems.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PointOfSale, contentDescription = "Checkout")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bayar Sekarang",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Checkout Payment Dialog
        if (showCheckoutDialog) {
            CheckoutPaymentDialog(
                totalCartPrice = totalCartPrice,
                onDismiss = { showCheckoutDialog = false },
                onPaymentSubmit = { cashAmountInput, cashierName ->
                    viewModel.submitCheckout(cashAmountInput, cashierName)
                    showCheckoutDialog = false
                }
            )
        }

        // Simulation Barcode helpers dialog
        if (showBarcodeHelpDialog) {
            BarcodeSimulatorGuideDialog(
                onDismiss = { showBarcodeHelpDialog = false },
                onSimulateBarcode = { code ->
                    viewModel.scanBarcode(code)
                    showBarcodeHelpDialog = false
                }
            )
        }

        // 58mm Thermal Printer Digital Simulator overlay
        lastCompletedTx?.let { txWithItems ->
            ReceiptPrinterSimulatorDialog(
                transaction = txWithItems,
                onDismiss = { viewModel.dismissReceiptAndResetCart() }
            )
        }
    }
}

@Composable
fun SearchBarAndScannerRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onBarcodeScan: (String) -> Unit,
    onScannerHelp: () -> Unit
) {
    var rawBarcodeText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Product Name and Category Live search
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Cari Beras, Indomie, Gula...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Barcode simulator trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = rawBarcodeText,
                    onValueChange = { rawBarcodeText = it },
                    placeholder = { Text("Ketik Barcode Scanner...") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode", tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (rawBarcodeText.isNotBlank()) {
                                onBarcodeScan(rawBarcodeText.trim())
                                rawBarcodeText = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                IconButton(
                    onClick = onScannerHelp,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Simulate scan help",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChipsSelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("Semua", "Sembako", "Minuman", "Camilan", "Bumbu")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { cat ->
            val isSelected = cat == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(cat) },
                label = { Text(cat) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape
            )
        }
    }
}

@Composable
fun CatalogProductItemCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    val isLowStock = product.stock <= product.lowStockThreshold
    Card(
        elevation = CardDefaults.cardElevation(1.2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = product.stock > 0, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Category tag badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = product.category,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Prices
            Text(
                text = FormatUtils.formatRupiah(product.sellPrice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stock Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stok: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (product.stock == 0) Color.Red else if (isLowStock) AlertOrange else Color.Gray
                )

                if (product.stock == 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Red)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("HABIS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (isLowStock) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "stok tipis",
                        tint = AlertOrange,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${FormatUtils.formatRupiah(item.product.sellPrice)} / pcs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Subtotal: ${FormatUtils.formatRupiah(item.subTotalListPrice)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quick counter controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Decrement button
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Reduce", modifier = Modifier.size(14.dp))
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Increment button checked against maximum stock
                val isMax = item.quantity >= item.product.stock
                IconButton(
                    onClick = onIncrement,
                    enabled = !isMax,
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                }

                // Delete entire item
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun CheckoutPaymentDialog(
    totalCartPrice: Double,
    onDismiss: () -> Unit,
    onPaymentSubmit: (Double, String) -> Unit
) {
    var paymentInputText by remember { mutableStateOf("") }
    var cashierNameInput by remember { mutableStateOf("Kasir Utama") }

    val parsedAmount = paymentInputText.toDoubleOrNull() ?: 0.0
    val changeAmount = parsedAmount - totalCartPrice

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Transaksi Pembayaran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Cashier Name field
                OutlinedTextField(
                    value = cashierNameInput,
                    onValueChange = { cashierNameInput = it },
                    label = { Text("Nama Operator Kasir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Tagihan Anda:", fontSize = 14.sp)
                    Text(
                        text = FormatUtils.formatRupiah(totalCartPrice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cash Received amount
                OutlinedTextField(
                    value = paymentInputText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            paymentInputText = newValue
                        }
                    },
                    label = { Text("Uang Tunai Diterima (Rp)") },
                    placeholder = { Text("Contoh: 100000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick denomination cash suggestions for SPEED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val suggestionList = listOf(totalCartPrice, 50000.0, 100000.0)
                    suggestionList.distinct().forEach { amount ->
                        val label = if (amount == totalCartPrice) "Uang Pas" else FormatUtils.formatRupiah(amount).replace("Rp ", "")
                        Button(
                            onClick = { paymentInputText = amount.toInt().toString() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time calculated change!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kembalian Kasir:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (changeAmount < 0) "Belum Cukup" else FormatUtils.formatRupiah(changeAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (changeAmount < 0) AlertOrange else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = { onPaymentSubmit(parsedAmount, cashierNameInput) },
                        enabled = parsedAmount >= totalCartPrice,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Proses")
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeSimulatorGuideDialog(
    onDismiss: () -> Unit,
    onSimulateBarcode: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Simulasi Pembaca Barcode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pilih produk di bawah untuk mensimulasikan pemindaian laser fisik oleh barcode scanner:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val mockCodes = listOf(
                    Pair("8990024011101", "Beras Setra Ramos 5kg"),
                    Pair("8992696404455", "Minyak Goreng Bimoli 2L"),
                    Pair("8998866100511", "Gula Pasir Gulaku 1kg"),
                    Pair("089686043825", "Satu bungkus Indomie Goreng"),
                    Pair("9999999999999", "Barcode Tidak Valid (Error demo)")
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mockCodes) { mock ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSimulateBarcode(mock.first) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = mock.second, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(text = "Code: ${mock.first}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                }
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Scan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun ReceiptPrinterSimulatorDialog(
    transaction: TransactionWithItems,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Struk Belanja Selesai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated 58mm Thermal paper roll
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCF6)),
                    border = BorderStroke(1.dp, Color.LightGray),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Header
                        Text(
                            text = "SEMBAKO MODERN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Jl. Sejahtera No. 45, Jakarta",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Telp: 0812-3456-7890",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "--------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Tx Meta
                        Text(
                            text = "Nota: #SBM-${transaction.transaction.id}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Waktu: ${FormatUtils.formatDateTime(transaction.transaction.timestamp)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Kasir: ${transaction.transaction.cashierName}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "--------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Cart Rows
                        transaction.items.forEach { item ->
                            val lineTotal = item.sellPrice * item.quantity
                            Text(
                                text = item.productName,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "  ${item.quantity} x ${FormatUtils.formatRupiah(item.sellPrice).replace("Rp ", "")}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(lineTotal).replace("Rp ", ""),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = "--------------------------------------",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Sum Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "TOTAL TAGIHAN:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(text = FormatUtils.formatRupiah(transaction.transaction.totalPrice).replace("Rp ", ""), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "TUNAI:", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = FormatUtils.formatRupiah(transaction.transaction.paymentAmount).replace("Rp ", ""), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "KEMBALIAN:", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(text = FormatUtils.formatRupiah(transaction.transaction.changeAmount).replace("Rp ", ""), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Footer Messages
                        Text(
                            text = "TERIMA KASIH",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "BARANG YANG SUDAH DIBELI",
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "TIDAK DAPAT DITUKAR KEMBALI",
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Mencetak struk lewat Printer Bluetooth...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Print")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak Thermal")
                    }

                    Button(
                        onClick = {
                            shareReceiptViaWhatsApp(context, transaction)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF25D366), contentColor = androidx.compose.ui.graphics.Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "WhatsApp")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kirim WA")
                    }
                }
            }
        }
    }
}

fun shareReceiptViaWhatsApp(context: android.content.Context, transaction: com.example.data.local.TransactionWithItems) {
    val builder = StringBuilder()
    builder.append("========= TOKO =========").append("\n")
    builder.append("    SEMBAKO MODERN").append("\n")
    builder.append("Jl. Sejahtera No. 45, Jakarta").append("\n")
    builder.append("Telp: 0812-3456-7890").append("\n")
    builder.append("========================").append("\n\n")
    builder.append("Nota: #SBM-${transaction.transaction.id}").append("\n")
    builder.append("Waktu: ${com.example.utils.FormatUtils.formatDateTime(transaction.transaction.timestamp)}").append("\n")
    builder.append("Kasir: ${transaction.transaction.cashierName}").append("\n")
    builder.append("------------------------").append("\n")
    transaction.items.forEach { item ->
        val itemTotal = item.sellPrice * item.quantity
        builder.append("${item.productName}").append("\n")
        builder.append("  ${item.quantity} x ${com.example.utils.FormatUtils.formatRupiah(item.sellPrice).replace("Rp ", "")} = ${com.example.utils.FormatUtils.formatRupiah(itemTotal).replace("Rp ", "")}").append("\n")
    }
    builder.append("------------------------").append("\n")
    builder.append("TOTAL TAGIHAN: ${com.example.utils.FormatUtils.formatRupiah(transaction.transaction.totalPrice)}").append("\n")
    builder.append("TUNAI: ${com.example.utils.FormatUtils.formatRupiah(transaction.transaction.paymentAmount)}").append("\n")
    builder.append("KEMBALIAN: ${com.example.utils.FormatUtils.formatRupiah(transaction.transaction.changeAmount)}").append("\n\n")
    builder.append("========================").append("\n")
    builder.append("      TERIMA KASIH").append("\n")
    builder.append("========================")

    val textToShare = builder.toString()
    
    val baseIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, textToShare)
        type = "text/plain"
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    
    try {
        val waIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, textToShare)
            type = "text/plain"
            setPackage("com.whatsapp")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(waIntent)
    } catch (e: Exception) {
        try {
            val waBusinessIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, textToShare)
                type = "text/plain"
                setPackage("com.whatsapp.w4b")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(waBusinessIntent)
        } catch (e2: Exception) {
            try {
                val chooserIntent = android.content.Intent.createChooser(baseIntent, "Kirim Struk via:")
                chooserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            } catch (e3: Exception) {
                e3.printStackTrace()
                Toast.makeText(context, "Tidak ada aplikasi pencari / pembagi yang didukung", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
