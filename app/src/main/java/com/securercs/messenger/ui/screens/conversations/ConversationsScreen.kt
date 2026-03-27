package com.securercs.messenger.ui.screens.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.securercs.messenger.data.model.Conversation
import com.securercs.messenger.data.repository.MessengerRepository
import com.securercs.messenger.ui.components.Avatar
import com.securercs.messenger.ui.components.ServiceBadge
import com.securercs.messenger.ui.theme.PrimaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onConversationClick: (String) -> Unit,
) {
    val conversations = MessengerRepository.getConversations()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Mehr")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = PrimaryGreen,
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Neuer Chat")
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Noch keine Chats",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
) {
    val serviceColor = MessengerRepository.getServiceColor(conversation.service)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                initials = conversation.avatarInitials,
                backgroundColor = conversation.avatarColor,
                size = 48.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.contactName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = conversation.lastMessageTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (conversation.unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ServiceBadge(
                        service = conversation.service,
                        backgroundColor = serviceColor,
                    )

                    if (conversation.isOnline) {
                        Text(
                            text = "🟢 Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
