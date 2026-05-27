package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import androidx.room.withTransaction

class PosRepository(
    private val database: AppDatabase,
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProductsFlow()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProductsFlow()

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return productDao.searchProductsFlow(query)
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        return productDao.getProductByBarcode(barcode)
    }

    suspend fun getProductById(id: Int): ProductEntity? {
        return productDao.getProductById(id)
    }

    suspend fun insertProduct(product: ProductEntity): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    val allTransactions: Flow<List<TransactionWithItems>> = transactionDao.getAllTransactionsWithItemsFlow()

    suspend fun insertCheckoutTransaction(
        totalPrice: Double,
        paymentAmount: Double,
        changeAmount: Double,
        cartItems: List<CartItem>,
        cashierName: String = "Kasir Utama"
    ): Long {
        return database.withTransaction {
            val transactionEntity = TransactionEntity(
                totalPrice = totalPrice,
                paymentAmount = paymentAmount,
                changeAmount = changeAmount,
                cashierName = cashierName
            )
            val newId = transactionDao.insertTransaction(transactionEntity)
            
            val itemEntities = cartItems.map { item ->
                TransactionItemEntity(
                    transactionId = newId,
                    productId = item.product.id,
                    productName = item.product.name,
                    sellPrice = item.product.sellPrice,
                    buyPrice = item.product.buyPrice,
                    quantity = item.quantity
                )
            }
            
            transactionDao.insertTransactionItems(itemEntities)
            
            for (item in cartItems) {
                productDao.decrementStock(item.product.id, item.quantity)
            }
            newId
        }
    }
}
