package com.securercs.messenger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.securercs.messenger.ui.navigation.AppNavigation
import com.securercs.messenger.ui.theme.SecureRCSMessengerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecureRCSMessengerTheme {
                AppNavigation()
            }
        }
    }
}
