package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ProductEntity::class, TransactionEntity::class, TransactionItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sembako_pos_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialProducts(database.productDao())
                }
            }
        }

        private suspend fun populateInitialProducts(productDao: ProductDao) {
            val initialProducts = listOf(
                ProductEntity(
                    barcode = "8990024011101",
                    name = "Beras Setra Ramos Premium 5kg",
                    category = "Sembako",
                    buyPrice = 72000.0,
                    sellPrice = 82000.0,
                    stock = 25,
                    lowStockThreshold = 5
                ),
                ProductEntity(
                    barcode = "8992696404455",
                    name = "Minyak Goreng Bimoli Refill 2L",
                    category = "Sembako",
                    buyPrice = 33000.0,
                    sellPrice = 38500.0,
                    stock = 42,
                    lowStockThreshold = 8
                ),
                ProductEntity(
                    barcode = "8998866100511",
                    name = "Gula Pasir Gulaku Premium 1kg",
                    category = "Sembako",
                    buyPrice = 14500.0,
                    sellPrice = 17500.0,
                    stock = 3, // Low stock on purpose to trigger low stock alerts instantly!
                    lowStockThreshold = 5
                ),
                ProductEntity(
                    barcode = "089686043825",
                    name = "Indomie Goreng Spesial 85g",
                    category = "Camilan",
                    buyPrice = 2800.0,
                    sellPrice = 3500.0,
                    stock = 120,
                    lowStockThreshold = 20
                ),
                ProductEntity(
                    barcode = "8999999042501",
                    name = "Susu Kental Manis Frisian Flag 370g",
                    category = "Minuman",
                    buyPrice = 10500.0,
                    sellPrice = 12500.0,
                    stock = 2, // Low stock alert!
                    lowStockThreshold = 6
                ),
                ProductEntity(
                    barcode = "8992002110298",
                    name = "Teh Celup Sariwangi isi 25",
                    category = "Minuman",
                    buyPrice = 5200.0,
                    sellPrice = 6900.0,
                    stock = 30,
                    lowStockThreshold = 5
                ),
                ProductEntity(
                    barcode = "8990022405018",
                    name = "Telur Ayam Negeri isi 10 (1kg)",
                    category = "Sembako",
                    buyPrice = 24000.0,
                    sellPrice = 28000.0,
                    stock = 15,
                    lowStockThreshold = 5
                ),
                ProductEntity(
                    barcode = "8991001120224",
                    name = "Kecap Manis Bango Botol 135ml",
                    category = "Bumbu",
                    buyPrice = 8000.0,
                    sellPrice = 10000.0,
                    stock = 18,
                    lowStockThreshold = 4
                ),
                ProductEntity(
                    barcode = "8991389221142",
                    name = "Garam Dapur Cap Kapal 250g",
                    category = "Bumbu",
                    buyPrice = 2000.0,
                    sellPrice = 3000.0,
                    stock = 50,
                    lowStockThreshold = 5
                ),
                ProductEntity(
                    barcode = "7622210813735",
                    name = "Biskuit Oreo Vanilla 133g",
                    category = "Camilan",
                    buyPrice = 6800.0,
                    sellPrice = 8500.0,
                    stock = 35,
                    lowStockThreshold = 5
                )
            )

            for (product in initialProducts) {
                productDao.insertProduct(product)
            }
        }
    }
}
