package com.securercs.messenger.ui.screens.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.securercs.messenger.data.model.Conversation
import com.securercs.messenger.data.repository.MessengerRepository
import com.securercs.messenger.ui.components.Avatar
import com.securercs.messenger.ui.components.ServiceBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val conversations = MessengerRepository.getConversations()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kontakte") }
            )
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
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Kontakte",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Keine Kontakte",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(conversations) { conversation ->
                    ContactItem(conversation = conversation)
                }
            }
        }
    }
}

@Composable
fun ContactItem(
    conversation: Conversation,
) {
    val serviceColor = MessengerRepository.getServiceColor(conversation.service)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = conversation.contactName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = conversation.contactHandle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ServiceBadge(
                        service = conversation.service,
                        backgroundColor = serviceColor,
                    )
                    if (conversation.isOnline) {
                        Text(
                            text = "🟢",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}
