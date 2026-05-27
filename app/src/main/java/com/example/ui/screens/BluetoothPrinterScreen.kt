package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.FormatUtils

@Composable
fun BluetoothPrinterScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var isTestingConnection by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Section ---
        Column {
            Text(
                text = "Integrasi Printer Thermal Bluetooth",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Panduan lengkap dan arsitektur integrasi printer POS (Point of Sale) untuk cetak struk kasir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- Simulated Bluetooth Status ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "BT Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(text = "Driver Printer Bluetooth", fontWeight = FontWeight.Bold)
                        Text(text = "Status: Simulasi Dihubungkan (POS-58 Printer)", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Button(
                    onClick = {
                        isTestingConnection = true
                        Toast.makeText(context, "Mulai tes koneksi printer...", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Tes Printer")
                }
            }
        }

        // Simulating printing feedback log
        if (isTestingConnection) {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "CONSOL PRINT LOG (POS-58 TEST):", fontSize = 12.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = { isTestingConnection = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "[BT] Connecting to SPP Port [00001101-...]\n" +
                                "[BT] Success paired with POS-58. Open socket connection.\n" +
                                "[ESC/POS] ESC @ (Init printer)\n" +
                                "[ESC/POS] ESC a 1 (Justify center)\n" +
                                "[PRINT] SEMBAKO MODERN SUCCESS TEST!\n" +
                                "[ESC/POS] ESC d 3 (Feed 3 lines)\n" +
                                "[ESC/POS] Socket flush & closed.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        // --- Core Blueprint Architecture Guide ---
        Divider()

        Text(
            text = "Langkah Integrasi Printer Thermal di Android (Kotlin)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        StepItem(
            number = "1",
            title = "Meminta Izin Bluetooth & Lokasi (Android Manifest)",
            description = "Pada file AndroidManifest.xml, tambahkan izin berikut agar aplikasi diizinkan mendeteksi bluetooth di sekitar:\n" +
                    "• android.permission.BLUETOOTH\n" +
                    "• android.permission.BLUETOOTH_ADMIN\n" +
                    "• android.permission.BLUETOOTH_CONNECT (Android 12+)\n" +
                    "• android.permission.BLUETOOTH_SCAN (Android 12+)"
        )

        StepItem(
            number = "2",
            title = "Mencari UUID SPP Printer",
            description = "Sebagian besar printer thermal mini menggunakan profil Bluetooth Serial Port Profile (SPP) dengan UUID standar:\n" +
                    "00001101-0000-1000-8000-00805F9B34FB\n" +
                    "Gunakan BluetoothDevice.createRfcommSocketToServiceRecord(uuid) untuk mendaftarkan Bluetooth socket stream."
        )

        StepItem(
            number = "3",
            title = "Gunakan Protokol ESC/POS Byte Commands",
            description = "Printer bluetooth mencetak teks melalui byte data standar (ESC/POS).\n" +
                    "• Inisialisasi: ByteArray(3) { 0x1B, 0x40, 0x00 }\n" +
                    "• Rata Tengah: ByteArray(3) { 0x1B, 0x61, 0x01 }\n" +
                    "• Rata Kiri: ByteArray(3) { 0x1B, 0x61, 0x00 }\n" +
                    "• Cetak Tebal: ByteArray(3) { 0x1B, 0x45, 0x01 }\n" +
                    "• Selesai Cetak Tebal: ByteArray(3) { 0x1B, 0x45, 0x00 }\n" +
                    "• Gunting Kertas: ByteArray(4) { 0x1d, 0x56, 0x41, 0x00 }"
        )

        // --- Best Libraries Recommendations ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Recommend, contentDescription = "Rec", tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Rekomendasi Library Printer Sembako Kasir", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                BulletPoint("Flutter: blue_thermal_printer atau esc_pos_utils paling diunggulkan.")
                BulletPoint("Android Native Kotlin: Gunakan Android Bluetooth API dengan custom helper printer, atau pustaka android-bluetooth-thermal-printer.")
                BulletPoint("Pastikan lebar struk dikonfigurasi: POS-58 (lebar maksimum 32 karakter per baris), atau POS-80 (lebar maksumum 48 karakter per baris).")
            }
        }
    }
}

@Composable
fun StepItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
