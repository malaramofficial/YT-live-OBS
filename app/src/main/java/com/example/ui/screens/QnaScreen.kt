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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.QnaEntity
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.LiveRed
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBgElevated
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LiveEngagementViewModel

@Composable
fun QnaScreen(
    viewModel: LiveEngagementViewModel,
    modifier: Modifier = Modifier
) {
    val qnaList by viewModel.qnaList.collectAsState()
    val spotlightQuestion by viewModel.spotlightQuestion.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active Queue, 1: Answered
    var showAddDialog by remember { mutableStateOf(false) }

    val pendingQuestions = qnaList.filter { !it.isAnswered }
    val answeredQuestions = qnaList.filter { it.isAnswered }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBlack)
            .testTag("qna_screen")
    ) {
        // Spotlight Question Card (Hero Section for Creator while live)
        SpotlightQuestionCard(
            question = spotlightQuestion,
            onNextQuestion = { viewModel.nextQuestion() },
            onMarkAnswered = { viewModel.markCurrentQuestionAnswered() },
            onRemove = {
                spotlightQuestion?.let { viewModel.removeQuestion(it.id) }
            },
            onAddNew = { showAddDialog = true }
        )

        // Tab Row: Queue vs Answered
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioCardBg,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = YouTubeRed
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Questions Queue (${pendingQuestions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_questions_queue")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "Answered (${answeredQuestions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_questions_answered")
            )
        }

        // List Section
        val displayList = if (selectedTab == 0) pendingQuestions else answeredQuestions
        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QuestionAnswer,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (selectedTab == 0) "कोई सवाल कतार में नहीं है" else "अभी तक कोई उत्तरित सवाल नहीं है",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "लाइव चैट से सवाल जोड़ें या नया सवाल बनाएं",
                        color = TextTertiary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("सवाल जोड़ें")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(displayList, key = { _, item -> item.id }) { index, item ->
                    QuestionListItem(
                        index = index + 1,
                        item = item,
                        isCurrentSpotlight = item.id == spotlightQuestion?.id,
                        onSpotlight = { viewModel.setQuestionSpotlight(item.id) },
                        onRemove = { viewModel.removeQuestion(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddQuestionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { question, author ->
                viewModel.addCustomQuestion(question, author)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SpotlightQuestionCard(
    question: QnaEntity?,
    onNextQuestion: () -> Unit,
    onMarkAnswered: () -> Unit,
    onRemove: () -> Unit,
    onAddNew: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spotlight_question_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Live Indicator & Overlay Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFF9800), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "CURRENT QUESTION",
                            color = StudioBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(StudioCardBgElevated, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Live On Overlay",
                            color = AccentBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onAddNew,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Question",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (question != null) {
                // Author name with avatar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(StudioCardBgElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = question.authorName.take(1).uppercase(),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = question.authorName,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // The Big Question Text (easy to read while live!)
                Text(
                    text = question.questionText,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCardBgElevated, RoundedCornerShape(10.dp))
                        .padding(14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCardBgElevated, RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "कोई सवाल अभी स्पॉटलाइट में नहीं है।\nनीचे सूची से सवाल चुनें या NEXT QUESTION दबाएं।",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Prominent Action Buttons: NEXT QUESTION | ANSWERED | REMOVE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // NEXT QUESTION Button (Primary, large)
                Button(
                    onClick = onNextQuestion,
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                        .testTag("btn_next_question")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "NEXT QUESTION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // ANSWERED Button (Success)
                Button(
                    onClick = onMarkAnswered,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(50.dp)
                        .testTag("btn_answered")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "ANSWERED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // REMOVE Button
                OutlinedButton(
                    onClick = onRemove,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(50.dp)
                        .testTag("btn_remove_question")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "REMOVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionListItem(
    index: Int,
    item: QnaEntity,
    isCurrentSpotlight: Boolean,
    onSpotlight: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSpotlight) StudioCardBgElevated else StudioCardBg
        ),
        border = if (isCurrentSpotlight) {
            androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF9800))
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, StudioCardBorder)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSpotlight)
            .testTag("qna_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Index number badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isCurrentSpotlight) Color(0xFFFF9800) else StudioCardBgElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    color = if (isCurrentSpotlight) StudioBlack else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.authorName,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    if (isCurrentSpotlight) {
                        Text(
                            text = "🔴 ACTIVE ON LIVE",
                            color = Color(0xFFFF9800),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else if (item.isAnswered) {
                        Text(
                            text = "✓ Answered",
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.questionText,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddQuestionDialog(
    onDismiss: () -> Unit,
    onAdd: (question: String, author: String) -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    var authorName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCardBg,
        title = {
            Text(
                text = "नया सवाल जोड़ें (Add Question)",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("सवाल (Question)") },
                    placeholder = { Text("उदा. आधुनिक ड्रिप सिंचाई की क्या सब्सिडी है?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_qna_question"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YouTubeRed,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("पूछने वाले का नाम (Viewer Name)") },
                    placeholder = { Text("उदा. रमेश कुमार") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_qna_author"),
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
                    if (questionText.isNotBlank()) {
                        onAdd(questionText.trim(), authorName.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                modifier = Modifier.testTag("btn_confirm_add_qna")
            ) {
                Text("Add to Q&A")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
