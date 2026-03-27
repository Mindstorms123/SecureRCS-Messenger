from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Dict, Iterable, List, Optional, Set


class RiskAcknowledgmentRequired(Exception):
    """Raised when a service with known risks is used without user acknowledgment."""

    def __init__(self, service: str, warning: str) -> None:
        self.service = service
        self.warning = warning
        super().__init__(f"{service!r} requires acknowledgment: {warning}")


@dataclass
class Message:
    sender: str
    recipient: str
    content: str
    protocol: str
    timestamp: datetime = field(default_factory=lambda: datetime.now(timezone.utc))

    def copy_for_protocol(self, protocol: str) -> "Message":
        return Message(
            sender=self.sender,
            recipient=self.recipient,
            content=self.content,
            protocol=protocol,
        )


class LocalStore:
    """Local-only storage to keep every message and warning on device."""

    def __init__(self) -> None:
        self.outbox: List[Dict] = []
        self.inbox: List[Dict] = []
        self.audit: List[str] = []

    @staticmethod
    def _serialize(message: Message, via: str) -> Dict:
        return {
            "sender": message.sender,
            "recipient": message.recipient,
            "content": message.content,
            "protocol": message.protocol,
            "via": via,
            "timestamp": message.timestamp.isoformat(),
        }

    def record_outgoing(self, message: Message, via: str) -> None:
        self.outbox.append(self._serialize(message, via))

    def record_incoming(self, message: Message, via: str) -> None:
        self.inbox.append(self._serialize(message, via))

    def record_audit(self, entry: str) -> None:
        self.audit.append(f"{datetime.now(timezone.utc).isoformat()} {entry}")

    def history(self) -> Dict[str, List[Dict]]:
        return {"outbox": list(self.outbox), "inbox": list(self.inbox), "audit": list(self.audit)}


class RiskRegistry:
    """Keeps a list of risks per service/protocol to warn the user before usage."""

    DEFAULT_RISKS: Dict[str, str] = {
        "sms": "SMS is not end-to-end encrypted and can be intercepted by carriers.",
        "rcs": "Carrier-managed RCS may leak metadata; E2EE depends on client and carrier support.",
        "matrix federation": "Federated homeservers can observe metadata; trust only servers you control.",
        "whatsapp": "Proprietary network; metadata and backups may be accessible to the provider.",
        "facebook messenger": "Proprietary network; metadata can be profiled.",
    }

    def __init__(self, custom_risks: Optional[Dict[str, str]] = None) -> None:
        self.risks = {**self.DEFAULT_RISKS, **(custom_risks or {})}

    @staticmethod
    def _normalize(service: str) -> str:
        return service.strip().lower()

    def get_warning(self, service: str) -> Optional[str]:
        return self.risks.get(self._normalize(service))

    def describe(self) -> Dict[str, str]:
        return dict(self.risks)


class Connector:
    """Base connector that never leaves the local process."""

    def __init__(self, name: str, protocol: str, store: LocalStore) -> None:
        self.name = name
        self.protocol = protocol
        self.store = store

    def send(self, message: Message) -> None:
        self.store.record_outgoing(message, self.name)

    def receive(self, message: Message) -> None:
        self.store.record_incoming(message, self.name)


class MatrixConnector(Connector):
    def __init__(self, store: LocalStore) -> None:
        super().__init__(name="matrix", protocol="matrix", store=store)


class RCSConnector(Connector):
    def __init__(self, store: LocalStore) -> None:
        super().__init__(name="rcs", protocol="rcs", store=store)


class ThirdPartyConnector(Connector):
    def __init__(self, name: str, store: LocalStore) -> None:
        super().__init__(name=name, protocol=name, store=store)


class MessengerService:
    """Local-only messenger that bridges Matrix/RCS and other services with warnings."""

    def __init__(self, store: Optional[LocalStore] = None, registry: Optional[RiskRegistry] = None) -> None:
        self.store = store or LocalStore()
        self.registry = registry or RiskRegistry()
        self.connectors: Dict[str, Connector] = {}
        self.bridges: Dict[str, List[str]] = {}
        self.acknowledged: Set[str] = set()

    @staticmethod
    def _normalize(service: str) -> str:
        return service.strip().lower()

    def add_connector(self, connector: Connector) -> None:
        self.connectors[self._normalize(connector.name)] = connector
        self.store.record_audit(f"connector-added {connector.name}")

    def acknowledge_service(self, service: str) -> Optional[str]:
        norm = self._normalize(service)
        warning = self.registry.get_warning(norm)
        self.acknowledged.add(norm)
        if warning:
            self.store.record_audit(f"warning-acknowledged {norm}: {warning}")
        return warning

    def _ensure_acknowledged(self, service: str) -> None:
        norm = self._normalize(service)
        warning = self.registry.get_warning(norm)
        if warning and norm not in self.acknowledged:
            raise RiskAcknowledgmentRequired(norm, warning)

    def connect_services(self, origin: str, targets: Iterable[str]) -> None:
        norm_origin = self._normalize(origin)
        self.bridges.setdefault(norm_origin, [])
        for target in targets:
            norm_target = self._normalize(target)
            if norm_target not in self.bridges[norm_origin]:
                self.bridges[norm_origin].append(norm_target)
        self.store.record_audit(f"bridge-updated {norm_origin}->{self.bridges[norm_origin]}")

    def _get_connector(self, service: str) -> Connector:
        norm = self._normalize(service)
        if norm not in self.connectors:
            raise KeyError(f"No connector registered for {service}")
        return self.connectors[norm]

    def send_message(
        self,
        service: str,
        sender: str,
        recipient: str,
        content: str,
        *,
        timestamp: Optional[datetime] = None,
    ) -> None:
        norm = self._normalize(service)
        self._ensure_acknowledged(norm)
        connector = self._get_connector(norm)
        message = Message(
            sender=sender,
            recipient=recipient,
            content=content,
            protocol=connector.protocol,
            timestamp=timestamp or datetime.now(timezone.utc),
        )
        connector.send(message)
        for target in self.bridges.get(norm, []):
            if target not in self.connectors:
                continue
            self._ensure_acknowledged(target)
            bridged = self._get_connector(target)
            bridged.send(message.copy_for_protocol(bridged.protocol))

    def receive_message(self, service: str, message: Message) -> None:
        norm = self._normalize(service)
        self._ensure_acknowledged(norm)
        connector = self._get_connector(norm)
        connector.receive(message)
        for target in self.bridges.get(norm, []):
            if target not in self.connectors:
                continue
            self._ensure_acknowledged(target)
            bridged = self._get_connector(target)
            bridged.receive(message.copy_for_protocol(bridged.protocol))

    def pending_warnings(self) -> Dict[str, str]:
        return {svc: warning for svc, warning in self.registry.describe().items() if svc not in self.acknowledged}

    def history(self) -> Dict[str, List[Dict]]:
        return self.store.history()


def demo() -> None:
    store = LocalStore()
    registry = RiskRegistry()
    messenger = MessengerService(store=store, registry=registry)
    messenger.add_connector(MatrixConnector(store))
    messenger.add_connector(RCSConnector(store))
    messenger.add_connector(ThirdPartyConnector("whatsapp", store))

    messenger.connect_services("matrix", ["rcs", "whatsapp"])

    print("Known service risks:")
    for service, warning in registry.describe().items():
        print(f"- {service}: {warning}")

    try:
        messenger.send_message("whatsapp", sender="alice", recipient="bob", content="Hi Bob from Matrix bridge")
    except RiskAcknowledgmentRequired as exc:
        print(f"⚠️  Warning for {exc.service}: {exc.warning}")
        messenger.acknowledge_service(exc.service)
        messenger.send_message("whatsapp", sender="alice", recipient="bob", content="Hi Bob from Matrix bridge (acknowledged)")

    messenger.acknowledge_service("matrix")
    messenger.acknowledge_service("rcs")
    messenger.send_message("matrix", sender="alice", recipient="carol", content="Routing via Matrix with bridge")

    print("\nLocal-only audit and history:")
    for entry in messenger.history()["audit"]:
        print(entry)
    print(messenger.history())


if __name__ == "__main__":
    demo()
