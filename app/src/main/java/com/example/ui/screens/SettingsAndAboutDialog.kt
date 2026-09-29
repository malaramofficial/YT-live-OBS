package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun SettingsAndAboutDialog(
    viewModel: LiveEngagementViewModel,
    onConnectYouTube: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentTitle by viewModel.channelTitle.collectAsState()
    val currentId by viewModel.channelId.collectAsState()
    val currentApiKey by viewModel.apiKey.collectAsState()
    val currentToken by viewModel.oauthToken.collectAsState()
    val isConnected by viewModel.isAccountConnected.collectAsState()

    var channelTitle by remember { mutableStateOf(currentTitle) }
    var channelId by remember { mutableStateOf(currentId) }
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var oauthToken by remember { mutableStateOf(currentToken) }

    LaunchedEffect(currentToken) {
        oauthToken = currentToken
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCardBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings & About",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // About Section - Malaram Live Branding
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCardBgElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(YouTubeRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartDisplay,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Malaram Live Engagement Tool",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Made by Malaramofficial",
                                    color = VerifiedBadgeColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "YouTube Live Streamers के लिए ऑल-इन-वन कंट्रोल सेंटर: लाइव चैट मॉडरेशन, Q&A कतार, रीयल-टाइम पोल और OBS ओवरले।",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // YouTube API Credentials
                Text(
                    text = "YOUTUBE API CONFIGURATION",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = channelTitle,
                    onValueChange = { channelTitle = it },
                    label = { Text("Channel Name / Branding") },
                    placeholder = { Text("Malaram Official") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_channel_title"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YouTubeRed,
                        unfocusedBorderColor = StudioCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = channelId,
                    onValueChange = { channelId = it },
                    label = { Text("YouTube Channel ID") },
                    placeholder = { Text("UCxxxxxxxxxxxxxxxxxx") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_channel_id"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YouTubeRed,
                        unfocusedBorderColor = StudioCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("YouTube Data API v3 Key (Optional)") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_api_key"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YouTubeRed,
                        unfocusedBorderColor = StudioCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Button(
                    onClick = onConnectYouTube,
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_connect_youtube")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (isConnected && currentToken.isNotBlank()) "YouTube Connected" else "Connect Google / YouTube",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "Google authorization is used for your YouTube account. Manual OAuth tokens are kept only as an advanced fallback.",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                OutlinedTextField(
                    value = oauthToken,
                    onValueChange = { oauthToken = it },
                    label = { Text("OAuth Bearer Token (For Chat Sending/Deleting)") },
                    placeholder = { Text("ya29....") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_oauth_token"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YouTubeRed,
                        unfocusedBorderColor = StudioCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                if (isConnected) {
                    OutlinedButton(
                        onClick = {
                            viewModel.disconnectYouTube()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_disconnect_account")
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Disconnect YouTube Account", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveSettings(
                        channelTitle = channelTitle.trim(),
                        channelId = channelId.trim(),
                        apiKey = apiKey.trim(),
                        oauthToken = oauthToken.trim()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_settings")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
