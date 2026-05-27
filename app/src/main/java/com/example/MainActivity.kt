package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.repository.PosRepository
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.PosViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local SQLite Room Database
        val database = AppDatabase.getDatabase(applicationContext)
        val productDao = database.productDao()
        val transactionDao = database.transactionDao()
        val repository = PosRepository(database, productDao, transactionDao)

        // Instantiate PosViewModel via simple ViewModel Factory DI
        val factory = PosViewModelFactory(repository)
        val posViewModel: PosViewModel by viewModels { factory }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainDashboard(viewModel = posViewModel)
                }
            }
        }
    }
}
