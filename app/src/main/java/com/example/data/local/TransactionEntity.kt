package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalPrice: Double,
    val paymentAmount: Double,
    val changeAmount: Double,
    val cashierName: String = "Kasir Sembako"
)
