package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LivePoll
import com.example.data.model.PollOption
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
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LiveEngagementViewModel

@Composable
fun PollScreen(
    viewModel: LiveEngagementViewModel,
    modifier: Modifier = Modifier
) {
    val currentPoll by viewModel.currentPoll.collectAsState()

    var showCreateForm by remember { mutableStateOf(false) }
    var newQuestion by remember { mutableStateOf("") }
    val newOptions = remember { mutableStateListOf("", "") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("poll_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Live Poll Card
        item {
            ActivePollDisplayCard(
                poll = currentPoll,
                onToggleActive = { viewModel.togglePollActive() },
                onToggleOverlay = { show -> viewModel.togglePollOverlay(show) }
            )
        }

        // Toggle Create New Poll Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CREATE / CHANGE POLL",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                TextButtonSmall(
                    label = if (showCreateForm) "Cancel" else "+ New Custom Poll",
                    onClick = { showCreateForm = !showCreateForm }
                )
            }
        }

        if (showCreateForm) {
            item {
                CreatePollCard(
                    question = newQuestion,
                    onQuestionChanged = { newQuestion = it },
                    options = newOptions,
                    onOptionChanged = { index, text -> newOptions[index] = text },
                    onAddOption = {
                        if (newOptions.size < 5) newOptions.add("")
                    },
                    onRemoveOption = { index ->
                        if (newOptions.size > 2) newOptions.removeAt(index)
                    },
                    onStartPoll = {
                        if (newQuestion.isNotBlank() && newOptions.count { it.isNotBlank() } >= 2) {
                            viewModel.createNewPoll(newQuestion.trim(), newOptions.toList())
                            showCreateForm = false
                        }
                    }
                )
            }
        }

        // Quick Presets Section
        item {
            Text(
                text = "QUICK POLL PRESETS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            PresetPollItem(
                title = "आज Live में क्या करें?",
                options = listOf("खेती", "Gaming", "Comedy", "Technology"),
                onClick = {
                    viewModel.createNewPoll(
                        "आज Live में क्या करें?",
                        listOf("खेती (Agriculture)", "Gaming", "Comedy", "Technology")
                    )
                }
            )
        }

        item {
            PresetPollItem(
                title = "आवाज़ और वीडियो कैसा है?",
                options = listOf("हाँ, एकदम साफ़ है 👍", "थोड़ी आवाज़ कम है", "वीडियो में अड़चन है"),
                onClick = {
                    viewModel.createNewPoll(
                        "क्या आपको मेरी आवाज़ और वीडियो साफ़ आ रही है?",
                        listOf("हाँ, एकदम साफ़ है 👍", "थोड़ी आवाज़ कम है", "वीडियो में अड़चन है")
                    )
                }
            )
        }

        item {
            PresetPollItem(
                title = "अगला टॉपिक क्या होना चाहिए?",
                options = listOf("आधुनिक कृषि यंत्र", "सरकारी किसान योजनाएं", "जैविक खेती", "ड्रिप सिंचाई"),
                onClick = {
                    viewModel.createNewPoll(
                        "अगला टॉपिक क्या होना चाहिए?",
                        listOf("आधुनिक कृषि यंत्र", "सरकारी किसान योजनाएं", "जैविक खेती", "ड्रिप सिंचाई")
                    )
                }
            )
        }
    }
}

@Composable
private fun ActivePollDisplayCard(
    poll: LivePoll,
    onVote: (Int) -> Unit,
    onToggleActive: () -> Unit,
    onReset: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(if (poll.isActive) Color(0xFF00E676) else TextTertiary, StudioCardBorder)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_poll_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Status badge, Total votes, Overlay switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (poll.isActive) Color(0xFF00E676).copy(alpha = 0.2f) else StudioCardBgElevated,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (poll.isActive) "🔴 LIVE VOTING" else "POLL ENDED",
                            color = if (poll.isActive) Color(0xFF00E676) else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "कुल वोट्स: ${poll.totalVotes}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Show on Overlay Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = if (poll.showOnOverlay) AccentBlue else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = poll.showOnOverlay,
                        onCheckedChange = onToggleOverlay,
                        modifier = Modifier.size(36.dp, 24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentBlue,
                            checkedTrackColor = AccentBlue.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Poll Question
            Text(
                text = poll.question,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(16.dp))

            // Poll Options with animated percentage bars
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                poll.options.forEach { option ->
                    val percentage = poll.percentageFor(option.id)
                    PollOptionBar(
                        option = option,
                        percentage = percentage,
                        isActive = poll.isActive,
                        onVote = { onVote(option.id) }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Bottom controls: End/Reopen Poll | Reset Votes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (poll.isActive) LiveRed else Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .testTag("btn_toggle_poll_active")
                ) {
                    Icon(
                        imageVector = if (poll.isActive) Icons.Default.StopCircle else Icons.Default.HowToVote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (poll.isActive) "END POLL" else "REOPEN POLL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_reset_poll")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset Votes", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PollOptionBar(
    option: PollOption,
    percentage: Int,
    isActive: Boolean,
    onVote: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        label = "pollProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StudioCardBgElevated)
            .clickable(enabled = isActive, onClick = onVote)
            .testTag("poll_option_${option.id}")
    ) {
        // Progress fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = animatedProgress)
                .background(
                    Brush.horizontalGradient(
                        listOf(YouTubeRed.copy(alpha = 0.35f), YouTubeRed.copy(alpha = 0.65f))
                    )
                )
        )

        // Text & Percentage overlay
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(StudioCardBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${option.id}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = option.text,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${option.votes} votes",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "$percentage%",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun CreatePollCard(
    question: String,
    onQuestionChanged: (String) -> Unit,
    options: List<String>,
    onOptionChanged: (Int, String) -> Unit,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onStartPoll: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("create_poll_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "New Live Poll",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = question,
                onValueChange = onQuestionChanged,
                label = { Text("सवाल (Question)") },
                placeholder = { Text("उदा. आज Live में क्या करें?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_new_poll_question"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YouTubeRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "विकल्प (Options)",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            options.forEachIndexed { index, optionText ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = optionText,
                        onValueChange = { onOptionChanged(index, it) },
                        placeholder = { Text("विकल्प ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_poll_option_$index"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YouTubeRed,
                            unfocusedBorderColor = StudioCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    if (options.size > 2) {
                        IconButton(
                            onClick = { onRemoveOption(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextTertiary)
                        }
                    }
                }
            }

            if (options.size < 5) {
                TextButtonSmall(
                    label = "+ विकल्प जोड़ें (Add Option)",
                    onClick = onAddOption
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onStartPoll,
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_submit_new_poll")
            ) {
                Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Live Poll", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PresetPollItem(
    title: String,
    options: List<String>,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "लागू करें",
                    color = AccentBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = options.joinToString(" • "),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
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
