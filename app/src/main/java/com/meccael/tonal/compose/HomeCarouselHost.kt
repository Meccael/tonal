package com.meccael.tonal.compose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.meccael.tonal.application.CallbacksInterface
import com.meccael.tonal.application.Fragment_Home_Carousel
import kotlinx.coroutines.delay

fun setupHomeCarouselCompose(composeView: ComposeView, fragment: Fragment_Home_Carousel) {
    composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

    composeView.setContent {
        val context = composeView.context
        // The music service may not be connected yet when this screen is rebuilt (after a rotation, say),
        // and its library can be reloaded later. Watch both so the screen fills in by itself.
        var serviceState by remember { mutableStateOf((fragment.activity as? CallbacksInterface)?.serviceMusic) }
        var dataVersion by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            var lastSignature = 0
            while (true) {
                val current = (fragment.activity as? CallbacksInterface)?.serviceMusic
                if (current !== serviceState) serviceState = current
                val recentList = try { current?.recent?.playlistMusics } catch (e: Exception) { null }
                val signature = if (recentList == null) 0 else System.identityHashCode(recentList) * 31 + recentList.size
                if (signature != lastSignature) {
                    lastSignature = signature
                    dataVersion++
                }
                delay(400)
            }
        }
        val serviceMusic = serviceState

        val categoriesCount = serviceMusic?.categoriesSize ?: 5

        val catTitles = remember(serviceMusic, dataVersion) {
            if (serviceMusic != null) {
                (0 until categoriesCount).map { idx ->
                    serviceMusic.getCategory(context, idx)?.title ?: "Category ${idx + 1}"
                }
            } else listOf("All Music", "Playlists", "Favorites", "Genres", "Albums")
        }

        // All Music card: the cover of the track that is (or was last) playing; the music note until one has played.
        var nowPlayingCover by remember { mutableStateOf(0L) }
        LaunchedEffect(serviceMusic) {
            while (true) {
                val c = if (serviceMusic != null && serviceMusic.trackStarted) {
                    serviceMusic.getCategory(context, 0)?.cover ?: 0L
                } else 0L
                if (c != nowPlayingCover) nowPlayingCover = c
                delay(1000)
            }
        }

        val catCovers = remember(serviceMusic, nowPlayingCover, dataVersion) {
            if (serviceMusic != null) {
                (0 until categoriesCount).map { idx ->
                    // Card 0 follows the playing track; cards 1..5 always show Tonal's own cover art.
                    when (idx) {
                        0 -> nowPlayingCover
                        in 1..5 -> 0L
                        else -> serviceMusic.getCategory(context, idx)?.cover ?: 0L
                    }
                }
            } else emptyList()
        }

        val catDefaults = remember(serviceMusic, dataVersion) {
            if (serviceMusic != null) {
                (0 until categoriesCount).map { idx ->
                    when (idx) {
                        0 -> com.meccael.tonal.R.drawable.cover_all
                        1 -> com.meccael.tonal.R.drawable.cover_albums
                        2 -> com.meccael.tonal.R.drawable.cover_folders
                        3 -> com.meccael.tonal.R.drawable.cover_artists
                        4 -> com.meccael.tonal.R.drawable.cover_genres
                        5 -> com.meccael.tonal.R.drawable.cover_playlists
                        else -> serviceMusic.getCategory(context, idx)?.defaultCover ?: com.meccael.tonal.R.drawable.cover0
                    }
                }
            } else emptyList()
        }

        val recentTracks = remember(serviceMusic, dataVersion) {
            serviceMusic?.recent?.playlistMusics ?: emptyList()
        }

        val recentCovers = remember(recentTracks) {
            recentTracks.map { it.cover }
        }

        var isPlaying by remember { mutableStateOf(serviceMusic?.isPlaying == true) }

        val firstRecentTitle = recentTracks.firstOrNull()?.title ?: "Recent Track"

        androidx.compose.material3.MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFF4ECDC), onPrimary = Color(0xFF222222), surface = Color(0xFF1C1C1C), onSurface = Color(0xFFF4ECDC), surfaceVariant = Color(0xFF2B2B2B), onSurfaceVariant = Color(0xFFB9AE98), outline = Color(0xFF888888))) {
            HomeCarouselScreen(
                uiState = HomeUiState(
                    categoriesSize = categoriesCount,
                    currentCardPosition = fragment.lastcardPosition,
                    recentMusicTitle = firstRecentTitle,
                    recentSectionTitle = "Recently added (${recentTracks.size})",
                    isPlaying = isPlaying,
                    categoryCovers = catCovers,
                    categoryDefaultCovers = catDefaults,
                    categoryTitles = catTitles,
                    recentCovers = recentCovers,
                    recentTitles = recentTracks.map { it.title ?: "" }
                ),
                onCategorySelected = { pos ->
                    fragment.lastcardPosition = pos
                },
                onCategoryClicked = { page ->
                    if (serviceMusic != null) {
                        val title = catTitles.getOrNull(page) ?: ""
                        if (page == 0) {
                            fragment.openMusics(page, title, null, null)
                        } else {
                            fragment.openPlaylists(page, title, null, null, null)
                        }
                    }
                },
                onOpenPlayer = {
                    fragment.openPlayer(null)
                },
                onRecentClicked = { index ->
                    // Cue the tapped song from the Recents playlist, play it, then show the player.
                    if (serviceMusic != null && index >= 0 && index < recentTracks.size) {
                        val recent = serviceMusic.recentPlaylist
                        serviceMusic.musicPlayed.setPosition(index)
                        serviceMusic.playlistPlayed = com.meccael.tonal.manager.CurrentPlaylist()
                        serviceMusic.playlistPlayed.newPlaylist(
                            recent.title, 0L, recent.cover, recent.size, recent.duration,
                            serviceMusic.recent.playlistMusics
                        )
                        serviceMusic.play(context, true)
                        isPlaying = true
                    }
                    fragment.openPlayer(null)
                },
                onPlayPause = {
                    if (serviceMusic != null) {
                        if (serviceMusic.isPlaying) {
                            serviceMusic.pause()
                            isPlaying = false
                        } else {
                            serviceMusic.play(context, false)
                            isPlaying = true
                        }
                    }
                }
            )
        }
    }
}
