package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.QnaEntity
import com.example.data.model.Announcement
import com.example.data.model.LivePoll
import com.example.data.model.OverlayConfig
import com.example.data.model.OverlayTheme
import com.example.data.repository.LiveConnectionState
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LiveRed
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBgElevated
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.SuperChatGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VerifiedBadgeColor
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LiveEngagementViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OverlayPreviewScreen(
    viewModel: LiveEngagementViewModel,
    modifier: Modifier = Modifier
) {
    val overlayConfig by viewModel.overlayConfig.collectAsState()
    val spotlightQuestion by viewModel.spotlightQuestion.collectAsState()
    val currentPoll by viewModel.currentPoll.collectAsState()
    val currentAnnouncement by viewModel.currentAnnouncement.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val channelTitle by viewModel.channelTitle.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var customAnnouncementText by remember { mutableStateOf("") }

    val quickAnnouncements = listOf(
        Pair("❤️", "Welcome to Live"),
        Pair("👍", "Like कर दो"),
        Pair("🔔", "Subscribe कर लो"),
        Pair("💬", "अपना सवाल भेजो"),
        Pair("📢", "अगला Poll शुरू होने वाला है")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("overlay_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LIVE OVERLAY PREVIEW",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "OBS Studio / StreamLabs स्क्रीन ओवरले",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(StudioCardBgElevated, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "16:9 PREVIEW",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // High Fidelity Overlay Canvas (Screen Preview)
            item {
                LiveOverlayCanvas(
                    config = overlayConfig,
                    channelName = channelTitle,
                    connectionState = connectionState,
                    question = spotlightQuestion,
                    poll = currentPoll,
                    announcement = currentAnnouncement
                )
            }

            // Theme selector chips
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Overlay Theme",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(OverlayTheme.values()) { theme ->
                                FilterChip(
                                    selected = overlayConfig.theme == theme,
                                    onClick = { viewModel.updateOverlayTheme(theme) },
                                    label = { Text(theme.displayName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = YouTubeRed,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Quick Announcement Buttons (Specified in Prompt)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(YouTubeRed.copy(alpha = 0.4f), StudioCardBorder))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_announcements_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = YouTubeRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Quick Announcements",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (currentAnnouncement != null) {
                                TextButtonSmall(
                                    label = "Clear Active",
                                    onClick = { viewModel.clearAnnouncement() }
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "एक टैप में लाइव स्क्रीन टिकर और चैट में संदेश प्रसारित करें:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        // Prompt-mandated buttons:
                        // ❤️ Welcome to Live
                        // 👍 Like कर दो
                        // 🔔 Subscribe कर लो
                        // 💬 अपना सवाल भेजो
                        // 📢 अगला Poll शुरू होने वाला है
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            quickAnnouncements.forEach { (emoji, text) ->
                                val isCurrent = currentAnnouncement?.text == text
                                Button(
                                    onClick = {
                                        viewModel.broadcastAnnouncement(emoji, text)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("प्रसारित: $text")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrent) YouTubeRed else StudioCardBgElevated
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_announcement_${emoji}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = emoji, fontSize = 16.sp)
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = text,
                                            color = if (isCurrent) Color.White else TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isCurrent) {
                                            Text(
                                                text = "ON AIR",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Custom announcement input
                        Text(
                            text = "कस्टम संदेश लिखें (Custom Announcement):",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customAnnouncementText,
                                onValueChange = { customAnnouncementText = it },
                                placeholder = { Text("उदा. 5 मिनट में परिणाम घोषित करेंगे...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_custom_announcement"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = YouTubeRed,
                                    unfocusedBorderColor = StudioCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (customAnnouncementText.isNotBlank()) {
                                        viewModel.broadcastAnnouncement("📢", customAnnouncementText.trim())
                                        customAnnouncementText = ""
                                        scope.launch {
                                            snackbarHostState.showSnackbar("कस्टम संदेश प्रसारित किया गया")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("btn_broadcast_custom")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Broadcast")
                            }
                        }
                    }
                }
            }

            // Overlay Elements Toggles
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = AccentBlue)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Overlay Elements Visibility",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        OverlayToggleRow(
                            label = "🔴 LIVE Badge",
                            checked = overlayConfig.showLiveBadge,
                            onCheckedChange = { viewModel.toggleOverlayElement("liveBadge", it) }
                        )
                        OverlayToggleRow(
                            label = "Channel Title (Malaram Official)",
                            checked = overlayConfig.showChannelTitle,
                            onCheckedChange = { viewModel.toggleOverlayElement("channelTitle", it) }
                        )
                        OverlayToggleRow(
                            label = "Viewer Count Counter",
                            checked = overlayConfig.showViewerCount,
                            onCheckedChange = { viewModel.toggleOverlayElement("viewerCount", it) }
                        )
                        OverlayToggleRow(
                            label = "Current Q&A Question",
                            checked = overlayConfig.showCurrentQuestion,
                            onCheckedChange = { viewModel.toggleOverlayElement("question", it) }
                        )
                        OverlayToggleRow(
                            label = "Live Poll Results Widget",
                            checked = overlayConfig.showPollResults,
                            onCheckedChange = { viewModel.toggleOverlayElement("poll", it) }
                        )
                        OverlayToggleRow(
                            label = "Running Announcement Ticker",
                            checked = overlayConfig.showAnnouncementTicker,
                            onCheckedChange = { viewModel.toggleOverlayElement("announcement", it) }
                        )
                    }
                }
            }

            // OBS Browser Source URL Guide
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBgElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "OBS Browser Source Setup",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "OBS में 'Browser Source' जोड़ें और 1920x1080 रेजोल्यूशन सेट करें:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioBlack, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "http://localhost:8080/overlay?theme=${overlayConfig.theme.name.lowercase()}",
                                color = AccentBlue,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(
                                        AnnotatedString("http://localhost:8080/overlay?theme=${overlayConfig.theme.name.lowercase()}")
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar("OBS Link Copied!")
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun LiveOverlayCanvas(
    config: OverlayConfig,
    channelName: String,
    connectionState: LiveConnectionState,
    question: QnaEntity?,
    poll: LivePoll,
    announcement: Announcement?
) {
    val bgColor = when (config.theme) {
        OverlayTheme.STUDIO_RED -> Color(0xFF0F1015)
        OverlayTheme.NEON_CYBER -> Color(0xFF0A0D18)
        OverlayTheme.MINIMAL_DARK -> Color(0xDD12131A)
        OverlayTheme.GREEN_SCREEN -> Color(0xFF00FF00) // Chroma key green!
    }

    val textColor = if (config.theme == OverlayTheme.GREEN_SCREEN) Color.Black else TextPrimary

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(YouTubeRed, AccentBlue))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .testTag("overlay_canvas")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP BAR: 🔴 LIVE | Malaram Official | Viewer Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (config.showLiveBadge) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(LiveRed, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                PulsingLiveDot()
                                Spacer(Modifier.width(4.dp))
                                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        if (config.showChannelTitle) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = channelName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VerifiedBadgeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    if (config.showViewerCount) {
                        val viewers = (connectionState as? LiveConnectionState.Active)?.streamInfo?.viewerCount ?: 1420
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = LiveRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$viewers",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // MIDDLE SECTION: Current Question or Poll (Overlay Graphics)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Question Spotlight Banner
                    if (config.showCurrentQuestion && question != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xE61F232E), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF9800), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Q&A",
                                    color = StudioBlack,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = question.questionText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Asked by ${question.authorName}",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    // Poll Results Mini Widget
                    if (config.showPollResults && poll.showOnOverlay && poll.options.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xE6161820), RoundedCornerShape(8.dp))
                                .border(1.dp, YouTubeRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "POLL: ${poll.question.take(24)}...",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            poll.options.take(3).forEach { opt ->
                                val pct = poll.percentageFor(opt.id)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${opt.text.take(8)}: $pct%",
                                    color = AccentBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // BOTTOM: Running Announcement Ticker
                if (config.showAnnouncementTicker && announcement != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(YouTubeRed, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = announcement.emoji, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = announcement.text,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = YouTubeRed,
                checkedTrackColor = YouTubeRed.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun TextButtonSmall(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = AccentBlue,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    )
}
