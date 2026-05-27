package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProductEntity
import com.example.ui.theme.AlertOrange
import com.example.ui.viewmodel.PosViewModel
import com.example.utils.FormatUtils

@Composable
fun ProductScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    var showUpsertDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var showConfirmDeleteDialog by remember { mutableStateOf<ProductEntity?>(null) }

    // Filter products locally by search query
    val filteredProducts = remember(allProducts, searchQuery) {
        if (searchQuery.isBlank()) {
            allProducts
        } else {
            allProducts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.barcode.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manajemen Barang",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Kelola katalog produk sembako, stok, dan rentang harga jual/beli.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add button to trigger Add Dialog
                Button(
                    onClick = {
                        selectedProductForEdit = null
                        showUpsertDialog = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Insert")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Barang")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick search items
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Ketik nama produk, barcode, atau kategori untuk filter...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "No Products",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Katalog kosong.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gunakan tombol + Tambah Barang untuk memasukkan data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemRowCard(
                            product = product,
                            onEditClick = {
                                selectedProductForEdit = product
                                showUpsertDialog = true
                            },
                            onDeleteClick = {
                                showConfirmDeleteDialog = product
                            }
                        )
                    }
                }
            }
        }

        // Add/Edit Dialog popup
        if (showUpsertDialog) {
            ProductUpsertDialog(
                productEntity = selectedProductForEdit,
                onDismiss = { showUpsertDialog = false },
                onSave = { savedProduct ->
                    viewModel.saveProduct(savedProduct)
                    showUpsertDialog = false
                    Toast.makeText(
                        context,
                        if (selectedProductForEdit == null) "Berhasil menambah produk baru!" else "Data produk berhasil diubah!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        // Delete confirmation
        showConfirmDeleteDialog?.let { productToDelete ->
            AlertDialog(
                onDismissRequest = { showConfirmDeleteDialog = null },
                title = { Text("Hapus Produk Sembako?") },
                text = { Text("Anda yakin ingin menghapus '${productToDelete.name}'? Seluruh data stok dan riwayat akan terpengaruh.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            viewModel.deleteProduct(productToDelete)
                            showConfirmDeleteDialog = null
                            Toast.makeText(context, "Produk berhasil dihapus", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Hapus", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDeleteDialog = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Floating Action Button for modern visual consistency (Material 3)
        FloatingActionButton(
            onClick = {
                selectedProductForEdit = null
                showUpsertDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Barang")
        }
    }
}

@Composable
fun ProductItemRowCard(
    product: ProductEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isLowStock = product.stock <= product.lowStockThreshold
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Main info columns
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = product.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Barcode: ${product.barcode}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Beli: ${FormatUtils.formatRupiah(product.buyPrice)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Jual: ${FormatUtils.formatRupiah(product.sellPrice)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Stok: ${product.stock}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.stock == 0) Color.Red else if (isLowStock) AlertOrange else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Quick actions column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEditClick,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductUpsertDialog(
    productEntity: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var rawBarcodeText by remember { mutableStateOf(productEntity?.barcode ?: "") }
    var rawNameText by remember { mutableStateOf(productEntity?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(productEntity?.category ?: "Sembako") }
    var rawBuyPriceText by remember { mutableStateOf(productEntity?.buyPrice?.toInt()?.toString() ?: "") }
    var rawSellPriceText by remember { mutableStateOf(productEntity?.sellPrice?.toInt()?.toString() ?: "") }
    var rawStockText by remember { mutableStateOf(productEntity?.stock?.toString() ?: "") }
    var rawThresholdText by remember { mutableStateOf(productEntity?.lowStockThreshold?.toString() ?: "5") }

    val categoriesList = listOf("Sembako", "Minuman", "Camilan", "Bumbu", "Lainnya")
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = if (productEntity == null) "Tambah Produk Baru" else "Ubah Detail Produk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Barcode Code
                item {
                    OutlinedTextField(
                        value = rawBarcodeText,
                        onValueChange = { rawBarcodeText = it },
                        label = { Text("Kode Barcode") },
                        placeholder = { Text("Contoh: 89902345...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Name text
                item {
                    OutlinedTextField(
                        value = rawNameText,
                        onValueChange = { rawNameText = it },
                        label = { Text("Nama Produk Sembako") },
                        placeholder = { Text("Contoh: Beras Ramos 5kg...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category dropdown selection
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Barang") },
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            categoriesList.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Price Rows
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = rawBuyPriceText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) rawBuyPriceText = it },
                            label = { Text("Harga Beli (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rawSellPriceText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) rawSellPriceText = it },
                            label = { Text("Harga Jual (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Stock rows + thresholds
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = rawStockText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) rawStockText = it },
                            label = { Text("Jumlah Stok") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rawThresholdText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) rawThresholdText = it },
                            label = { Text("Batas Tipis") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Actions Segment
                item {
                    Spacer(modifier = Modifier.height(8.dp))
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

                        val isFormValid = rawBarcodeText.isNotBlank() &&
                                rawNameText.isNotBlank() &&
                                rawBuyPriceText.isNotBlank() &&
                                rawSellPriceText.isNotBlank() &&
                                rawStockText.isNotBlank()

                        Button(
                            onClick = {
                                if (isFormValid) {
                                    val newProduct = ProductEntity(
                                        id = productEntity?.id ?: 0,
                                        barcode = rawBarcodeText.trim(),
                                        name = rawNameText.trim(),
                                        category = selectedCategory,
                                        buyPrice = rawBuyPriceText.toDoubleOrNull() ?: 0.0,
                                        sellPrice = rawSellPriceText.toDoubleOrNull() ?: 0.0,
                                        stock = rawStockText.toIntOrNull() ?: 0,
                                        lowStockThreshold = rawThresholdText.toIntOrNull() ?: 5
                                    )
                                    onSave(newProduct)
                                }
                            },
                            enabled = isFormValid,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}
