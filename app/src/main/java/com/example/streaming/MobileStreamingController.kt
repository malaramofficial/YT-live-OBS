package com.example.streaming

import android.content.Context
import android.util.Log
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpStream
import com.pedro.library.view.OpenGlView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MobileStreamState { IDLE, PREPARING, PREVIEWING, CONNECTING, LIVE, FAILED }

class MobileStreamingController(context: Context) : ConnectChecker {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(MobileStreamState.IDLE)
    val state: StateFlow<MobileStreamState> = _state.asStateFlow()
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()
    private val rtmpStream = RtmpStream(appContext, this).apply { getStreamClient().setReTries(10) }

    fun prepare(width: Int = 1280, height: Int = 720, fps: Int = 30, videoBitrate: Int = 4_000_000): Boolean {
        _state.value = MobileStreamState.PREPARING
        return try {
            val videoOk = rtmpStream.prepareVideo(width, height, fps, videoBitrate)
            val audioOk = rtmpStream.prepareAudio(128_000, 44_100, false)
            val ok = videoOk && audioOk
            if (!ok) { _state.value = MobileStreamState.FAILED; _message.value = "Device encoder preparation failed" }
            ok
        } catch (e: Exception) {
            Log.e("MobileStreaming", "prepare failed", e)
            _state.value = MobileStreamState.FAILED; _message.value = e.message ?: "Encoder preparation failed"; false
        }
    }

    fun attachPreview(view: OpenGlView) {
        try { if (!rtmpStream.isOnPreview) rtmpStream.startPreview(view); _state.value = MobileStreamState.PREVIEWING }
        catch (e: Exception) { Log.e("MobileStreaming", "preview failed", e); _state.value = MobileStreamState.FAILED; _message.value = e.message ?: "Camera preview failed" }
    }

    fun start(ingestUrl: String): Boolean {
        if (ingestUrl.isBlank()) return false
        return try { _state.value = MobileStreamState.CONNECTING; rtmpStream.startStream(ingestUrl); true }
        catch (e: Exception) { Log.e("MobileStreaming", "start failed", e); _state.value = MobileStreamState.FAILED; _message.value = e.message ?: "Unable to start stream"; false }
    }

    fun stop() {
        try { if (rtmpStream.isStreaming) rtmpStream.stopStream(); if (rtmpStream.isOnPreview) rtmpStream.stopPreview() } catch (e: Exception) { Log.e("MobileStreaming", "stop failed", e) }
        _state.value = MobileStreamState.IDLE; _message.value = ""
    }

    fun release() {
        try { if (rtmpStream.isStreaming) rtmpStream.stopStream(); if (rtmpStream.isOnPreview) rtmpStream.stopCamera(); rtmpStream.release() } catch (e: Exception) { Log.e("MobileStreaming", "release failed", e) }
        _state.value = MobileStreamState.IDLE
    }

    override fun onConnectionStarted(url: String) { _state.value = MobileStreamState.CONNECTING; _message.value = "Connecting to YouTube..." }
    override fun onConnectionSuccess() { _state.value = MobileStreamState.LIVE; _message.value = "YouTube ingest connected" }
    override fun onConnectionFailed(reason: String) { _state.value = MobileStreamState.FAILED; _message.value = "Stream connection failed: " + reason }
    override fun onNewBitrate(bitrate: Long) { _message.value = "Upload: " + (bitrate / 1000) + " kbps" }
    override fun onDisconnect() { if (_state.value == MobileStreamState.LIVE || _state.value == MobileStreamState.CONNECTING) _state.value = MobileStreamState.IDLE }
    override fun onAuthError() { _state.value = MobileStreamState.FAILED; _message.value = "YouTube ingest authentication failed" }
    override fun onAuthSuccess() { _message.value = "YouTube ingest authenticated" }
}