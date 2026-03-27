package com.securercs.messenger.data.model

data class ServiceAccount(
    val service: String,
    val displayName: String,
    val handle: String,
    val isConnected: Boolean,
)
