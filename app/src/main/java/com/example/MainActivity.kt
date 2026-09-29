package com.malaramofficial.ytliveobs

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LiveConnectionState
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.example.ui.screens.LiveChatScreen
import com.example.ui.screens.LiveDashboardScreen
import com.example.ui.screens.OverlayPreviewScreen
import com.example.ui.screens.PollScreen
import com.example.ui.screens.PulsingLiveDot
import com.example.ui.screens.QnaScreen
import com.example.ui.screens.SettingsAndAboutDialog
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LiveRed
import com.example.ui.theme.MalaramLiveTheme
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBgElevated
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.SuperChatGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LiveEngagementViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {

    companion object {
        private const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"
    }

    private val youtubeAuthorizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                runCatching {
                    Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data)
                }.onSuccess { authResult ->
                    val token = authResult.accessToken
                    if (!token.isNullOrBlank()) {
                        viewModel.setOAuthToken(token)
                        viewModel.connectYouTube()
                    } else {
                        viewModel.setConnectionError("Google authorization returned no YouTube access token.")
                    }
                }.onFailure {
                    viewModel.setConnectionError("Google/YouTube authorization failed. Please try Connect YouTube again.")
                }
            } else {
                viewModel.setConnectionError("Google/YouTube authorization was cancelled or denied.")
            }
        }
    private val viewModel: LiveEngagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MalaramLiveTheme {
                MainAppScreen(
                    viewModel = viewModel,
                    onConnectYouTube = ::authorizeYouTube
                )
            }
        }
    }

    private fun authorizeYouTube() {
        val request = AuthorizationRequest.Builder()
            .setRequestedScopes(listOf(Scope(YOUTUBE_SCOPE)))
            .build()

        Identity.getAuthorizationClient(this)
            .authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution() && result.pendingIntent != null) {
                    youtubeAuthorizationLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                    )
                } else {
                    result.accessToken?.takeIf { it.isNotBlank() }?.let { token ->
                        viewModel.setOAuthToken(token)
                        viewModel.connectYouTube()
                    } ?: viewModel.setConnectionError("Google authorization returned no YouTube access token.")
                }
            }
            .addOnFailureListener {
                viewModel.setConnectionError("Unable to start Google/YouTube authorization. Check the app OAuth configuration.")
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: LiveEngagementViewModel,
    onConnectYouTube: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val qnaList by viewModel.qnaList.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val currentPoll by viewModel.currentPoll.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val isLiveActive = connectionState is LiveConnectionState.Active
    val pendingQuestionsCount = qnaList.count { !it.isAnswered }
    val superChatsCount = chatMessages.count { it.isSuperChat }

    // Intercept back button to return to LIVE tab before exiting
    BackHandler(enabled = currentTab != ScreenTab.LIVE) {
        viewModel.setTab(ScreenTab.LIVE)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StudioBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartDisplay,
                            contentDescription = null,
                            tint = YouTubeRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Malaram Live",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLiveActive) {
                            Spacer(Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(LiveRed, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                PulsingLiveDot()
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("topbar_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & About",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = StudioBlack
                )
            )
        },
        bottomBar = {
            // Mandated navigation: LIVE | CHAT | Q&A | POLL | OVERLAY
            NavigationBar(
                containerColor = StudioCardBg,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // 1. LIVE Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.LIVE,
                    onClick = { viewModel.setTab(ScreenTab.LIVE) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "LIVE",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "LIVE",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == ScreenTab.LIVE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = YouTubeRed,
                        selectedTextColor = YouTubeRed,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = StudioCardBgElevated
                    ),
                    modifier = Modifier.testTag("nav_tab_live")
                )

                // 2. CHAT Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.CHAT,
                    onClick = { viewModel.setTab(ScreenTab.CHAT) },
                    icon = {
                        if (superChatsCount > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = SuperChatGold, contentColor = StudioBlack) {
                                    Text("$superChatsCount")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "CHAT",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "CHAT",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "CHAT",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == ScreenTab.CHAT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = YouTubeRed,
                        selectedTextColor = YouTubeRed,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = StudioCardBgElevated
                    ),
                    modifier = Modifier.testTag("nav_tab_chat")
                )

                // 3. Q&A Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.QNA,
                    onClick = { viewModel.setTab(ScreenTab.QNA) },
                    icon = {
                        if (pendingQuestionsCount > 0) {
                            BadgedBox(badge = {
                                Badge(containerColor = AccentBlue) {
                                    Text("$pendingQuestionsCount")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.QuestionAnswer,
                                    contentDescription = "Q&A",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.QuestionAnswer,
                                contentDescription = "Q&A",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Q&A",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == ScreenTab.QNA) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = YouTubeRed,
                        selectedTextColor = YouTubeRed,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = StudioCardBgElevated
                    ),
                    modifier = Modifier.testTag("nav_tab_qna")
                )

                // 4. POLL Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.POLL,
                    onClick = { viewModel.setTab(ScreenTab.POLL) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Poll,
                            contentDescription = "POLL",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "POLL",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == ScreenTab.POLL) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = YouTubeRed,
                        selectedTextColor = YouTubeRed,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = StudioCardBgElevated
                    ),
                    modifier = Modifier.testTag("nav_tab_poll")
                )

                // 5. OVERLAY Tab
                NavigationBarItem(
                    selected = currentTab == ScreenTab.OVERLAY,
                    onClick = { viewModel.setTab(ScreenTab.OVERLAY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = "OVERLAY",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "OVERLAY",
                            fontSize = 11.sp,
                            fontWeight = if (currentTab == ScreenTab.OVERLAY) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = YouTubeRed,
                        selectedTextColor = YouTubeRed,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = StudioCardBgElevated
                    ),
                    modifier = Modifier.testTag("nav_tab_overlay")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.LIVE -> {
                    LiveDashboardScreen(
                        viewModel = viewModel,
                        onNavigateTab = { tab -> viewModel.setTab(tab) },
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
                ScreenTab.CHAT -> {
                    LiveChatScreen(viewModel = viewModel)
                }
                ScreenTab.QNA -> {
                    QnaScreen(viewModel = viewModel)
                }
                ScreenTab.POLL -> {
                    PollScreen(viewModel = viewModel)
                }
                ScreenTab.OVERLAY -> {
                    OverlayPreviewScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsAndAboutDialog(
            viewModel = viewModel,
            onConnectYouTube = onConnectYouTube,
            onDismiss = { showSettingsDialog = false }
        )
    }
}
