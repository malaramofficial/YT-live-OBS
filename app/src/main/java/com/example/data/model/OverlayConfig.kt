package com.example.data.model

enum class OverlayTheme(val displayName: String) {
    STUDIO_RED("YouTube Studio (Dark Red)"),
    NEON_CYBER("Cyber Neon (OBS Style)"),
    MINIMAL_DARK("Minimal Clean Glass"),
    GREEN_SCREEN("Chroma Green (Transparent)")
}

data class OverlayConfig(
    val showLiveBadge: Boolean = true,
    val showChannelTitle: Boolean = true,
    val showViewerCount: Boolean = true,
    val showCurrentQuestion: Boolean = true,
    val showPollResults: Boolean = true,
    val showAnnouncementTicker: Boolean = true,
    val theme: OverlayTheme = OverlayTheme.STUDIO_RED,
    val channelName: String = "Malaram Official"
)
