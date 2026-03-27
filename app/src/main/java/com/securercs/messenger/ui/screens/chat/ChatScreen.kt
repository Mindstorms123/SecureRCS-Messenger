package com.securercs.messenger.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securercs.messenger.data.model.Conversation
import com.securercs.messenger.data.repository.MessengerRepository
import com.securercs.messenger.ui.components.Avatar
import com.securercs.messenger.ui.components.MessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBackClick: () -> Unit,
) {
    val conversation = MessengerRepository.getConversationById(conversationId)
    val messages = MessengerRepository.getMessagesForConversation(conversationId)
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (conversation == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Konversation nicht gefunden")
        }
        return
    }

    val serviceColor = MessengerRepository.getServiceColor(conversation.service)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Avatar(
                            initials = conversation.avatarInitials,
                            backgroundColor = conversation.avatarColor,
                            size = 36.dp
                        )
                        Column {
                            Text(
                                text = conversation.contactName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = conversation.service.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Call, contentDescription = "Anruf")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Mehr")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        content = message.content,
                        timestamp = message.timestamp,
                        isOutgoing = message.isOutgoing,
                        status = message.status,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .imePadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Datei anhängen",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .weight(1f),
                    placeholder = { Text("Nachricht...") },
                    maxLines = 3,
                )

                IconButton(
                    onClick = {
                        if (messageText.isNotEmpty()) {
                            MessengerRepository.sendMessage(conversationId, messageText)
                            messageText = ""
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Senden",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
