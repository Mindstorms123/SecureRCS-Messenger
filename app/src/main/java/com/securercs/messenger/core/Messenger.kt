package com.securercs.messenger.core

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

class RiskAcknowledgmentRequired(val service: String, val warning: String) :
    IllegalStateException("$service requires acknowledgment: $warning")

data class Message(
    val sender: String,
    val recipient: String,
    val content: String,
    val protocol: String,
    val timestamp: Instant = Instant.now(),
) {
    fun copyForProtocol(protocol: String): Message = copy(protocol = protocol)
}

data class StoredMessage(
    val sender: String,
    val recipient: String,
    val content: String,
    val protocol: String,
    val via: String,
    val timestamp: String,
)

data class HistorySnapshot(
    val outbox: List<StoredMessage>,
    val inbox: List<StoredMessage>,
    val audit: List<String>,
)

data class SendRequest(
    val service: String,
    val sender: String,
    val recipient: String,
    val content: String,
    val targets: List<String> = emptyList(),
)

class LocalStore {
    private val formatter = DateTimeFormatter.ISO_INSTANT
    private val outbox = mutableListOf<StoredMessage>()
    private val inbox = mutableListOf<StoredMessage>()
    private val audit = mutableListOf<String>()

    private fun serialize(message: Message, via: String): StoredMessage =
        StoredMessage(
            sender = message.sender,
            recipient = message.recipient,
            content = message.content,
            protocol = message.protocol,
            via = via,
            timestamp = formatter.format(message.timestamp),
        )

    fun recordOutgoing(message: Message, via: String) {
        outbox.add(serialize(message, via))
    }

    fun recordIncoming(message: Message, via: String) {
        inbox.add(serialize(message, via))
    }

    fun recordAudit(entry: String) {
        audit.add("${formatter.format(Instant.now())} $entry")
    }

    fun history(): HistorySnapshot = HistorySnapshot(
        outbox = outbox.toList(),
        inbox = inbox.toList(),
        audit = audit.toList(),
    )
}

class RiskRegistry(customRisks: Map<String, String>? = null) {
    private val risks: Map<String, String> = DEFAULT_RISKS + (customRisks ?: emptyMap())

    fun getWarning(service: String): String? = risks[normalize(service)]

    fun describe(): Map<String, String> = risks.toMap()

    private fun normalize(service: String): String = service.trim().lowercase(Locale.getDefault())

    companion object {
        val DEFAULT_RISKS: Map<String, String> = mapOf(
            "sms" to "SMS is not end-to-end encrypted and can be intercepted by carriers.",
            "rcs" to "Carrier-managed RCS may leak metadata; E2EE depends on client and carrier support.",
            "matrix" to "Federated Matrix homeservers can observe metadata; trust only servers you control.",
            "matrix federation" to "Federated homeservers can observe metadata; trust only servers you control.",
            "whatsapp" to "Proprietary network; metadata and backups may be accessible to the provider.",
            "facebook messenger" to "Proprietary network; metadata can be profiled.",
        )
    }
}

open class Connector(
    val name: String,
    val protocol: String,
    private val store: LocalStore,
) {
    open fun send(message: Message) {
        store.recordOutgoing(message, name)
    }

    open fun receive(message: Message) {
        store.recordIncoming(message, name)
    }
}

class MatrixConnector(store: LocalStore) : Connector(name = "matrix", protocol = "matrix", store = store)
class RCSConnector(store: LocalStore) : Connector(name = "rcs", protocol = "rcs", store = store)
class ThirdPartyConnector(name: String, store: LocalStore) : Connector(name = name, protocol = name, store = store)

class MessengerService(
    private val store: LocalStore,
    private val registry: RiskRegistry,
) {
    private val connectors: MutableMap<String, Connector> = mutableMapOf()
    private val bridges: MutableMap<String, MutableList<String>> = mutableMapOf()
    private val acknowledged: MutableSet<String> = mutableSetOf()

    private fun normalize(service: String): String = service.trim().lowercase(Locale.getDefault())

    fun addConnector(connector: Connector) {
        connectors[normalize(connector.name)] = connector
        store.recordAudit("connector-added ${connector.name}")
    }

    fun services(): List<String> = connectors.keys.sorted()

    fun acknowledgeService(service: String): String? {
        val norm = normalize(service)
        val warning = registry.getWarning(norm)
        acknowledged.add(norm)
        warning?.let {
            store.recordAudit("warning-acknowledged $norm: $it")
        }
        return warning
    }

    private fun ensureAcknowledged(service: String) {
        val norm = normalize(service)
        val warning = registry.getWarning(norm)
        if (warning != null && norm !in acknowledged) {
            throw RiskAcknowledgmentRequired(norm, warning)
        }
    }

    private fun connectorFor(service: String): Connector {
        val norm = normalize(service)
        return connectors[norm] ?: error("No connector registered for $service")
    }

    fun connectServices(origin: String, targets: List<String>) {
        val normOrigin = normalize(origin)
        val stored = bridges.getOrPut(normOrigin) { mutableListOf() }
        targets.map(::normalize).forEach { target ->
            if (target !in stored) stored.add(target)
        }
        store.recordAudit("bridge-updated $normOrigin->$stored")
    }

    fun sendMessage(
        service: String,
        sender: String,
        recipient: String,
        content: String,
        targets: List<String> = emptyList(),
    ) {
        val norm = normalize(service)
        ensureAcknowledged(norm)
        val connector = connectorFor(norm)
        val message = Message(sender = sender, recipient = recipient, content = content, protocol = connector.protocol)
        connector.send(message)
        bridges[norm].orEmpty().plus(targets.map(::normalize)).distinct().forEach { target ->
            if (!connectors.containsKey(target)) return@forEach
            ensureAcknowledged(target)
            connectorFor(target).send(message.copyForProtocol(connectorFor(target).protocol))
        }
    }

    fun receiveMessage(service: String, message: Message) {
        val norm = normalize(service)
        ensureAcknowledged(norm)
        val connector = connectorFor(norm)
        connector.receive(message)
        bridges[norm].orEmpty().forEach { target ->
            if (!connectors.containsKey(target)) return@forEach
            ensureAcknowledged(target)
            connectorFor(target).receive(message.copyForProtocol(connectorFor(target).protocol))
        }
    }

    fun pendingWarnings(): Map<String, String> =
        registry.describe().filter { (service, _) ->
            val norm = normalize(service)
            norm !in acknowledged && (connectors.keys.contains(norm) || bridges.values.flatten().contains(norm))
        }

    fun history(): HistorySnapshot = store.history()
}
