package com.securercs.messenger

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.focusable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securercs.messenger.R
import com.securercs.messenger.core.HistorySnapshot
import com.securercs.messenger.core.LocalStore
import com.securercs.messenger.core.MatrixConnector
import com.securercs.messenger.core.MessengerService
import com.securercs.messenger.core.RCSConnector
import com.securercs.messenger.core.RiskAcknowledgmentRequired
import com.securercs.messenger.core.RiskRegistry
import com.securercs.messenger.core.SendRequest
import com.securercs.messenger.core.StoredMessage
import com.securercs.messenger.core.ThirdPartyConnector
import com.securercs.messenger.ui.theme.SecureRCSMessengerTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val messengerViewModel: MessengerViewModel by viewModels { MessengerViewModel.factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureRCSMessengerTheme {
                MessengerScreen(viewModel = messengerViewModel)
            }
        }
    }
}

data class UiState(
    val pendingWarnings: Map<String, String> = emptyMap(),
    val outbox: List<StoredMessage> = emptyList(),
    val inbox: List<StoredMessage> = emptyList(),
    val audit: List<String> = emptyList(),
    val services: List<String> = emptyList(),
    val status: String? = null,
    val pendingRisk: RiskAcknowledgmentRequired? = null,
    val contacts: List<Contact> = emptyList(),
    val recentChats: List<ChatPreview> = emptyList(),
)

data class Contact(
    val name: String,
    val handle: String,
    val service: String,
)

data class ChatPreview(
    val title: String,
    val content: String,
    val via: String,
    val protocol: String,
    val timestamp: String,
)

private const val MAX_RECENT_CHATS = 10

private val DEFAULT_CONTACTS = listOf(
    Contact(name = "Matrix Space", handle = "@matrix:home", service = "matrix"),
    Contact(name = "RCS Bridge", handle = "+49 151 000000", service = "rcs"),
    Contact(name = "WhatsApp Bridge", handle = "+49 170 000000", service = "whatsapp"),
)

private val CHAT_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

class MessengerViewModel(
    private val messenger: MessengerService,
) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(buildUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<UiState> = _uiState

    private var lastPendingSend: SendRequest? = null

    fun sendMessage(request: SendRequest) {
        try {
            lastPendingSend = null
            messenger.sendMessage(
                service = request.service,
                sender = request.sender,
                recipient = request.recipient,
                content = request.content,
                targets = request.targets,
            )
            _uiState.value = buildUiState(status = "Nachricht lokal versendet.")
        } catch (risk: RiskAcknowledgmentRequired) {
            lastPendingSend = request
            _uiState.value = buildUiState(pendingRisk = risk)
        }
    }

    fun acknowledgeAndRetry(service: String) {
        messenger.acknowledgeService(service)
        val pending = lastPendingSend
        if (pending != null) {
            sendMessage(pending)
        } else {
            _uiState.value = buildUiState(status = "Risiko bestätigt für $service")
        }
    }

    fun dismissRisk() {
        lastPendingSend = null
        _uiState.value = buildUiState(status = "Senden abgebrochen")
    }

    private fun buildUiState(
        status: String? = null,
        pendingRisk: RiskAcknowledgmentRequired? = null,
    ): UiState {
        val history = messenger.history()
        val services = messenger.services()
        return UiState(
            pendingWarnings = messenger.pendingWarnings(),
            outbox = history.outbox,
            inbox = history.inbox,
            audit = history.audit,
            services = services,
            status = status,
            pendingRisk = pendingRisk,
            contacts = buildContacts(history, services),
            recentChats = buildRecentChats(history),
        )
    }

    private fun buildContacts(history: HistorySnapshot, services: List<String>): List<Contact> {
        val derived = sequenceOf(history.outbox.asSequence(), history.inbox.asSequence()).flatten().flatMap {
            val senderHandle = it.sender.trim()
            val recipientHandle = it.recipient.trim()
            listOf(
                Contact(name = resolveDisplayName(it.sender), handle = senderHandle, service = it.protocol),
                Contact(name = resolveDisplayName(it.recipient), handle = recipientHandle, service = it.protocol),
            )
        }.toList()
        val defaults = DEFAULT_CONTACTS.filter { default -> default.service in services }
        return (derived + defaults)
            .distinctBy { it.handle.trim() to it.service }
            .sortedBy { it.name.lowercase() }
    }

    private fun resolveDisplayName(raw: String): String {
        val trimmed = raw.trim()
        val base = trimmed.substringBefore("@").substringBefore(":").ifBlank { trimmed }
        return base.ifBlank { trimmed }
    }

    private fun buildRecentChats(history: HistorySnapshot): List<ChatPreview> {
        val fallbackInstant = Instant.now()
        return sequenceOf(history.outbox.asSequence(), history.inbox.asSequence())
            .flatten()
            .map { msg ->
                val parsedInstantResult = runCatching { Instant.parse(msg.timestamp) }
                val parsedInstant = parsedInstantResult.getOrElse {
                    Log.w("SecureRCS", "Could not parse timestamp: ${msg.timestamp}")
                    fallbackInstant
                }
                val displayTime = runCatching { CHAT_TIME_FORMATTER.format(parsedInstant) }.getOrElse { msg.timestamp }
                Triple(msg, parsedInstant, displayTime)
            }
            .sortedByDescending { it.second }
            .take(MAX_RECENT_CHATS)
            .map { (msg, _, displayTime) ->
                ChatPreview(
                    title = "${msg.sender} ↔ ${msg.recipient}",
                    content = msg.content,
                    via = msg.via,
                    protocol = msg.protocol,
                    timestamp = displayTime,
                )
            }
            .toList()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val store = LocalStore()
                val registry = RiskRegistry()
                val service = MessengerService(store = store, registry = registry).apply {
                    addConnector(MatrixConnector(store))
                    addConnector(RCSConnector(store))
                    addConnector(ThirdPartyConnector("whatsapp", store))
                    // Use Matrix as the hub: connectors bridge into Matrix, and Matrix fans out to the others (directional to avoid loops).
                    connectServices("rcs", listOf("matrix"))
                    connectServices("whatsapp", listOf("matrix"))
                    connectServices("matrix", listOf("rcs", "whatsapp"))
                }
                return MessengerViewModel(service) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen(
    // Initialisierung erfolgt hier als Default-Parameter, was in dieser Form
    // vom Compiler innerhalb der @Composable-Funktion akzeptiert wird.
    viewModel: MessengerViewModel = viewModel(factory = MessengerViewModel.factory())
) {
    // State-Beobachtung
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- STRINGS VORAB EXTRAHIEREN (Wichtig für Modifier-Blocks) ---
    val screenDesc = stringResource(R.string.screen_message_list_desc)
    val titleMatrix = stringResource(R.string.title_matrix_first)
    val subtitleModern = stringResource(R.string.subtitle_modern_dark)
    // ---------------------------------------------------------------

    // Status-Meldungen via Snackbar anzeigen
    uiState.status?.let { status ->
        LaunchedEffect(status) {
            snackbarHostState.showSnackbar(status)
        }
    }

    Scaffold(
        modifier = Modifier.semantics {
            // Nutzung der vorab extrahierten Variable
            contentDescription = screenDesc
        },
        topBar = {
            TopAppBar(title = { Text("SecureRCS Messenger") })
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = titleMatrix,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitleModern,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { ContactsCard(contacts = uiState.contacts) }

            item { RecentChatsCard(chats = uiState.recentChats) }

            item {
                PendingWarningsCard(
                    pending = uiState.pendingWarnings,
                    onAcknowledge = viewModel::acknowledgeAndRetry
                )
            }

            item {
                SendMessageCard(
                    services = uiState.services,
                    onSend = viewModel::sendMessage,
                )
            }

            item { HistoryCard(title = "Outbox", messages = uiState.outbox) }

            item { HistoryCard(title = "Inbox", messages = uiState.inbox) }

            item { AuditCard(entries = uiState.audit) }
        }
    }

    // Risiko-Dialog anzeigen, falls vorhanden
    uiState.pendingRisk?.let { risk ->
        RiskDialog(
            risk = risk,
            onConfirm = { viewModel.acknowledgeAndRetry(risk.service) },
            onCancel = viewModel::dismissRisk,
        )
    }
}

@Composable
fun ContactsCard(contacts: List<Contact>) {
    // 1. Strings vorab extrahieren, da semantics { } kein Composable-Kontext ist
    val labelContacts = stringResource(R.string.label_contacts)
    val contactsEmpty = stringResource(R.string.contacts_empty)
    val scrollHint = stringResource(R.string.contacts_scroll_hint)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = labelContacts,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (contacts.isEmpty()) {
                Text(text = contactsEmpty)
            } else {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .semantics {
                            // 2. Hier die Variable nutzen statt stringResource()
                            contentDescription = scrollHint
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    contacts.forEach { contact ->
                        ContactCard(contact)
                    }
                }
            }
        }
    }
}


@Composable
fun ContactCard(contact: Contact) {
    Card(
        modifier = Modifier
            .semantics { contentDescription = "${contact.name} (${contact.handle})" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(contact.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(contact.handle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(contact.service.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun RecentChatsCard(chats: List<ChatPreview>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.label_recent_chats), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (chats.isEmpty()) {
                Text(stringResource(R.string.recent_chats_empty))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chats.forEachIndexed { index, chat ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(chat.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(chat.content, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.chat_meta, chat.protocol.uppercase(), chat.via, chat.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (index != chats.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingWarningsCard(
    pending: Map<String, String>,
    onAcknowledge: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Risiken bestätigen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (pending.isEmpty()) {
                Text("Alle bekannten Risiken wurden bestätigt.")
            } else {
                pending.forEach { (service, warning) ->
                    WarningRow(service = service, warning = warning, onAcknowledge = onAcknowledge)

                }
            }
        }
    }
}

@Composable
fun WarningRow(service: String, warning: String, onAcknowledge: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(service.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(warning, style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { onAcknowledge(service) }) { Text("Risiko akzeptieren") }
            Text("Pflicht vor Nutzung", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SendMessageCard(
    services: List<String>,
    onSend: (SendRequest) -> Unit,
) {
    var origin by remember { mutableStateOf(services.firstOrNull().orEmpty()) }
    var sender by remember { mutableStateOf("alice") }
    var recipient by remember { mutableStateOf("bob") }
    var content by remember { mutableStateOf("Hallo von SecureRCS!") }
    val selectedTargets = remember { mutableStateListOf<String>() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nachricht senden / bridgen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ServiceSelector(
                services = services,
                selected = origin,
                onSelect = { new ->
                    origin = new
                    selectedTargets.clear()
                },
            )
            Text("Bridge-Ziele", style = MaterialTheme.typography.labelLarge)
            services.filter { it != origin }.forEach { target ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = target in selectedTargets,
                        onCheckedChange = { checked ->
                            if (checked) selectedTargets.add(target) else selectedTargets.remove(target)
                        },
                    )
                    Text(target)
                }
            }
            OutlinedTextField(value = sender, onValueChange = { sender = it }, label = { Text("Sender") })
            OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Empfänger") })
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Inhalt") })
            Button(
                onClick = {
                    onSend(
                        SendRequest(
                            service = origin,
                            sender = sender,
                            recipient = recipient,
                            content = content,
                            targets = selectedTargets.toList(),
                        ),
                    )
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Senden")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceSelector(
    services: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        services.forEach { service ->
            val isSelected = selected == service
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(service) },
                label = { Text(service) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryCard(title: String, messages: List<StoredMessage>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (messages.isEmpty()) {
                Text("Keine Einträge.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages) { msg ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${msg.sender} → ${msg.recipient} über ${msg.via} (${msg.protocol})")
                            Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                            Text(msg.timestamp, style = MaterialTheme.typography.bodySmall)
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun AuditCard(entries: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Audit Trail (lokal)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (entries.isEmpty()) {
                Text("Keine Audit-Einträge.")
            } else {
                entries.forEach { entry ->
                    Text(entry, style = MaterialTheme.typography.bodyMedium)

                }
            }
        }
    }
}

@Composable
fun RiskDialog(
    risk: RiskAcknowledgmentRequired,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = onConfirm) { Text("Risiko bestätigen") }
        },
        dismissButton = {
            Button(onClick = onCancel) { Text("Abbrechen") }
        },
        title = { Text("⚠️ Risiko für ${risk.service}") },
        text = { Text(risk.warning) },
    )
}
