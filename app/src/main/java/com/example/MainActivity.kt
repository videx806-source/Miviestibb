package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
                // Header Panel
                HeaderComponent()

                // Search Panel
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChanged = { viewModel.setQuery(it) }
                )

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

@Composable
fun HeaderComponent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
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
            modifier = Modifier
                .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NATIVO",
                color = Color.Red,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.5.sp
            )
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
fun LiveStreamsTab(
    viewModel: MediaViewModel,
    searchQuery: String
) {
    val filteredEvents = remember(searchQuery) {
        viewModel.vivoEvents.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    if (filteredEvents.isEmpty()) {
        EmptyStateComponent(message = "No se encontraron eventos en vivo que coincidan con la búsqueda.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
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

@Composable
fun ChannelsTab(
    viewModel: MediaViewModel,
    searchQuery: String
) {
    val filteredCanales = remember(searchQuery) {
        viewModel.canales.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    if (filteredCanales.isEmpty()) {
        EmptyStateComponent(message = "No se encontraron canales que coincidan con la búsqueda.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
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

@Composable
fun MoviesTab(
    viewModel: MediaViewModel,
    searchQuery: String
) {
    val filteredMovies = remember(searchQuery) {
        viewModel.peliculas.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    if (filteredMovies.isEmpty()) {
        EmptyStateComponent(message = "No se encontraron películas o series que coincidan con la búsqueda.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
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

                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Boton reproducir",
                        tint = Color.White,
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(27.dp))
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
