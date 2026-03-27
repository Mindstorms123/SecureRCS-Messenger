package com.securercs.messenger.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.securercs.messenger.data.model.ServiceAccount
import com.securercs.messenger.data.repository.MessengerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {
    private val _services = MutableStateFlow<List<ServiceAccount>>(emptyList())
    val services: StateFlow<List<ServiceAccount>> = _services

    init {
        _services.value = MessengerRepository.getAvailableServices()
    }

    fun connectService(service: String) {
        MessengerRepository.connectService(service)
        _services.value = MessengerRepository.getAvailableServices()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel() as T
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginComplete: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory())
) {
    val services by viewModel.services.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verbinde Messenger-Dienste") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔐 SecureRCS Messenger",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Wähle die Messenger-Dienste, die du verwenden möchtest",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(services) { service ->
                ServiceLoginCard(
                    service = service,
                    onConnect = {
                        viewModel.connectService(service.service)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onLoginComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = services.any { it.isConnected }
                ) {
                    Text("Fertig - Zu den Chats")
                }
            }
        }
    }
}

@Composable
fun ServiceLoginCard(
    service: ServiceAccount,
    onConnect: () -> Unit,
) {
    val backgroundColor = MessengerRepository.getServiceColor(service.service)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = service.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = service.handle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (service.isConnected) {
                ElevatedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Verbunden",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Verbunden")
                }
            } else {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verbinden")
                }
            }
        }
    }
}
