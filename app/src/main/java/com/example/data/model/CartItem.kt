package com.example.data.model

import com.example.data.local.ProductEntity

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
) {
    val subTotalListPrice: Double
        get() = product.sellPrice * quantity

    val subTotalBuyPrice: Double
        get() = product.buyPrice * quantity
}
