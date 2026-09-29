package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.streaming.MobileStreamState
import com.example.streaming.MobileStreamingController
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed
import com.pedro.library.view.OpenGlView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.LiveEngagementViewModel

@Composable
fun MobileStreamingScreen(viewModel: LiveEngagementViewModel) {
    val context = LocalContext.current
    val controller = remember { MobileStreamingController(context) }
    val streamState by controller.state.collectAsState()
    val statusMessage by controller.message.collectAsState()
    val connected by viewModel.isAccountConnected.collectAsState()
    var title by remember { mutableStateOf("Malaram Official Live") }
    var previewView by remember { mutableStateOf<OpenGlView?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionDenied = result.values.any { !it }
    }
    val hasPermissions = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasPermissions, previewView) {
        if (hasPermissions && previewView != null) {
            if (controller.prepare()) controller.attachPreview(previewView!!)
        }
    }

    DisposableEffect(Unit) { onDispose { controller.release() } }

    fun begin() {
        if (!hasPermissions) { permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)); return }
        if (!connected) return
        if (streamState == MobileStreamState.LIVE || streamState == MobileStreamState.CONNECTING || streamState == MobileStreamState.PREPARING) return
        androidx.compose.runtime.LaunchedEffect(Unit)
    }

    Box(Modifier.fillMaxSize().background(StudioBlack)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(300.dp).background(Color.Black), contentAlignment = Alignment.Center) {
                AndroidView(factory = { ctx -> OpenGlView(ctx).also { previewView = it } }, modifier = Modifier.fillMaxSize())
                if (!hasPermissions) Text("Camera + microphone permission required", color = TextSecondary)
                if (streamState == MobileStreamState.LIVE) Text("🔴 LIVE", color = Color.White, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopStart).padding(16.dp))
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mobile YouTube Studio", color = TextPrimary, fontSize = 22.sp)
                Text("Camera + microphone → hardware H.264/AAC → YouTube RTMPS", color = TextSecondary, fontSize = 13.sp)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Live title") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (statusMessage.isNotBlank()) Text(statusMessage, color = TextSecondary, fontSize = 12.sp)
                if (permissionDenied) Text("Camera/microphone permission denied. Allow both in Android settings.", color = Color(0xFFFF6B6B), fontSize = 12.sp)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (!hasPermissions) { permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)); return@Button }
                            if (!connected) return@Button
                            if (streamState == MobileStreamState.LIVE || streamState == MobileStreamState.CONNECTING) { controller.stop(); return@Button }
                            androidx.compose.runtime.LaunchedEffect(Unit)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (streamState == MobileStreamState.LIVE) Color(0xFF8B0000) else YouTubeRed),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (streamState == MobileStreamState.LIVE) "STOP LIVE" else "GO LIVE") }
                }
                if (!connected) Text("पहले Google/YouTube Connect करें।", color = YouTubeRed, fontSize = 13.sp)
                Text("No Video ID required. The app creates the YouTube broadcast, creates the ingest stream, binds them, and sends the camera feed directly.", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}