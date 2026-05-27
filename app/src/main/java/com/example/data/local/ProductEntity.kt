package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val category: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Int,
    val lowStockThreshold: Int = 5
)
