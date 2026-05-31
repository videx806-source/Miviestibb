package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaItemEntity
import com.example.ui.MediaViewModel
import com.example.ui.VideoItem
import com.example.ui.VideoPlayerView
import com.example.ui.theme.VidexTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Full Edge to Edge
        enableEdgeToEdge()

        setContent {
            VidexTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MediaViewModel) {
    val appReady by viewModel.appReady.collectAsState()
    val loadStatus by viewModel.loadStatus.collectAsState()
    val blockingUpdate by viewModel.blockingUpdate.collectAsState()
    val nonBlockingUpdate by viewModel.nonBlockingUpdate.collectAsState()
    val systemMessage by viewModel.systemMessage.collectAsState()
    val isSmsDialogOpen by viewModel.isSmsDialogOpen.collectAsState()

    val activeTab by viewModel.activeTab.collectAsState()
    val subTab by viewModel.subTab.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    
    val downloadingItemId by viewModel.downloadingItemId.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    
    val recordingsList by viewModel.recordings.collectAsState()
    val downloadsList by viewModel.downloads.collectAsState()

    val connectivityMode by viewModel.connectivityMode.collectAsState()
    val isVerifying by viewModel.isVerifyingConnection.collectAsState()

    if (!appReady) {
        InitialSplashScreen(
            status = loadStatus,
            onRetry = { viewModel.cargarTodo() }
        )
    } else if (blockingUpdate != null) {
        BlockingUpdateScreen(blockingUpdate!!)
    } else {
        if (selectedVideo != null) {
            val video = selectedVideo!!
            val mins = recordingSeconds / 60
            val secs = recordingSeconds % 60
            val recTimeString = String.format("%02d:%02d", mins, secs)

            VideoPlayerView(
                streamUrl = video.streamUrl,
                title = video.title,
                isRecording = isRecording,
                recordingTime = recTimeString,
                onToggleRecording = { viewModel.toggleRecording() },
                onClose = { viewModel.setSelectedVideo(null) }
            )
        } else {
            Scaffold(
                bottomBar = {
                    BottomNavBar(
                        activeTab = activeTab,
                        onTabSelected = { viewModel.setActiveTab(it) }
                    )
                },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                // Header Panel with Connection Simulator Toggle
                HeaderComponent(viewModel)

                // Search Panel
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChanged = { viewModel.setQuery(it) }
                )

                // Connectivity state loading spinner overlay
                if (isVerifying) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Comprobando conexión con servidor central...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    // Tab Routing Content
                    when (activeTab) {
                        "vivo" -> LiveStreamsTab(
                            viewModel = viewModel,
                            searchQuery = searchQuery
                        )
                        "canal" -> ChannelsTab(
                            viewModel = viewModel,
                            searchQuery = searchQuery
                        )
                        "pelicula" -> MoviesTab(
                            viewModel = viewModel,
                            searchQuery = searchQuery
                        )
                        "grabaciones" -> GrabacionesAndDescargasTab(
                            viewModel = viewModel,
                            subTab = subTab,
                            onSubTabSelected = { viewModel.setSubTab(it) },
                            recordings = recordingsList,
                            downloads = downloadsList,
                            searchQuery = searchQuery
                        )
                    }
                }
            }

            // Real simulated download dialogue
            if (downloadingItemId != null) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = { Text("Descargando contenido...", color = Color.White) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Almacenando fragmentos multimedia de forma segura en almacenamiento local de la aplicación...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            LinearProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "${(downloadProgress * 100).toInt()}% completado",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

            if (isSmsDialogOpen && systemMessage != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.setSmsDialogOpen(false) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.setSmsDialogOpen(false) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ENTENDIDO")
                        }
                    },
                    icon = {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Aviso", tint = MaterialTheme.colorScheme.primary)
                    },
                    title = { Text("Aviso de VIDEX") },
                    text = { Text(systemMessage!!) },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // IPTV Sources Sourcing control overlay Dialog
            val sourcesDialogOpened by viewModel.sourcesDialogOpened.collectAsState()
            val isSyncing by viewModel.isSyncing.collectAsState()
            val syncLogs by viewModel.syncLogs.collectAsState()
            val activeSourcePresetName by viewModel.activeSourcePresetName.collectAsState()
            var m3uInputText by remember { mutableStateOf("") }
            var tabSelectedPresetOrM3u by remember { mutableStateOf(0) } // 0: Presets, 1: M3U Manual

            if (sourcesDialogOpened) {
                AlertDialog(
                    onDismissRequest = { if (!isSyncing) viewModel.setSourcesDialogOpened(false) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.setSourcesDialogOpened(false) },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ENTENDIDO")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            "📁 GESTIÓN DE FUENTES IPTV",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) {
                            Text(
                                "Elige un servidor sincronizado o introduce un enlace / lista IPTV en formato estándar M3U.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )

                            // Tabs inside Dialog
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (tabSelectedPresetOrM3u == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { tabSelectedPresetOrM3u = 0 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Servidores",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (tabSelectedPresetOrM3u == 0) Color.White else Color.Gray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (tabSelectedPresetOrM3u == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { tabSelectedPresetOrM3u = 1 }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Importar M3U",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (tabSelectedPresetOrM3u == 1) Color.White else Color.Gray
                                    )
                                }
                            }

                            if (tabSelectedPresetOrM3u == 0) {
                                // Preset Sources List
                                Text(
                                    "Servidores en la Nube Disponibles:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                val presets = listOf("Videx Central Oficial", "IPTV España Pública", "Deportes 24h Premium")
                                presets.forEach { pName ->
                                    val isActive = pName == activeSourcePresetName
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable(enabled = !isSyncing) {
                                                viewModel.syncFromSourcePreset(pName)
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = pName,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = when(pName) {
                                                    "Videx Central Oficial" -> "Grilla nacional de alta calidad integrada."
                                                    "IPTV España Pública" -> "Frecuencias públicas en vivo (RTVE, Mediaset)."
                                                    else -> "Señales especializadas de deportes y motor."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(Color.Green, RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                }
                            } else {
                                // M3U Manual Input Form
                                Text(
                                    "Pega tu lista M3U aquí:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                TextField(
                                    value = m3uInputText,
                                    onValueChange = { m3uInputText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    placeholder = {
                                        Text(
                                            "#EXTM3U\n#EXTINF:-1 group-title=\"Deportes\",Canal De Prueba 1\nhttps://url-stream.m3u8",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Button to fill sample M3U
                                    OutlinedButton(
                                        onClick = {
                                            m3uInputText = """
                                                #EXTM3U
                                                #EXTINF:-1 group-title="Deportes" tvg-logo="https://videx.es/logo.png",Gol TV España (M3U Canales)
                                                https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
                                                #EXTINF:-1 group-title="Cine" tvg-logo="https://videx.es/cine.png",Cine Acción Retro
                                                https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8
                                                #EXTINF:-1 group-title="Generalista",Informativo TV España (M3U)
                                                https://playertest.longtailvideo.com/adaptive/vimeo/282848/playlist.m3u8
                                            """.trimIndent()
                                        },
                                        enabled = !isSyncing,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("CARGAR DEMO", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.parseAndImportM3uContent(m3uInputText)
                                        },
                                        enabled = !isSyncing && m3uInputText.isNotBlank(),
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("IMPORTAR M3U", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Sourcing logs console
                            Text(
                                "Registro de Operación de Enlace:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray,
                                letterSpacing = 0.5.sp
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(syncLogs) { logLine ->
                                        Text(
                                            text = logLine,
                                            color = if (logLine.contains("❌") || logLine.contains("⚠️")) Color.Red
                                                    else if (logLine.contains("✅") || logLine.contains("🎉") || logLine.contains("🌟") || logLine.contains("⚡")) Color.Green
                                                    else Color.LightGray,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            if (isSyncing) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Estableciendo canal de syncing...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // QR Sincronización TV overlay Dialog
            val qrLightboxOpen by viewModel.qrLightboxOpen.collectAsState()
            if (qrLightboxOpen) {
                AlertDialog(
                    onDismissRequest = { viewModel.setQrLightboxOpen(false) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.setQrLightboxOpen(false) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CERRAR")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            "📺 VINCULAR SMART TV",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Sincroniza Videx en tu Smart TV. Escanea este código QR con la cámara de tu móvil para vincular tu cuenta y transmitir en tu pantalla.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // High-tech Canvas drawn programmatic Mockup QR Code
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sizePx = size.width
                                    val gridSize = 13
                                    val cellSize = sizePx / gridSize
                                    
                                    // Draw positional markers and random pixels
                                    for (row in 0 until gridSize) {
                                        for (col in 0 until gridSize) {
                                            val isPositionMarker = 
                                                (row < 4 && col < 4) || // Top-left
                                                (row < 4 && col >= gridSize - 4) || // Top-right
                                                (row >= gridSize - 4 && col < 4) // Bottom-left
                                            
                                            if (isPositionMarker) {
                                                val isBorder = row == 0 || row == 3 || col == 0 || col == 3 ||
                                                               row == 0 || row == 3 || col == gridSize - 1 || col == gridSize - 4 ||
                                                               row == gridSize - 1 || row == gridSize - 4 || col == 0 || col == 3
                                                
                                                val isInner = row in 1..2 && col in 1..2 ||
                                                              row in 1..2 && col in (gridSize - 3)..(gridSize - 2) ||
                                                              row in (gridSize - 3)..(gridSize - 2) && col in 1..2
                                                if (isBorder || isInner) {
                                                    drawRect(
                                                        color = Color.Black,
                                                        topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                                    )
                                                }
                                            } else {
                                                val seed = (row * 37 + col * 73) % 4
                                                if (seed == 1 || seed == 3) {
                                                    drawRect(
                                                        color = Color.Black,
                                                        topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Código de Vinculación: 462-VXR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Válido durante 5 minutos",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                )
            }

            // Custom Notification Overlays (Toast style)
            if (toastMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = toastMessage!!,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun HeaderComponent(viewModel: MediaViewModel) {
    val connectivityMode by viewModel.connectivityMode.collectAsState()
    val isVerifying by viewModel.isVerifyingConnection.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "VIDEX APP",
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Streaming Multicanal • 100% NAcIONAL",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // IPTV Sources Settings Button
            IconButton(
                onClick = { viewModel.setSourcesDialogOpened(true) },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Gestionar Fuentes",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // TV Sync Code Button
            IconButton(
                onClick = { viewModel.setQrLightboxOpen(true) },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Sincronizar Smart TV",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Connection Toggle State Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        if (isVerifying) Color.LightGray.copy(alpha = 0.1f)
                        else if (connectivityMode == "LINEA") Color.Green.copy(alpha = 0.15f)
                        else Color.Red.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !isVerifying) {
                        if (connectivityMode == "LINEA") {
                            viewModel.setConnectivityMode("SIN_CONEXION")
                        } else {
                            viewModel.setConnectivityMode("LINEA")
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isVerifying) Color.LightGray
                            else if (connectivityMode == "LINEA") Color.Green
                            else Color.Red,
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isVerifying) "COMPROBANDO"
                           else if (connectivityMode == "LINEA") "ONLINE"
                           else "SIN CONEXIÓN",
                    color = if (isVerifying) Color.LightGray
                            else if (connectivityMode == "LINEA") Color.Green
                            else Color.Red,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarComponent(query: String, onQueryChanged: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        placeholder = { Text("Buscar partidos, canales, películas...", color = Color.Gray) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Icono buscar",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar consulta",
                        tint = Color.LightGray
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun BottomNavBar(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = activeTab == "vivo",
            onClick = { onTabSelected("vivo") },
            label = { Text("🏆 En Vivo") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Pestaña de eventos en vivo"
                )
            }
        )
        NavigationBarItem(
            selected = activeTab == "canal",
            onClick = { onTabSelected("canal") },
            label = { Text("📺 Canales") },
            icon = {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Pestaña de canales de TV"
                )
            }
        )
        NavigationBarItem(
            selected = activeTab == "pelicula",
            onClick = { onTabSelected("pelicula") },
            label = { Text("🎬 Películas") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Pestaña de películas de cine"
                )
            }
        )
        NavigationBarItem(
            selected = activeTab == "grabaciones",
            onClick = { onTabSelected("grabaciones") },
            label = { Text("🔁 Grabaciones") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Pestaña de local grabaciones"
                )
            }
        )
    }
}

@Composable
fun CategorySelectionRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun OfflineWarningComponent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Sin Conexión",
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "MODO SIN CONEXIÓN",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Contenido no disponible en modo sin conexión. Ve al menú de Grabaciones para reproducir tus descargas locales de forma segura.",
                    color = Color.LightGray.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun LiveStreamsTab(
    viewModel: MediaViewModel,
    searchQuery: String
) {
    val connectivityMode by viewModel.connectivityMode.collectAsState()
    if (connectivityMode == "SIN_CONEXION") {
        OfflineWarningComponent()
        return
    }

    val selectedCategory by viewModel.selectedLiveCategory.collectAsState()
    val categories = listOf("Todos", "Fútbol", "Motor", "Baloncesto")
    val vivoEvents by viewModel.vivoEvents.collectAsState()

    val filteredEvents = remember(vivoEvents, searchQuery, selectedCategory) {
        vivoEvents.filter {
            val matchesQuery = it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "Todos" || it.category == selectedCategory
            matchesQuery && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CategorySelectionRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.setLiveCategory(it) }
        )

        if (filteredEvents.isEmpty()) {
            EmptyStateComponent(message = "No se encontraron eventos en vivo que coincidan con la búsqueda.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredEvents) { event ->
                    MediaCard(
                        video = event,
                        onPlay = { viewModel.setSelectedVideo(event) },
                        onDownload = { viewModel.downloadVideo(event) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelsTab(
    viewModel: MediaViewModel,
    searchQuery: String
) {
    val connectivityMode by viewModel.connectivityMode.collectAsState()
    if (connectivityMode == "SIN_CONEXION") {
        OfflineWarningComponent()
        return
    }

    val selectedCategory by viewModel.selectedChannelCategory.collectAsState()
    val categories = listOf("Todos", "Cine", "Deportes", "Documentales", "Noticias")
    val canales by viewModel.canales.collectAsState()

    val filteredCanales = remember(canales, searchQuery, selectedCategory) {
        canales.filter {
            val matchesQuery = it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "Todos" || it.category == selectedCategory
            matchesQuery && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CategorySelectionRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.setChannelCategory(it) }
        )

        if (filteredCanales.isEmpty()) {
            EmptyStateComponent(message = "No se encontraron canales que coincidan con la búsqueda.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredCanales) { canal ->
                    MediaCard(
                        video = canal,
                        onPlay = { viewModel.setSelectedVideo(canal) },
                        onDownload = { viewModel.downloadVideo(canal) }
                    )
                }
            }
        }
    }
}

@Composable
fun MoviesTab(
    viewModel: MediaViewModel,
    searchQuery: String
) {
    val connectivityMode by viewModel.connectivityMode.collectAsState()
    if (connectivityMode == "SIN_CONEXION") {
        OfflineWarningComponent()
        return
    }

    val selectedCategory by viewModel.selectedMovieCategory.collectAsState()
    val categories = listOf("Todas", "Ciencia Ficción", "Drama", "Animación", "Suspenso")
    val peliculas by viewModel.peliculas.collectAsState()

    val filteredMovies = remember(peliculas, searchQuery, selectedCategory) {
        peliculas.filter {
            val matchesQuery = it.title.contains(searchQuery, ignoreCase = true) ||
                    it.subtitle.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "Todas" || it.category == selectedCategory
            matchesQuery && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CategorySelectionRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.setMovieCategory(it) }
        )

        Box(modifier = Modifier.weight(1f)) {
            if (filteredMovies.isEmpty()) {
                EmptyStateComponent(message = "No se encontraron películas o series que coincidan con la búsqueda.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredMovies) { movie ->
                        MediaCard(
                            video = movie,
                            onPlay = { viewModel.setSelectedVideo(movie) },
                            onDownload = { viewModel.downloadVideo(movie) }
                        )
                    }
                }
            }
        }

        var isDirectExpanded by remember { mutableStateOf(false) }
        var directUrlInput by remember { mutableStateOf("") }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDirectExpanded = !isDirectExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔍 ¿No encuentras tu película?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isDirectExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expandir búsqueda directa de SwPlayer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (isDirectExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Búscala en h5.swplayer.com, copia la URL y pégala aquí para reproducirla o descargarla automáticamente:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = directUrlInput,
                            onValueChange = { directUrlInput = it },
                            placeholder = { Text("https://h5.swplayer.com/...", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Button(
                            onClick = {
                                if (directUrlInput.isNotBlank()) {
                                    viewModel.openDirectSwPlayerUrl(directUrlInput)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("▶ ABRIR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaCard(
    video: VideoItem,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFF222233))
            ) {
                Text(
                    text = video.category.uppercase(),
                    color = Color.White.copy(alpha = 0.05f),
                    fontWeight = FontWeight.Black,
                    fontSize = 54.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = video.year,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Icono rating",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = video.rating,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val isLocked = video.year == "CANDADO"
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PlayArrow,
                        contentDescription = if (isLocked) "Contenido bloqueado" else "Boton reproducir",
                        tint = Color.White,
                        modifier = Modifier
                            .size(54.dp)
                            .background(if (isLocked) Color(0xFFC62828) else MaterialTheme.colorScheme.primary, RoundedCornerShape(27.dp))
                            .padding(12.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.subtitle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.extraInfo,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onDownload,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Descargar video",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Button(
                            onClick = onPlay,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "VER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GrabacionesAndDescargasTab(
    viewModel: MediaViewModel,
    subTab: String,
    onSubTabSelected: (String) -> Unit,
    recordings: List<MediaItemEntity>,
    downloads: List<MediaItemEntity>,
    searchQuery: String
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { onSubTabSelected("grabaciones") },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (subTab == "grabaciones") MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (subTab == "grabaciones") Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Grabaciones (${recordings.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSubTabSelected("descargas") },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (subTab == "descargas") MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (subTab == "descargas") Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Descargas (${downloads.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (subTab == "grabaciones") {
            val filteredRecs = remember(recordings, searchQuery) {
                recordings.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }

            if (filteredRecs.isEmpty()) {
                EmptyStateComponent(
                    message = "Aún no tienes grabaciones en vivo.\n\nPara grabar, inicia cualquier transmisión de canales o eventos deportivos y toca el botón circular rojo de grabación."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRecs) { rec ->
                        OfflineItemCard(
                            title = rec.title,
                            subtitle = rec.subtitle,
                            durationText = "${rec.durationSeconds}s de transmisión guardada",
                            onPlay = {
                                viewModel.setSelectedVideo(
                                    VideoItem(
                                        id = rec.id,
                                        title = rec.title,
                                        subtitle = rec.subtitle,
                                        category = "grabado",
                                        streamUrl = rec.streamUrl
                                    )
                                )
                            },
                            onDelete = { viewModel.deleteRecording(rec.id) }
                        )
                    }
                }
            }
        } else {
            val filteredDownloads = remember(downloads, searchQuery) {
                downloads.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }

            if (filteredDownloads.isEmpty()) {
                EmptyStateComponent(
                    message = "No tienes descargas almacenadas.\n\nPuedes guardar partidos en vivo, programas o películas completas tocando el botón de descarga en las tarjetas de contenido."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDownloads) { dl ->
                        OfflineItemCard(
                            title = dl.title,
                            subtitle = "Guardado localmente en la app",
                            durationText = "Listo para reproducir sin conexión",
                            onPlay = {
                                viewModel.setSelectedVideo(
                                    VideoItem(
                                        id = dl.id,
                                        title = dl.title,
                                        subtitle = "Reproducción Local Offline",
                                        category = "descarga",
                                        streamUrl = dl.streamUrl
                                    )
                                )
                            },
                            onDelete = { viewModel.deleteRecording(dl.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineItemCard(
    title: String,
    subtitle: String,
    durationText: String,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = durationText,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar elemento offline",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onPlay,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("VER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyStateComponent(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Icono informativo",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                color = Color.LightGray.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun InitialSplashScreen(status: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "VIDEX",
                fontWeight = FontWeight.Black,
                fontSize = 54.sp,
                color = Color(0xFFF5C518),
                letterSpacing = 4.sp
            )
            Text(
                text = "PLATAFORMA DE STREAMING",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            if (status == "Sin Conexión") {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Sin conexión",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "VIDEX requiere conexión de red",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5C518))
                ) {
                    Text("🔄 REINTENTAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                CircularProgressIndicator(
                    color = Color(0xFFF5C518),
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                text = "v2.3.3",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun BlockingUpdateScreen(update: com.example.ui.UpdateInfo) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "VIDEX",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color(0xFFF5C518)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Update status",
                tint = Color(0xFFF5C518),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = update.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = update.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            if (update.cambios.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Novedades:",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFF5C518),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        update.cambios.forEach { cambio ->
                            Text(
                                text = "• $cambio",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { /* Download */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5C518)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⬇ DESCARGAR APK",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Versión actual: v2.3.3  →  Requerida: v${update.versionMinima}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun UpdateBanner(update: com.example.ui.UpdateInfo, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5C518)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⬆️ ¡Nueva versión v${update.versionActual} disponible!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = update.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black.copy(alpha = 0.8f)
                )
            }
            Row {
                TextButton(onClick = { /* Download */ }) {
                    Text("Descargar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Black)
                }
            }
        }
    }
}

