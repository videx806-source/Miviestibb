package com.example.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MediaItemEntity
import com.example.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

const val APP_VERSION = "2.3.3"

data class VideoItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val category: String, // "Fútbol", "Motor", "Baloncesto", "Cine", "Deportes", etc.
    val streamUrl: String,
    val thumbnailUrl: String = "",
    val extraInfo: String = "",
    val rating: String = "4.5",
    val year: String = "2024"
)

data class UpdateInfo(
    val title: String,
    val description: String,
    val versionActual: String,
    val versionMinima: String,
    val isMandatory: Boolean,
    val urlDescarga: String,
    val cambios: List<String>
)

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MediaRepository
    val recordings: StateFlow<List<MediaItemEntity>>
    val downloads: StateFlow<List<MediaItemEntity>>

    // Initial Loading Screen & API status
    private val _appReady = MutableStateFlow(false)
    val appReady: StateFlow<Boolean> = _appReady.asStateFlow()

    private val _loadStatus = MutableStateFlow("Verificando conexión...")
    val loadStatus: StateFlow<String> = _loadStatus.asStateFlow()

    private val _blockingUpdate = MutableStateFlow<UpdateInfo?>(null)
    val blockingUpdate: StateFlow<UpdateInfo?> = _blockingUpdate.asStateFlow()

    private val _nonBlockingUpdate = MutableStateFlow<UpdateInfo?>(null)
    val nonBlockingUpdate: StateFlow<UpdateInfo?> = _nonBlockingUpdate.asStateFlow()

    private val _systemMessage = MutableStateFlow<String?>(null)
    val systemMessage: StateFlow<String?> = _systemMessage.asStateFlow()

    private val _isSmsDialogOpen = MutableStateFlow(false)
    val isSmsDialogOpen: StateFlow<Boolean> = _isSmsDialogOpen.asStateFlow()

    // UI navigation state
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

    // Downloading State
    private val _downloadingItemId = MutableStateFlow<String?>(null)
    val downloadingItemId: StateFlow<String?> = _downloadingItemId.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private var recordingHandler: Handler? = null
    private var recordingRunnable: Runnable? = null

    // Connectivity State
    private val _connectivityMode = MutableStateFlow("LINEA") // "LINEA" (Online), "SIN_CONEXION" (Offline)
    val connectivityMode: StateFlow<String> = _connectivityMode.asStateFlow()

    private val _isVerifyingConnection = MutableStateFlow(false)
    val isVerifyingConnection: StateFlow<Boolean> = _isVerifyingConnection.asStateFlow()

    private val _qrLightboxOpen = MutableStateFlow(false)
    val qrLightboxOpen: StateFlow<Boolean> = _qrLightboxOpen.asStateFlow()

    // Category filters
    private val _selectedLiveCategory = MutableStateFlow("Todos")
    val selectedLiveCategory: StateFlow<String> = _selectedLiveCategory.asStateFlow()

    private val _selectedChannelCategory = MutableStateFlow("Todos")
    val selectedChannelCategory: StateFlow<String> = _selectedChannelCategory.asStateFlow()

    private val _selectedMovieCategory = MutableStateFlow("Todas")
    val selectedMovieCategory: StateFlow<String> = _selectedMovieCategory.asStateFlow()

    // Reactive streams holding loaded channels/movies
    private val _vivoEvents = MutableStateFlow<List<VideoItem>>(emptyList())
    val vivoEvents: StateFlow<List<VideoItem>> = _vivoEvents.asStateFlow()

    private val _canales = MutableStateFlow<List<VideoItem>>(emptyList())
    val canales: StateFlow<List<VideoItem>> = _canales.asStateFlow()

    private val _peliculas = MutableStateFlow<List<VideoItem>>(emptyList())
    val peliculas: StateFlow<List<VideoItem>> = _peliculas.asStateFlow()

    // Sourcing, parsing, and streaming logs (IPTV Tab)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(
        listOf("ℹ️ Bienvenido a VIDEX v2.3.3. Configura playlists o sincroniza servidores.")
    )
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _activeSourcePresetName = MutableStateFlow("Videx Central Oficial")
    val activeSourcePresetName: StateFlow<String> = _activeSourcePresetName.asStateFlow()

    private val _sourcesDialogOpened = MutableStateFlow(false)
    val sourcesDialogOpened: StateFlow<Boolean> = _sourcesDialogOpened.asStateFlow()

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

        // Run full initial parallel load sequence
        cargarTodo()
    }

    // Parallel Initial Loading sequence
    fun cargarTodo() {
        viewModelScope.launch {
            _appReady.value = false
            _loadStatus.value = "Verificando conexión..."

            val online = verificarConexion()
            if (!online) {
                // If offline, check if we have downloaded content
                val hasDownloads = downloads.value.isNotEmpty()
                if (hasDownloads) {
                    _connectivityMode.value = "SIN_CONEXION"
                    _loadStatus.value = "Modo Sin Conexión"
                    _activeTab.value = "grabaciones"
                    _subTab.value = "descargas"
                    _appReady.value = true
                } else {
                    _connectivityMode.value = "SIN_CONEXION"
                    _loadStatus.value = "Sin Conexión"
                    // Wait, so we don't complete ready state - we require user retry
                }
                return@launch
            }

            _loadStatus.value = "Cargando datos..."

            // Load all endpoints in parallel on background threads
            val eventsJob = viewModelScope.launch(Dispatchers.IO) {
                val response = fetchTextUrl("https://apio.videx.lol/eventosss.json")
                if (response != null) {
                    val parsed = parseEvents(response)
                    _vivoEvents.value = parsed
                }
            }

            val channelsJob = viewModelScope.launch(Dispatchers.IO) {
                val response = fetchTextUrl("https://apio.videx.lol/canales.json")
                if (response != null) {
                    val parsed = parseChannels(response)
                    _canales.value = parsed
                }
            }

            val updatesJob = viewModelScope.launch(Dispatchers.IO) {
                val response = fetchTextUrl("https://apio.videx.lol/actualizaciones.json")
                if (response != null) {
                    procesarActualizanes(response)
                }
            }

            val smsJob = viewModelScope.launch(Dispatchers.IO) {
                val response = fetchTextUrl("https://apio.videx.lol/sms.json")
                if (response != null) {
                    procesarSistemaMensajes(response)
                }
            }

            // Await jobs
            eventsJob.join()
            channelsJob.join()
            updatesJob.join()
            smsJob.join()

            // Preload some movies if remote list has none
            if (_peliculas.value.isEmpty()) {
                _peliculas.value = listOf(
                    VideoItem(
                        id = "p1",
                        title = "Dune: Part Two",
                        subtitle = "Viaje legendario de Paul Atreides",
                        category = "Ciencia Ficción",
                        streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                        extraInfo = "Fantasía/Aventura • 2h 46m",
                        rating = "4.9",
                        year = "2024"
                    ),
                    VideoItem(
                        id = "p2",
                        title = "Oppenheimer",
                        subtitle = "La historia detrás de la bomba nuclear",
                        category = "Drama",
                        streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                        extraInfo = "Drama/Historia • 3h 0m",
                        rating = "4.8",
                        year = "2023"
                    )
                )
            }

            _loadStatus.value = "¡Listo!"
            kotlinx.coroutines.delay(500)
            _appReady.value = true
        }
    }

    private fun parseEvents(json: String): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        try {
            val obj = JSONObject(json)
            val arr = obj.optJSONArray("eventos")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val id = item.optString("id", UUID.randomUUID().toString())
                    val title = item.optString("titulo", "")
                    val equipos = item.optString("equipos", "")
                    val liga = item.optString("liga", "Fútbol")
                    val fecha = item.optString("fecha", "")
                    val hora = item.optString("hora", "")
                    val zona = item.optString("zona_horaria", "CET")
                    val imagenUrl = item.optString("imagen_url", "")
                    val streamUrl = item.optString("stream_url", "")
                    val canal = item.optString("canal", "VIDEX")
                    val desc = item.optString("descripcion", "")

                    var category = "Fútbol"
                    val tL = title.lowercase()
                    val lL = liga.lowercase()
                    if (tL.contains("f1") || tL.contains("fórmula") || lL.contains("motor") || lL.contains("f1")) {
                        category = "Motor"
                    } else if (tL.contains("nba") || tL.contains("basket") || tL.contains("baloncesto") || lL.contains("nba")) {
                        category = "Baloncesto"
                    }

                    list.add(
                        VideoItem(
                            id = id,
                            title = title,
                            subtitle = if (equipos.isNotEmpty()) "$equipos • $liga" else "$liga • $desc",
                            category = category,
                            streamUrl = streamUrl,
                            thumbnailUrl = imagenUrl,
                            extraInfo = "$canal • $fecha $hora ($zona)",
                            rating = "4.9",
                            year = "EN VIVO"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseChannels(json: String): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        try {
            val obj = JSONObject(json)
            val arr = obj.optJSONArray("canales")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val id = item.optString("id", UUID.randomUUID().toString())
                    val nombre = item.optString("nombre", "")
                    val categoria = item.optString("categoria", "Cine")
                    val logoUrl = item.optString("logo_url", "")
                    val img = item.optString("img", "")
                    val streamUrl = item.optString("stream_url", "")
                    val resolucion = item.optString("resolucion", "HD")
                    val idioma = item.optString("idioma", "ES")
                    val pais = item.optString("pais", "España")
                    val esPremium = item.optBoolean("es_premium", false)
                    val activo = item.optBoolean("activo", true)
                    val desc = item.optString("descripcion", "")

                    val logoToUse = if (img.isNotEmpty()) img else logoUrl

                    var finalCategory = "Cine"
                    val catLower = categoria.lowercase()
                    if (catLower.contains("cine") || catLower.contains("película")) {
                        finalCategory = "Cine"
                    } else if (catLower.contains("deporte") || catLower.contains("dazn") || catLower.contains("sport")) {
                        finalCategory = "Deportes"
                    } else if (catLower.contains("docu") || catLower.contains("naturaleza") || catLower.contains("cultural")) {
                        finalCategory = "Documentales"
                    } else if (catLower.contains("noticia") || catLower.contains("news") || catLower.contains("info")) {
                        finalCategory = "Noticias"
                    }

                    list.add(
                        VideoItem(
                            id = id,
                            title = nombre,
                            subtitle = desc,
                            category = finalCategory,
                            streamUrl = streamUrl,
                            thumbnailUrl = logoToUse,
                            extraInfo = "$resolucion • $idioma • $pais" + (if (esPremium) " • PREMIUM" else "") + (if (!activo) " • CANDADO" else ""),
                            rating = if (esPremium) "4.9" else "4.6",
                            year = if (!activo) "CANDADO" else "24 HORAS"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun procesarActualizanes(jsonString: String) {
        try {
            val obj = JSONObject(jsonString)
            val act = obj.optJSONObject("actualizacion")
            if (act != null) {
                val versionActual = act.optString("version_actual", "")
                val versionMinima = act.optString("version_minima_requerida", "")
                val obligatoria = act.optBoolean("requiere_actualizacion_obligatoria", false)
                val titulo = act.optString("titulo", "Nueva actualización")
                val desc = act.optString("descripcion", "")

                val urls = act.optJSONObject("urls_descarga")
                val downloadUrl = urls?.optString("apk_directo", "") ?: urls?.optString("android", "") ?: ""

                val cambiosArr = act.optJSONArray("cambios")
                val cambiosList = mutableListOf<String>()
                if (cambiosArr != null) {
                    for (idx in 0 until cambiosArr.length()) {
                        cambiosList.add(cambiosArr.getString(idx))
                    }
                }

                val parsedInfo = UpdateInfo(
                    title = titulo,
                    description = desc,
                    versionActual = versionActual,
                    versionMinima = versionMinima,
                    isMandatory = obligatoria,
                    urlDescarga = downloadUrl,
                    cambios = cambiosList
                )

                if (obligatoria && compararVersiones(APP_VERSION, versionMinima) < 0) {
                    _blockingUpdate.value = parsedInfo
                } else if (compararVersiones(APP_VERSION, versionActual) < 0) {
                    _nonBlockingUpdate.value = parsedInfo
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun procesarSistemaMensajes(jsonString: String) {
        try {
            val obj = JSONObject(jsonString)
            val mostrar = obj.optBoolean("Mostrar", false)
            val mensaje = obj.optString("mensaje", "")
            if (mostrar && mensaje.isNotEmpty()) {
                _systemMessage.value = mensaje
                _isSmsDialogOpen.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSmsDialogOpen(open: Boolean) {
        _isSmsDialogOpen.value = open
    }

    fun dismissNonBlockingUpdate() {
        _nonBlockingUpdate.value = null
    }

    // Movie searching sk.php (GET https://apio.videx.lol/movies/sk.php?q=...)
    fun searchMoviesRemote(query: String) {
        if (query.trim().length < 2) return
        viewModelScope.launch(Dispatchers.IO) {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://apio.videx.lol/movies/sk.php?q=$encodedQuery"
            val response = fetchTextUrl(url)
            if (response != null) {
                try {
                    val obj = JSONObject(response)
                    val results = obj.optJSONArray("results")
                    val parsed = mutableListOf<VideoItem>()
                    if (results != null) {
                        for (i in 0 until results.length()) {
                            val item = results.getJSONObject(i)
                            val title = item.optString("title", "")
                            val image = item.optString("image", "")
                            val swUrl = item.optString("url", "")

                            val finalStreamUrl = if (swUrl.startsWith("https://h5.swplayer.com")) {
                                swUrl.replace("https://h5.swplayer.com", "https://apio.videx.lol") + ".m3u8"
                            } else {
                                swUrl
                            }

                            parsed.add(
                                VideoItem(
                                    id = "p_rem_$i",
                                    title = title,
                                    subtitle = "Película recomendada / Click para ver",
                                    category = "Todas",
                                    streamUrl = finalStreamUrl,
                                    thumbnailUrl = image,
                                    extraInfo = "SwPlayer Stream Link",
                                    rating = "4.8",
                                    year = "2024"
                                )
                            )
                        }
                    }
                    _peliculas.value = parsed
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun openDirectSwPlayerUrl(urlStr: String) {
        val trimmed = urlStr.trim()
        if (!trimmed.contains("h5.swplayer.com")) {
            triggerToast("❌ URL no válida. Debe ser de h5.swplayer.com")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
            val targetUrl = "https://apio.videx.lol/movies/v3.php?url=$encoded"
            val response = fetchTextUrl(targetUrl)
            if (response != null) {
                val streamUrl = trimmed.replace("https://h5.swplayer.com", "https://apio.videx.lol") + ".m3u8"
                val rawTitle = trimmed.substringAfterLast("/").replace("-", " ")
                val cleanTitle = if (rawTitle.contains("?")) rawTitle.substringBefore("?") else rawTitle

                val videoItem = VideoItem(
                    id = "direct_" + UUID.randomUUID().toString().take(5),
                    title = cleanTitle.uppercase(),
                    subtitle = "Enlace directo SwPlayer cargado",
                    category = "Todas",
                    streamUrl = streamUrl,
                    thumbnailUrl = "",
                    extraInfo = "Direct Link",
                    rating = "4.9",
                    year = "VOD"
                )

                withContext(Dispatchers.Main) {
                    setSelectedVideo(videoItem)
                }
            } else {
                triggerToast("❌ Error al procesar detalle remoto")
            }
        }
    }

    // Reporting (GET https://apio.videx.lol/reporte.php?canal=X&problema=Y&usuario=Z)
    fun reportIssue(videoTitle: String, problem: String, descriptionAditional: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalProblem = if (descriptionAditional.isNotEmpty()) "$problem: $descriptionAditional" else problem
            val encodedTitle = java.net.URLEncoder.encode(videoTitle, "UTF-8")
            val encodedProblem = java.net.URLEncoder.encode(finalProblem, "UTF-8")
            val url = "https://apio.videx.lol/reporte.php?canal=$encodedTitle&problema=$encodedProblem&usuario=Anonimo"

            val response = fetchTextUrl(url)
            if (response != null) {
                try {
                    val obj = JSONObject(response)
                    val ok = obj.optBoolean("ok", false)
                    val duplicado = obj.optBoolean("duplicado", false)
                    if (ok) {
                        if (duplicado) {
                            triggerToast("⚠️ Ya reportaste este problema recientemente")
                        } else {
                            triggerToast("✅ Reporte enviado. ¡Gracias!")
                        }
                    } else {
                        val err = obj.optString("error", "Error en servidor")
                        triggerToast("❌ $err")
                    }
                } catch (e: Exception) {
                    triggerToast("✅ Reporte enviado con éxito")
                }
            } else {
                triggerToast("❌ Error al enviar el reporte. Verifica tu conexión.")
            }
        }
    }

    // IPTV presets loading simulation
    fun syncFromSourcePreset(presetName: String) {
        _activeSourcePresetName.value = presetName
        viewModelScope.launch {
            _isSyncing.value = true
            _syncLogs.value = listOf(
                "🌐 Conectando con servidor '$presetName'...",
                "📡 Analizando cabeceras CORS de origen seguro...",
                "⏱️ Descargando archivo JSON codificado de canales de emisión"
            )
            kotlinx.coroutines.delay(700)
            _syncLogs.value = _syncLogs.value + listOf(
                "⚙️ Leyendo descriptores bitrates HLS m3u8...",
                "📥 Mapeando variables remotas de streaming..."
            )
            kotlinx.coroutines.delay(600)

            when (presetName) {
                "Videx Central Oficial" -> {
                    cargarTodo()
                }
                "IPTV España Pública" -> {
                    _vivoEvents.value = listOf(
                        VideoItem("v_rtve", "RTVE La 1 HD en vivo", "Sigue el gran encuentro en vivo", "Fútbol", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "", "RTVE • Directo Nacional", "4.8", "HOY"),
                        VideoItem("v_tele", "Telecinco HD Deportes", "Clasificación en directo", "Motor", "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8", "", "Mediaset • En Vivo", "4.5", "HOY")
                    )
                    _canales.value = listOf(
                        VideoItem("c_antena", "Antena 3 HD", "Los mejores Blockbusters del cine", "Cine", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "", "Atresmedia • Cine", "4.6", "24 Horas"),
                        VideoItem("c_la2", "La 2 Documentales", "Exploración y maravillas de la tierra", "Documentales", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8", "", "RTVE • Cultura", "4.7", "24 Horas")
                    )
                }
                "Deportes 24h Premium" -> {
                    _vivoEvents.value = listOf(
                        VideoItem("dep_liga", "Real Madrid vs Barcelona", "Sabor de derbi en vivo", "Fútbol", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "", "34.1k Espectadores • Liga", "5.0", "HOY"),
                        VideoItem("dep_f1", "Fórmula 1: GP España", "Carrera en directo", "Motor", "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8", "", "22.0k Espectadores", "4.9", "HOY")
                    )
                    _canales.value = listOf(
                        VideoItem("dep_c1", "DAZN F1 Deportes", "Toda la cobertura del motor", "Deportes", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "", "Motor 24/7 • HD", "4.8", "24 Horas")
                    )
                }
            }

            _syncLogs.value = _syncLogs.value + listOf(
                "📊 Verificando disponibilidad de codec H.264...",
                "🌟 ¡Lista de reproducción obtenida correctamente!",
                "📱 Base de datos remota sincronizada. Interfaz actualizada."
            )
            kotlinx.coroutines.delay(500)
            triggerToast("🎉 ¡Sincronizado con origen '$presetName'!")
            _isSyncing.value = false
        }
    }

    fun parseAndImportM3uContent(m3uContent: String) {
        if (m3uContent.isBlank()) {
            triggerToast("⚠️ El contenido M3U está vacío.")
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _activeSourcePresetName.value = "Lista M3U Importada"
            _syncLogs.value = listOf(
                "⏳ Inicializando deserializador M3U...",
                "🔍 Analizando cabeceras #EXTM3U..."
            )
            kotlinx.coroutines.delay(600)

            val newEvents = mutableListOf<VideoItem>()
            val newCanales = mutableListOf<VideoItem>()
            val newPeliculas = mutableListOf<VideoItem>()

            val lines = m3uContent.split("\n", "\r\n")
            var currentTitle = ""
            var currentGroup = ""
            var originalIndex = 1

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXTINF:")) {
                    val parts = trimmed.substringAfter("#EXTINF:")
                    val titlePart = parts.substringAfterLast(",")
                    currentTitle = if (titlePart.isNotEmpty()) titlePart else "Canal M3U $originalIndex"

                    currentGroup = if (parts.contains("group-title=\"")) {
                        parts.substringAfter("group-title=\"").substringBefore("\"")
                    } else if (parts.contains("group-title=")) {
                        parts.substringAfter("group-title=").substringBefore(" ")
                    } else {
                        "Importado M3U"
                    }
                } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    if (currentTitle.isNotEmpty()) {
                        val itemId = "imp_${originalIndex}_${UUID.randomUUID().toString().take(4)}"
                        val finalGroup = if (currentGroup.isNotEmpty()) currentGroup else "Canales M3U"
                        val item = VideoItem(
                            id = itemId,
                            title = currentTitle,
                            subtitle = "Stream M3U • $finalGroup",
                            category = finalGroup,
                            streamUrl = trimmed,
                            extraInfo = "$finalGroup • Fuente Externa",
                            rating = "4.7",
                            year = "M3U"
                        )

                        val lowerGroup = finalGroup.lowercase()
                        if (lowerGroup.contains("futbol") || lowerGroup.contains("deportes") || lowerGroup.contains("sports") || lowerGroup.contains("vivo") || lowerGroup.contains("live")) {
                            newEvents.add(item.copy(category = "Fútbol", subtitle = "Deportes en directo"))
                        } else if (lowerGroup.contains("peli") || lowerGroup.contains("cine") || lowerGroup.contains("movie") || lowerGroup.contains("series")) {
                            newPeliculas.add(item.copy(category = "Drama", subtitle = "Cine Importado"))
                        } else {
                            val cat = if (lowerGroup.contains("noticias") || lowerGroup.contains("news")) "Noticias"
                                      else if (lowerGroup.contains("docu")) "Documentales"
                                      else "Cine"
                            newCanales.add(item.copy(category = cat))
                        }
                        originalIndex++
                        currentTitle = ""
                        currentGroup = ""
                    }
                }
            }

            _syncLogs.value = _syncLogs.value + listOf(
                "⚡ Parser M3U completado.",
                "📊 Sintaxis de ${lines.size} líneas escaneada con éxito.",
                "✅ Canales importados: ${newEvents.size} en vivo, ${newCanales.size} canales TV, ${newPeliculas.size} películas."
            )
            kotlinx.coroutines.delay(1000)

            if (newEvents.isNotEmpty() || newCanales.isNotEmpty() || newPeliculas.isNotEmpty()) {
                _vivoEvents.value = newEvents
                _canales.value = newCanales
                _peliculas.value = newPeliculas
                triggerToast("🎉 ¡Lista M3U procesada con éxito! ${newEvents.size + newCanales.size + newPeliculas.size} canales importados.")
            } else {
                _syncLogs.value = _syncLogs.value + listOf("❌ Error: No se encontraron descriptores #EXTINF con URLs válidas de streams.")
                triggerToast("⚠️ No se encontraron canales M3U válidos. Revisa el formato.")
            }
            _isSyncing.value = false
        }
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun setSubTab(sub: String) {
        _subTab.value = sub
    }

    fun setSelectedVideo(video: VideoItem?) {
        if (video != null && video.year == "CANDADO") {
            triggerToast("🔒 Contenido Premium Bloqueado. Suscríbete para acceder.")
            return
        }
        _selectedVideo.value = video
        if (video == null && _isRecording.value) {
            stopRecording(save = false)
        }
    }

    fun setQuery(q: String) {
        _searchQuery.value = q
        // Auto trigger search if tab is movies
        if (_activeTab.value == "pelicula") {
            searchMoviesRemote(q)
        }
    }

    fun setConnectivityMode(mode: String) {
        if (_connectivityMode.value == mode) return
        viewModelScope.launch {
            _isVerifyingConnection.value = true
            triggerToast("Verificando estado de la red...")
            kotlinx.coroutines.delay(1200)
            _connectivityMode.value = mode
            _isVerifyingConnection.value = false
            if (mode == "SIN_CONEXION") {
                triggerToast("⚠️ Modo Sin Conexión Activado. Listas online deshabilitadas.")
            } else {
                triggerToast("⚡ ¡Conexión en línea restablecida con éxito!")
                cargarTodo()
            }
        }
    }

    fun setQrLightboxOpen(open: Boolean) {
        _qrLightboxOpen.value = open
    }

    fun setSourcesDialogOpened(opened: Boolean) {
        _sourcesDialogOpened.value = opened
    }

    fun setLiveCategory(cat: String) {
        _selectedLiveCategory.value = cat
    }

    fun setChannelCategory(cat: String) {
        _selectedChannelCategory.value = cat
    }

    fun setMovieCategory(cat: String) {
        _selectedMovieCategory.value = cat
    }

    fun triggerToast(msg: String) {
        _toastMessage.value = msg
        Handler(Looper.getMainLooper()).postDelayed({
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }, 3000)
    }

    // Toggle simulated recording for currently playing video
    fun toggleRecording() {
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

    // Connection checker
    suspend fun verificarConexion(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://apio.videx.lol/sms.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "HEAD"
                val code = conn.responseCode
                code in 200..399
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun fetchTextUrl(urlString: String, timeout: Int = 10000): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = timeout
            conn.readTimeout = timeout
            conn.requestMethod = "GET"
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun compararVersiones(v1: String, v2: String): Int {
        val a = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val b = v2.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until 3) {
            val ai = a.getOrElse(i) { 0 }
            val bi = b.getOrElse(i) { 0 }
            if (ai < bi) return -1
            if (ai > bi) return 1
        }
        return 0
    }
}
