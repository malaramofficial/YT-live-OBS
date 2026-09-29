package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LiveRed
import com.example.ui.theme.ModeratorGreen
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBgElevated
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.SuperChatGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LiveEngagementViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveChatScreen(
    viewModel: LiveEngagementViewModel,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var filterType by remember { mutableStateOf("ALL") } // ALL, SUPER, QUESTIONS, HIGHLIGHTS
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var replyText by remember { mutableStateOf("") }
    var messageToDelete by remember { mutableStateOf<ChatMessage?>(null) }
    var quickChatText by remember { mutableStateOf("") }

    val filteredMessages = when (filterType) {
        "SUPER" -> chatMessages.filter { it.isSuperChat }
        "QUESTIONS" -> chatMessages.filter { it.messageText.contains("?") || it.messageText.contains("क्या") || it.messageText.contains("कैसे") || it.messageText.contains("क्यूं") }
        "HIGHLIGHTS" -> chatMessages.filter { it.isHighlighted }
        else -> chatMessages
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("live_chat_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header stats & filter chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioCardBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE CHAT FEED",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${filteredMessages.size} Messages",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Filter row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == "ALL",
                        onClick = { filterType = "ALL" },
                        label = { Text("सभी (${chatMessages.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YouTubeRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_all_chats")
                    )
                    FilterChip(
                        selected = filterType == "SUPER",
                        onClick = { filterType = "SUPER" },
                        label = { Text("Super Chat (${chatMessages.count { it.isSuperChat }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SuperChatGold,
                            selectedLabelColor = StudioBlack
                        ),
                        modifier = Modifier.testTag("filter_super_chats")
                    )
                    FilterChip(
                        selected = filterType == "QUESTIONS",
                        onClick = { filterType = "QUESTIONS" },
                        label = { Text("सवाल (Q&A)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_questions")
                    )
                }
            }

            // Message list
            if (filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (filterType == "ALL") "प्रतीक्षा करें, लाइव चैट संदेश यहाँ दिखेंगे..." else "इस फ़िल्टर में कोई संदेश नहीं है",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMessages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onReply = {
                                replyToMessage = msg
                                replyText = "@${msg.authorName} "
                            },
                            onAddToQna = {
                                viewModel.addChatToQna(msg)
                                scope.launch {
                                    snackbarHostState.showSnackbar("सवाल Q&A लिस्ट में जोड़ दिया गया!")
                                }
                            },
                            onHighlight = {
                                viewModel.highlightMessage(msg.id)
                            },
                            onDelete = {
                                messageToDelete = msg
                            }
                        )
                    }
                }
            }

            // Quick Chat Input at bottom
            SurfaceChatBar(
                text = quickChatText,
                onTextChanged = { quickChatText = it },
                onSend = {
                    if (quickChatText.isNotBlank()) {
                        viewModel.sendChatReply(quickChatText.trim())
                        quickChatText = ""
                        scope.launch {
                            snackbarHostState.showSnackbar("संदेश चैट में भेजा गया")
                        }
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
        )

        // Reply Dialog
        if (replyToMessage != null) {
            AlertDialog(
                onDismissRequest = { replyToMessage = null },
                containerColor = StudioCardBg,
                title = {
                    Text(
                        text = "उत्तर दें: ${replyToMessage?.authorName}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "\"${replyToMessage?.messageText}\"",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .background(StudioCardBgElevated, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                                .fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("अपना उत्तर लिखें...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reply_text"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YouTubeRed,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                viewModel.sendChatReply(replyText.trim())
                                replyToMessage = null
                                replyText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                        modifier = Modifier.testTag("btn_confirm_reply")
                    ) {
                        Text("Send Reply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { replyToMessage = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (messageToDelete != null) {
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                containerColor = StudioCardBg,
                title = { Text("टिप्पणी हटाएं?", color = TextPrimary) },
                text = {
                    Text(
                        text = "क्या आप ${messageToDelete?.authorName} की यह टिप्पणी लाइव चैट से हटाना चाहते हैं?",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            messageToDelete?.let { viewModel.deleteMessage(it.id) }
                            messageToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                        modifier = Modifier.testTag("btn_confirm_delete")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToDelete = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onReply: () -> Unit,
    onAddToQna: () -> Unit,
    onHighlight: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    val cardBorder = if (message.isHighlighted) {
        BorderStrokeModifier(1.5.dp, SuperChatGold)
    } else if (message.isSuperChat) {
        BorderStrokeModifier(1.dp, SuperChatGold.copy(alpha = 0.6f))
    } else {
        BorderStrokeModifier(0.5.dp, StudioCardBorder)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isHighlighted) {
                StudioCardBgElevated
            } else if (message.isSuperChat) {
                Color(0xFF282110)
            } else {
                StudioCardBg
            }
        ),
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_item_${message.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Avatar, Username, Badges, Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Photo
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StudioCardBgElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (message.authorPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = message.authorPhotoUrl,
                            contentDescription = message.authorName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = message.authorName.take(1).uppercase(),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.authorName,
                            color = if (message.isSuperChat) SuperChatGold else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (message.isModerator) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Moderator",
                                tint = ModeratorGreen,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        if (message.isMember) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Member",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Text(
                        text = timeFormatted,
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }

                // Super Chat pill
                if (message.isSuperChat && message.superChatAmount != null) {
                    Text(
                        text = message.superChatAmount,
                        color = StudioBlack,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(SuperChatGold, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Comment Text
            Text(
                text = message.messageText,
                color = if (message.isDeleted) TextTertiary else TextPrimary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(start = 2.dp)
            )

            Spacer(Modifier.height(10.dp))

            // Action Buttons Row: Reply | Add to Q&A | Highlight | Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reply Button
                ActionButtonSmall(
                    icon = Icons.Default.Reply,
                    label = "Reply",
                    tint = AccentBlue,
                    testTag = "btn_reply_${message.id}",
                    onClick = onReply
                )

                // Add to Q&A Button
                ActionButtonSmall(
                    icon = if (message.isAddedToQna) Icons.Default.Check else Icons.Default.HelpOutline,
                    label = if (message.isAddedToQna) "In Q&A" else "+ Q&A",
                    tint = if (message.isAddedToQna) Color(0xFF00E676) else Color(0xFFFF9800),
                    testTag = "btn_add_qna_${message.id}",
                    onClick = onAddToQna
                )

                // Highlight Button
                ActionButtonSmall(
                    icon = if (message.isHighlighted) Icons.Default.Star else Icons.Default.StarBorder,
                    label = if (message.isHighlighted) "Highlighted" else "Highlight",
                    tint = if (message.isHighlighted) SuperChatGold else TextSecondary,
                    testTag = "btn_highlight_${message.id}",
                    onClick = onHighlight
                )

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_delete_${message.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete comment",
                        tint = TextTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonSmall(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .background(StudioCardBgElevated)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SurfaceChatBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioCardBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            placeholder = { Text("लाइव चैट में मैसेज लिखें...", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("input_live_chat_msg"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YouTubeRed,
                unfocusedBorderColor = StudioCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(Modifier.width(8.dp))

        Button(
            onClick = onSend,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
            modifier = Modifier
                .height(50.dp)
                .testTag("btn_send_live_chat")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun BorderStrokeModifier(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
