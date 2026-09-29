package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.malaramofficial.ytliveobs.R
import com.example.data.model.LiveStreamInfo
import com.example.data.repository.LiveConnectionState
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LiveRed
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBgElevated
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VerifiedBadgeColor
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LiveEngagementViewModel
import com.example.ui.viewmodel.ScreenTab
import kotlinx.coroutines.delay

@Composable
fun LiveDashboardScreen(
    viewModel: LiveEngagementViewModel,
    onNavigateTab: (ScreenTab) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnected by viewModel.isAccountConnected.collectAsState()
    val channelTitle by viewModel.channelTitle.collectAsState()
    val currentPoll by viewModel.currentPoll.collectAsState()
    val spotlightQuestion by viewModel.spotlightQuestion.collectAsState()
    val currentAnnouncement by viewModel.currentAnnouncement.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App / Channel Header Banner
        item {
            ChannelHeaderBanner(
                channelTitle = channelTitle,
                isConnected = isConnected,
                connectionState = connectionState,
                onOpenSettings = onOpenSettings
            )
        }

        // Branching: Disconnected vs Checking vs NoActiveLive vs Active
        when (val state = connectionState) {
            is LiveConnectionState.Disconnected -> {
                item {
                    ConnectYouTubeCard(
                        channelTitle = channelTitle,
                        onConnect = { chName ->
                            viewModel.connectYouTube(chName)
                        },
                    )
                }
            }
            is LiveConnectionState.Checking -> {
                item {
                    CheckingLiveCard()
                }
            }
            is LiveConnectionState.NoActiveLive -> {
                item {
                    NoActiveLiveCard(
                        message = state.message,
                        onRefresh = { viewModel.checkActiveLive() },
                        onCheckVideoId = { videoId -> viewModel.checkVideoById(videoId) }
                    )
                }
            }
            is LiveConnectionState.Active -> {
                item {
                    ActiveLiveStreamCard(
                        streamInfo = state.streamInfo,
                        onRefresh = { viewModel.checkActiveLive() }
                    )
                }

                // Quick Navigation Hub
                item {
                    Text(
                        text = "LIVE CONTROL CENTER",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                // Live Chat Quick Card
                item {
                    ControlCenterHubCard(
                        title = "Live Chat",
                        subtitle = "${chatMessages.size} संदेश प्राप्त | त्वरित उत्तर व हाइलाइट करें",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        accentColor = AccentBlue,
                        badgeText = "${chatMessages.count { it.isSuperChat }} Super Chat",
                        testTag = "hub_card_chat",
                        onClick = { onNavigateTab(ScreenTab.CHAT) }
                    )
                }

                // Q&A Quick Card
                item {
                    ControlCenterHubCard(
                        title = "Q&A System",
                        subtitle = if (spotlightQuestion != null) {
                            "चालू सवाल: \"${spotlightQuestion?.questionText?.take(35)}...\""
                        } else {
                            "दर्शकों के सवालों को पंक्ति में व्यवस्थित करें"
                        },
                        icon = Icons.Default.QuestionAnswer,
                        accentColor = Color(0xFFFF9800),
                        badgeText = if (spotlightQuestion != null) "सक्रिय" else "तैयार",
                        testTag = "hub_card_qna",
                        onClick = { onNavigateTab(ScreenTab.QNA) }
                    )
                }

                // Poll Quick Card
                item {
                    ControlCenterHubCard(
                        title = "Live Poll",
                        subtitle = "सवाल: ${currentPoll.question} (${currentPoll.totalVotes} वोट्स)",
                        icon = Icons.Default.Poll,
                        accentColor = Color(0xFF00E676),
                        badgeText = if (currentPoll.isActive) "VOTING LIVE" else "ENDED",
                        testTag = "hub_card_poll",
                        onClick = { onNavigateTab(ScreenTab.POLL) }
                    )
                }

                // Announcement Quick Card
                item {
                    ControlCenterHubCard(
                        title = "Quick Announcements",
                        subtitle = currentAnnouncement?.text ?: "लाइव के दौरान त्वरित संदेश प्रसारित करें",
                        icon = Icons.Default.Campaign,
                        accentColor = YouTubeRed,
                        badgeText = if (currentAnnouncement != null) "OVERLAY ACTIVE" else null,
                        testTag = "hub_card_announcement",
                        onClick = { onNavigateTab(ScreenTab.OVERLAY) }
                    )
                }

                // Overlay Preview Card
                item {
                    ControlCenterHubCard(
                        title = "Live Overlay Preview",
                        subtitle = "OBS / StreamLabs स्क्रीन ओवरले ग्राफ़िक्स देखें व कस्टमाइज़ करें",
                        icon = Icons.Default.LiveTv,
                        accentColor = Color(0xFF7C4DFF),
                        badgeText = "OBS HD",
                        testTag = "hub_card_overlay",
                        onClick = { onNavigateTab(ScreenTab.OVERLAY) }
                    )
                }
            }
            is LiveConnectionState.Error -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("त्रुटि: ${state.errorMsg}", color = LiveRed, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.checkActiveLive() },
                                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                            ) {
                                Text("पुनः प्रयास करें")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelHeaderBanner(
    channelTitle: String,
    isConnected: Boolean,
    connectionState: LiveConnectionState,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(StudioCardBorder, LiveRed.copy(alpha = 0.3f)))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                // Banner illustration
                Image(
                    painter = painterResource(id = R.drawable.img_live_studio_banner),
                    contentDescription = "Live Studio Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, StudioCardBg.copy(alpha = 0.95f))
                            )
                        )
                )

                // Live status pill at top right
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(
                            if (connectionState is LiveConnectionState.Active) LiveRed else Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (connectionState is LiveConnectionState.Active) {
                        PulsingLiveDot()
                        Spacer(Modifier.width(6.dp))
                        Text("LIVE NOW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.SensorsOff,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("OFFLINE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel Avatar with Glow
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isConnected) VerifiedBadgeColor else TextTertiary, CircleShape)
                        .background(StudioCardBgElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channelTitle.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = channelTitle,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Channel",
                            tint = VerifiedBadgeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "YouTube Live Engagement Center",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.testTag("channel_settings_btn")
                ) {
                    Text("Settings", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PulsingLiveDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(8.dp)
            .alpha(alpha)
            .background(Color.White, CircleShape)
    )
}

@Composable
private fun ConnectYouTubeCard(
    channelTitle: String,
    onConnect: (String) -> Unit
) {
    var inputChannel by remember { mutableStateOf(channelTitle) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(YouTubeRed.copy(alpha = 0.4f), StudioCardBorder))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(LiveRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = YouTubeRed,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Connect YouTube",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "अपने YouTube चैनल को कनेक्ट करें और लाइव चैट, Q&A, लाइव पोल और OBS ओवरले को एक ही जगह से नियंत्रित करें।",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            OutlinedTextField(
                value = inputChannel,
                onValueChange = { inputChannel = it },
                label = { Text("Channel Name / Handle") },
                placeholder = { Text("Malaram Official") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_channel_name"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YouTubeRed,
                    unfocusedBorderColor = StudioCardBorder,
                    focusedLabelColor = YouTubeRed,
                    cursorColor = YouTubeRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onConnect(inputChannel) },
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_connect_youtube")
            ) {
                Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connect YouTube", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

        }
    }
}

@Composable
private fun CheckingLiveCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = YouTubeRed,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Checking Active Live Stream...",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "चैनल पर सक्रिय लाइव प्रसारण की जाँच की जा रही है",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun NoActiveLiveCard(
    message: String,
    onRefresh: () -> Unit,
    onStartTestMode: () -> Unit,
    onCheckVideoId: (String) -> Unit
) {
    var videoIdInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(StudioCardBorder, StudioCardBorder))),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("no_active_live_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(StudioCardBgElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SensorsOff,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "No Active Live",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = message,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardBgElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_refresh_live")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Check Again", color = TextPrimary)
                }

                Button(
                    onClick = onStartTestMode,
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_start_demo_live")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Test Live Run", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (showManualInput) "छुपाएं" else "+ किसी भी YouTube Live Video ID से कनेक्ट करें",
                color = AccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { showManualInput = !showManualInput }
                    .padding(4.dp)
            )

            AnimatedVisibility(visible = showManualInput) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = videoIdInput,
                            onValueChange = { videoIdInput = it },
                            placeholder = { Text("उदा. dQw4w9WgXcQ या Live ID") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_video_id"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = StudioCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (videoIdInput.isNotBlank()) onCheckVideoId(videoIdInput.trim())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveLiveStreamCard(
    streamInfo: LiveStreamInfo,
    onRefresh: () -> Unit
) {
    var elapsedSeconds by remember { mutableStateOf(1320L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timerString = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(LiveRed, YouTubeRed))
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_live_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Live status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(LiveRed, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    PulsingLiveDot()
                    Spacer(Modifier.width(6.dp))
                    Text("🔴 LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(StudioCardBgElevated, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⏱️ $timerString",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Thumbnail and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 66.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioCardBgElevated)
                ) {
                    if (streamInfo.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = streamInfo.thumbnailUrl,
                            contentDescription = "Stream Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_live_studio_banner),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = streamInfo.title.ifBlank { "Malaram Official Live Stream" },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = streamInfo.channelTitle,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Metrics row: Viewers, Likes, Quality
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioCardBgElevated, RoundedCornerShape(10.dp))
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(
                    icon = Icons.Default.Visibility,
                    value = "${streamInfo.viewerCount}",
                    label = "Viewers",
                    color = LiveRed
                )
                MetricItem(
                    icon = Icons.Default.ThumbUp,
                    value = "${streamInfo.likeCount}",
                    label = "Likes",
                    color = AccentBlue
                )
                MetricItem(
                    icon = Icons.Default.Sensors,
                    value = streamInfo.streamQuality,
                    label = "Quality",
                    color = Color(0xFF00E676)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ControlCenterHubCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    badgeText: String?,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (badgeText != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = badgeText,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
