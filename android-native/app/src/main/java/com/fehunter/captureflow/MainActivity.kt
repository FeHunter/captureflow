package com.fehunter.captureflow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fehunter.captureflow.ui.CaptureFlowApp
import com.fehunter.captureflow.ui.CaptureViewModel

class MainActivity : ComponentActivity() {
    private val captureViewModel: CaptureViewModel by viewModels()
    private var pendingShareIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingShareIntent = intent
        setContent {
            val preferences = remember { getSharedPreferences("captureflow_preferences", MODE_PRIVATE) }
            var themeMode by remember {
                mutableStateOf(preferences.getString("theme_mode", "system") ?: "system")
            }
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val navy = Color(0xFF07182B)
            val deepBlue = Color(0xFF0C2B46)
            val ocean = Color(0xFF164B62)
            val blue = Color(0xFF1F5F8B)
            val cyan = Color(0xFF39A6D6)
            val lightSurface = Color(0xFFF4F8FC)
            val scheme = if (isDark) darkColorScheme(
                primary = Color(0xFF63C4F0), onPrimary = Color(0xFF002033),
                secondary = Color(0xFF58B9D6), onSecondary = Color(0xFF06212C),
                tertiary = Color(0xFF8ABFE8), background = navy,
                onBackground = Color(0xFFF3F7FB), surface = deepBlue,
                onSurface = Color(0xFFF3F7FB), surfaceVariant = ocean,
                onSurfaceVariant = Color(0xFFB7CEDF), outline = Color(0xFF52758E),
            ) else lightColorScheme(
                primary = blue, onPrimary = Color.White,
                secondary = ocean, onSecondary = Color.White,
                tertiary = cyan, background = lightSurface, onBackground = navy,
                surface = Color.White, onSurface = navy,
                surfaceVariant = Color(0xFFE1EDF6),
                onSurfaceVariant = Color(0xFF3D5D73), outline = Color(0xFF7D9AAF),
            )
            SideEffect { preferences.edit().putString("theme_mode", themeMode).apply() }
            MaterialTheme(colorScheme = scheme) {
                val shared = pendingShareIntent
                LaunchedEffect(shared) {
                    if (shared?.action == Intent.ACTION_SEND) {
                        captureViewModel.receiveSharedIntent(shared)
                        pendingShareIntent = null
                    }
                }
                CaptureFlowApp(
                    viewModel = captureViewModel,
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShareIntent = intent
    }
}
