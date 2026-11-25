package com.meccael.tonal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.meccael.tonal.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Lifecycle

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class VisualBaselineTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        org.robolectric.shadows.ShadowLog.stream = System.`out`
    }

    @Test
    fun captureHomeCarouselStates() {
        val activity = org.robolectric.Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        val composeView = androidx.compose.ui.platform.ComposeView(activity).apply {
            setContent {
                androidx.compose.material3.MaterialTheme {
                    com.meccael.tonal.compose.HomeCarouselScreen(
                        uiState = com.meccael.tonal.compose.HomeUiState(
                            categoriesSize = 5,
                            currentCardPosition = 0,
                            recentMusicTitle = "Sample Track",
                            recentSectionTitle = "Recently added (3)",
                            isPlaying = false,
                            categoryTitles = listOf("All Music", "Playlists", "Favorites", "Genres", "Albums")
                        ),
                        onCategorySelected = {},
                        onCategoryClicked = {},
                        onOpenPlayer = {},
                        onPlayPause = {}
                    )
                }
            }
        }
        activity.setContentView(composeView)

        val widthPx = 1080
        val heightPx = 2400
        composeView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        ShadowLooper.idleMainLooper()

        // 1. Initial State
        val bitmap1 = Bitmap.createBitmap(composeView.measuredWidth, composeView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas1 = Canvas(bitmap1)
        canvas1.drawColor(Color.parseColor("#121212"))
        composeView.draw(canvas1)
        saveScreenshot(bitmap1, "testHomeCarousel_01_initial")
    }

    private fun saveScreenshot(bitmap: Bitmap, fileName: String) {
        val localDir = File("build/outputs/screenshots/${this::class.java.simpleName}")
        if (!localDir.exists()) {
            localDir.mkdirs()
        }
        val file = File(localDir, "$fileName.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            println("SCREENSHOT_DIR: ${localDir.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
