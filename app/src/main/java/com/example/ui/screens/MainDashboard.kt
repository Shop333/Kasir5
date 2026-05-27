package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.PosViewModel

sealed class Screen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("Dashboard", Icons.Default.Dashboard)
    object Cashier : Screen("Kasir (POS)", Icons.Default.PointOfSale)
    object Inventory : Screen("Produk (CRUD)", Icons.Default.Inventory)
    object Orders : Screen("Riwayat", Icons.Default.ReceiptLong)
    object PrinterGuide : Screen("Printer BT", Icons.Default.Print)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: PosViewModel
) {
    var activeScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sembako Modern POS",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "OFFLINE OK",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Render Bottom Navigation on narrow mobile screens
            if (!isTablet) {
                NavigationBar {
                    val navigationItems = listOf(
                        Screen.Dashboard,
                        Screen.Cashier,
                        Screen.Inventory,
                        Screen.Orders,
                        Screen.PrinterGuide
                    )
                    navigationItems.forEach { screen ->
                        NavigationBarItem(
                            selected = activeScreen == screen,
                            onClick = { activeScreen = screen },
                            icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Render Navigation Rail on wide tablet setups (landscape / smart counters!)
            if (isTablet) {
                NavigationRail(
                    header = {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 16.dp)
                        )
                    },
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    val railItems = listOf(
                        Screen.Dashboard,
                        Screen.Cashier,
                        Screen.Inventory,
                        Screen.Orders,
                        Screen.PrinterGuide
                    )
                    railItems.forEach { screen ->
                        NavigationRailItem(
                            selected = activeScreen == screen,
                            onClick = { activeScreen = screen },
                            icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, maxLines = 1) }
                        )
                    }
                }
            }

            // Main screen display pane
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (activeScreen) {
                    is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                    is Screen.Cashier -> CashierScreen(viewModel = viewModel)
                    is Screen.Inventory -> ProductScreen(viewModel = viewModel)
                    is Screen.Orders -> TransactionsScreen(viewModel = viewModel)
                    is Screen.PrinterGuide -> BluetoothPrinterScreen()
                }
            }
        }
    }
}
