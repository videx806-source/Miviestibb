package com.example.ui

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoPlayerView(
    streamUrl: String,
    title: String,
    isRecording: Boolean,
    recordingTime: String,
    onToggleRecording: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // Overlay controls state
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    
    // Gestures
    var volumeLevel by remember { mutableStateOf(0.7f) }
    var brightnessLevel by remember { mutableStateOf(0.8f) }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }

    // Auto-disable controller overlay
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
    }

    // HTML / HLS Player configuration loaded inside WebView
    val playerHtml = remember(streamUrl) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <script src="https://cdn.jsdelivr.net/npm/hls.js@1.4.12/dist/hls.min.js"></script>
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #000000;
                    overflow: hidden;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }
                video {
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                    outline: none;
                }
            </style>
        </head>
        <body>
            <video id="vid" autoplay playsinline style="background: black;"></video>
            <script>
                var video = document.getElementById('vid');
                var streamUrl = "$streamUrl";
                
                function playVideo() {
                    video.play();
                }
                
                function pauseVideo() {
                    video.pause();
                }
                
                function setVolumeValue(val) {
                    video.volume = val;
                }
                
                if (Hls.isSupported()) {
                    var hls = new Hls({
                        enableWorker: true,
                        lowLatencyMode: true
                    });
                    hls.loadSource(streamUrl);
                    hls.attachMedia(video);
                    hls.on(Hls.Events.MANIFEST_PARSED, function() {
                        video.play().catch(function(e) {
                            console.log("Autoplay blocked: " + e);
                        });
                    });
                    hls.on(Hls.Events.ERROR, function (event, data) {
                        if (data.fatal) {
                            switch (data.type) {
                                case Hls.ErrorTypes.NETWORK_ERROR:
                                    hls.startLoad();
                                    break;
                                case Hls.ErrorTypes.MEDIA_ERROR:
                                    hls.recoverMediaError();
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                } else if (video.canPlayType('application/vnd.apple.mpegurl') || streamUrl.endsWith('.mp4')) {
                    video.src = streamUrl;
                    video.play().catch(function(e) {
                        console.log("Autoplay blocked: " + e);
                    });
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    // Update WebView when dynamic streamUrl changes
    LaunchedEffect(playerHtml, webViewRef) {
        webViewRef?.let { webView ->
            webView.loadDataWithBaseURL("https://localhost", playerHtml, "text/html", "UTF-8", null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // Platform WebView Surface View
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    webViewRef = this
                }
            },
            update = { webView ->
                webViewRef = webView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom gestures modifier layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            showVolumeOverlay = false
                            showBrightnessOverlay = false
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val halfWidth = size.width / 2f
                        if (change.position.x < halfWidth) {
                            // Left side = Brightness
                            brightnessLevel = (brightnessLevel - dragAmount.y / 800f).coerceIn(0f, 1f)
                            showBrightnessOverlay = true
                        } else {
                            // Right side = Volume
                            volumeLevel = (volumeLevel - dragAmount.y / 800f).coerceIn(0f, 1f)
                            webViewRef?.evaluateJavascript("setVolumeValue($volumeLevel);", null)
                            showVolumeOverlay = true
                        }
                    }
                }
        )

        // Volume parameters indicator
        if (showVolumeOverlay) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (volumeLevel > 0.5f) "🔊" else "🔉",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(volumeLevel * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Brightness parameter indicator
        if (showBrightnessOverlay) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "☀️",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(brightnessLevel * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Overlay Interactive Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Cerrar reproductor",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isRecording) Color.Red else Color.Green, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecording) "GRABANDO • $recordingTime" else "EN VIVO",
                                color = if (isRecording) Color.Red else Color.Green,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Recording button action
                    IconButton(
                        onClick = onToggleRecording,
                        modifier = Modifier
                            .background(
                                if (isRecording) Color.Red else Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            )
                    ) {
                        Text(
                            text = if (isRecording) "■" else "●",
                            color = if (isRecording) Color.White else Color.Red,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Play / Pause central button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            webViewRef?.evaluateJavascript("pauseVideo();", null)
                            isPlaying = false
                        } else {
                            webViewRef?.evaluateJavascript("playVideo();", null)
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    if (isPlaying) {
                        Text(
                            text = "❚❚",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play o Pausa",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Subtitle / Tips guide overlay at bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "💡 Desliza verticalmente: Lado izquierdo para Brillo | Lado derecho para Volumen",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
