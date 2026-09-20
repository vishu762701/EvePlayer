package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.mediastore.MediaItemModel
import com.example.player.MediaPlaybackManager
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.player.AudioPlayerScreen
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.EveTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private lateinit var playbackManager: MediaPlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        playbackManager = MediaPlaybackManager.getInstance(this)

        handleIncomingIntent(intent)

        setContent {
            val userSettings by libraryViewModel.userSettings.collectAsState()

            EveTheme(themeMode = userSettings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "library"
                    ) {
                        composable("library") {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                playbackManager = playbackManager,
                                onNavigateToVideoPlayer = {
                                    navController.navigate("video_player")
                                },
                                onNavigateToAudioPlayer = {
                                    navController.navigate("audio_player")
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("video_player") {
                            VideoPlayerScreen(
                                playbackManager = playbackManager,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("audio_player") {
                            AudioPlayerScreen(
                                playbackManager = playbackManager,
                                onToggleFavorite = { libraryViewModel.toggleFavorite(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                lifecycleScope.launch {
                    val resolved = libraryViewModel.resolveMedia(uri)
                    if (resolved != null) {
                        playbackManager.playMedia(resolved, startPositionMs = 0L)
                    }
                }
            }
        }
    }
}
