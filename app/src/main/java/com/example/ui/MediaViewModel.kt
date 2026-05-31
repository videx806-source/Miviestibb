package com.example.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MediaItemEntity
import com.example.data.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class VideoItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val category: String, // "vivo", "canal", "pelicula"
    val streamUrl: String,
    val thumbnailUrl: String = "",
    val extraInfo: String = "",
    val rating: String = "4.5",
    val year: String = "2024"
)

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MediaRepository
    val recordings: StateFlow<List<MediaItemEntity>>
    val downloads: StateFlow<List<MediaItemEntity>>

    init {
        val mediaDao = AppDatabase.getDatabase(application).mediaDao()
        repository = MediaRepository(mediaDao)
        recordings = repository.allRecs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        downloads = repository.allDownloads.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // UI state
    private val _activeTab = MutableStateFlow("vivo") // "vivo", "canal", "pelicula", "grabaciones"
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _subTab = MutableStateFlow("grabaciones") // "grabaciones", "descargas"
    val subTab: StateFlow<String> = _subTab.asStateFlow()

    private val _selectedVideo = MutableStateFlow<VideoItem?>(null)
    val selectedVideo: StateFlow<VideoItem?> = _selectedVideo.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    // Downloading Simulation State
    private val _downloadingItemId = MutableStateFlow<String?>(null)
    val downloadingItemId: StateFlow<String?> = _downloadingItemId.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private var recordingHandler: Handler? = null
    private var recordingRunnable: Runnable? = null

    // Sample Data
    val vivoEvents = listOf(
        VideoItem(
            id = "v1",
            title = "Final Champions League",
            subtitle = "Sigue el gran encuentro en vivo",
            category = "vivo",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            extraInfo = "14.5k Espectadores • Deportes",
            rating = "4.9",
            year = "HOY"
        ),
        VideoItem(
            id = "v2",
            title = "Fórmula 1: GP Mónaco",
            subtitle = "Clasificación en directo",
            category = "vivo",
            streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            extraInfo = "8.2k Espectadores • Carrera",
            rating = "4.8",
            year = "HOY"
        ),
        VideoItem(
            id = "v3",
            title = "NBA Playoffs: Lakers vs Celtics",
            subtitle = "Transmisión oficial de las finales",
            category = "vivo",
            streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            extraInfo = "11.1k Espectadores • Baloncesto",
            rating = "4.7",
            year = "HOY"
        )
    )

    val canales = listOf(
        VideoItem(
            id = "c1",
            title = "HBO Cine HD",
            subtitle = "Los mejores Blockbusters del cine",
            category = "canal",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            extraInfo = "Deportes y Cine • Calidad FHD",
            rating = "4.6",
            year = "24 Horas"
        ),
        VideoItem(
            id = "c2",
            title = "ESPN Deportes",
            subtitle = "Todo el análisis y partidos mundiales",
            category = "canal",
            streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            extraInfo = "Fútbol y NFL • En Vivo",
            rating = "4.8",
            year = "24 Horas"
        ),
        VideoItem(
            id = "c3",
            title = "National Geographic",
            subtitle = "Exploración y maravillas de la tierra",
            category = "canal",
            streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            extraInfo = "Ciencia y Aventura • FHD",
            rating = "4.5",
            year = "24 Horas"
        ),
        VideoItem(
            id = "c4",
            title = "Canal de Noticias 24h",
            subtitle = "Conectados a la realidad mundial",
            category = "canal",
            streamUrl = "https://playertest.longtailvideo.com/adaptive/vimeo/282848/playlist.m3u8",
            extraInfo = "Informativo • Minuto a minuto",
            rating = "4.1",
            year = "24 Horas"
        )
    )

    val peliculas = listOf(
        VideoItem(
            id = "p1",
            title = "Dune: Part Two",
            subtitle = "Viaje legendario de Paul Atreides",
            category = "pelicula",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            extraInfo = "Fantasía/Aventura • 2h 46m",
            rating = "4.9",
            year = "2024"
        ),
        VideoItem(
            id = "p2",
            title = "Oppenheimer",
            subtitle = "La historia detrás de la bomba nuclear",
            category = "pelicula",
            streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            extraInfo = "Drama/Historia • 3h 0m",
            rating = "4.8",
            year = "2023"
        ),
        VideoItem(
            id = "p3",
            title = "Spider-Man: Across the Spider-Verse",
            subtitle = "Miles Morales cruza el multiverso",
            category = "pelicula",
            streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            extraInfo = "Animación/Aventura • 2h 20m",
            rating = "4.9",
            year = "2023"
        ),
        VideoItem(
            id = "p4",
            title = "The Last of Us (S1)",
            subtitle = "Brote de hongo destruye la sociedad",
            category = "pelicula",
            streamUrl = "https://playertest.longtailvideo.com/adaptive/vimeo/282848/playlist.m3u8",
            extraInfo = "Suspenso/Acción • 9 Episodios",
            rating = "4.7",
            year = "2023"
        )
    )

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun setSubTab(sub: String) {
        _subTab.value = sub
    }

    fun setSelectedVideo(video: VideoItem?) {
        _selectedVideo.value = video
        // Reset recording simulation when player is open/close
        if (video == null && _isRecording.value) {
            stopRecording(save = false)
        }
    }

    fun setQuery(q: String) {
        _searchQuery.value = q
    }

    fun triggerToast(msg: String) {
        _toastMessage.value = msg
        // Dismiss after 3s
        _selectedVideo.value?.let {
            // Keep player alive, just clear toast
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }, 3000)
    }

    // Toggle simulated recording for currently playing video
    fun toggleRecording() {
        val cur = _selectedVideo.value ?: return
        if (_isRecording.value) {
            stopRecording(save = true)
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        _recordingSeconds.value = 0
        triggerToast("🔴 Grabación iniciada")

        recordingHandler = Handler(Looper.getMainLooper())
        recordingRunnable = object : Runnable {
            override fun run() {
                _recordingSeconds.value++
                recordingHandler?.postDelayed(this, 1000)
            }
        }
        recordingHandler?.post(recordingRunnable!!)
    }

    private fun stopRecording(save: Boolean) {
        _isRecording.value = false
        recordingHandler?.removeCallbacks(recordingRunnable ?: return)
        recordingHandler = null
        recordingRunnable = null

        val currentVideo = _selectedVideo.value ?: return
        val secs = _recordingSeconds.value
        _recordingSeconds.value = 0

        if (save && secs > 1) {
            viewModelScope.launch {
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val dateStr = formatter.format(Date())
                val recordingEntity = MediaItemEntity(
                    id = "rec_" + UUID.randomUUID().toString(),
                    title = "[G] ${currentVideo.title}",
                    subtitle = "Grabado el $dateStr",
                    category = currentVideo.category,
                    streamUrl = currentVideo.streamUrl,
                    thumbnailUrl = currentVideo.thumbnailUrl,
                    isRecording = true,
                    isDownloaded = false,
                    durationSeconds = secs,
                    localPath = "Almacenamiento Local"
                )
                repository.insert(recordingEntity)
                triggerToast("💾 Grabación guardada ($secs s) en la pestaña Grabaciones.")
            }
        } else {
            triggerToast("🚫 Grabación cancelada (muy corta)")
        }
    }

    // Download content simulation
    fun downloadVideo(video: VideoItem) {
        if (_downloadingItemId.value != null) {
            triggerToast("⚠️ Ya hay una descarga en curso.")
            return
        }

        _downloadingItemId.value = video.id
        _downloadProgress.value = 0f

        val handler = Handler(Looper.getMainLooper())
        var currentProgress = 0f
        val runnable = object : Runnable {
            override fun run() {
                currentProgress += 0.1f
                if (currentProgress >= 1.0f) {
                    _downloadProgress.value = 1.0f
                    _downloadingItemId.value = null
                    
                    // Save in room DB as downloaded
                    viewModelScope.launch {
                        val downloadedEntity = MediaItemEntity(
                            id = "dl_" + video.id,
                            title = "[D] ${video.title}",
                            subtitle = "Descargado listo para sin conexión",
                            category = video.category,
                            streamUrl = video.streamUrl,
                            thumbnailUrl = video.thumbnailUrl,
                            isRecording = false,
                            isDownloaded = true,
                            durationSeconds = 0,
                            localPath = "videx_downloads/${video.id}.mp4"
                        )
                        repository.insert(downloadedEntity)
                        triggerToast("🎬 '${video.title}' se guardó en tu carpeta de descargas.")
                    }
                } else {
                    _downloadProgress.value = currentProgress
                    handler.postDelayed(this, 300)
                }
            }
        }
        handler.post(runnable)
    }

    fun deleteRecording(recId: String) {
        viewModelScope.launch {
            repository.deleteById(recId)
            triggerToast("🗑️ Elemento eliminado permanentemente.")
        }
    }
}
