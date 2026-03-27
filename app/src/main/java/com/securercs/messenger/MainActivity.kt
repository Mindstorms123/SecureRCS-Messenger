package com.securercs.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
)

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
        return UiState(
            pendingWarnings = messenger.pendingWarnings(),
            outbox = history.outbox,
            inbox = history.inbox,
            audit = history.audit,
            services = messenger.services(),
            status = status,
            pendingRisk = pendingRisk,
        )
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
                    connectServices("matrix", listOf("rcs", "whatsapp"))
                }
                return MessengerViewModel(service) as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen(viewModel: MessengerViewModel = viewModel(factory = MessengerViewModel.factory())) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    uiState.status?.let { status ->
        LaunchedEffect(status) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(status)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SecureRCS Messenger") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PendingWarningsCard(pending = uiState.pendingWarnings, onAcknowledge = viewModel::acknowledgeAndRetry)
            SendMessageCard(
                services = uiState.services,
                onSend = viewModel::sendMessage,
            )
            HistoryCard(title = "Outbox", messages = uiState.outbox)
            HistoryCard(title = "Inbox", messages = uiState.inbox)
            AuditCard(entries = uiState.audit)
        }
    }

    uiState.pendingRisk?.let { risk ->
        RiskDialog(
            risk = risk,
            onConfirm = { viewModel.acknowledgeAndRetry(risk.service) },
            onCancel = viewModel::dismissRisk,
        )
    }
}

@Composable
fun PendingWarningsCard(
    pending: Map<String, String>,
    onAcknowledge: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Risiken bestätigen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (pending.isEmpty()) {
                Text("Alle bekannten Risiken wurden bestätigt.")
            } else {
                pending.forEach { (service, warning) ->
                    WarningRow(service = service, warning = warning, onAcknowledge = onAcknowledge)
                    Divider()
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
            Text("Pflicht vor Nutzung", style = MaterialTheme.typography.bodySmall)
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
                        Divider()
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
                    Divider()
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
