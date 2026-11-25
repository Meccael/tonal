package com.meccael.tonal.compose

import android.content.ContentUris
import android.content.res.Configuration
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.meccael.tonal.R

// Tonal's warm cream palette (see README, Design).
private val CreamText = Color(0xFFF4ECDC)
private val CreamAccent = Color(0xFFEADFC8)
private val CreamMuted = Color(0xFFB9AE98)

/** Raised pill button: charcoal face, cream rim, a faint inner ring and a soft shadow underneath; sinks when pressed. */
@Composable
private fun RaisedPill(text: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .height(33.dp)
            .drawBehind {
                val strip = 3.dp.toPx()
                val faceH = size.height - strip
                val radius = CornerRadius(faceH / 2f)
                if (!pressed) {
                    drawRoundRect(
                        color = Color(0x66000000),
                        topLeft = Offset(2.dp.toPx(), strip),
                        size = Size(size.width - 4.dp.toPx(), faceH),
                        cornerRadius = radius
                    )
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        if (pressed) listOf(Color(0xFF222222), Color(0xFF2E2E2E))
                        else listOf(Color(0xFF3D3D3D), Color(0xFF2B2B2B)),
                        endY = faceH
                    ),
                    size = Size(size.width, faceH),
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = CreamAccent.copy(alpha = if (pressed) 0.6f else 0.38f),
                    topLeft = Offset(0.5.dp.toPx(), 0.5.dp.toPx()),
                    size = Size(size.width - 1.dp.toPx(), faceH - 1.dp.toPx()),
                    cornerRadius = radius,
                    style = Stroke(width = 1.dp.toPx())
                )
                drawRoundRect(
                    color = CreamAccent.copy(alpha = if (pressed) 0.35f else 0.2f),
                    topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                    size = Size(size.width - 6.dp.toPx(), faceH - 6.dp.toPx()),
                    cornerRadius = CornerRadius((faceH - 6.dp.toPx()) / 2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp)
            .padding(bottom = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            color = CreamText
        )
    }
}

@Composable
fun HomeCarouselScreen(
    uiState: HomeUiState,
    onCategorySelected: (Int) -> Unit,
    onCategoryClicked: (Int) -> Unit,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onRecentClicked: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pageCount = uiState.categoriesSize.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = uiState.currentCardPosition.coerceIn(0, pageCount - 1),
        pageCount = { pageCount }
    )

    LaunchedEffect(pagerState.currentPage) {
        onCategorySelected(pagerState.currentPage)
    }

    // The Recently added title follows the cover nearest the left edge as the row scrolls.
    val recentsState = rememberLazyListState()
    val density = LocalDensity.current
    val shownRecentTitle by remember(uiState.recentTitles, uiState.recentMusicTitle) {
        derivedStateOf {
            val halfCover = with(density) { 41.dp.toPx() }
            var index = recentsState.firstVisibleItemIndex
            if (recentsState.firstVisibleItemScrollOffset > halfCover) index += 1
            uiState.recentTitles.getOrNull(index)
                ?: uiState.recentTitles.lastOrNull()
                ?: uiState.recentMusicTitle
        }
    }

    val shownRecentIndex by remember {
        derivedStateOf {
            val halfCover = with(density) { 41.dp.toPx() }
            var index = recentsState.firstVisibleItemIndex
            if (recentsState.firstVisibleItemScrollOffset > halfCover) index += 1
            index
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Blurred copy of the front card's cover fills the screen behind everything.
        Crossfade(
            targetState = pagerState.currentPage,
            animationSpec = tween(400),
            modifier = Modifier.fillMaxSize(),
            label = "homeBackdrop"
        ) { page ->
            val bgCoverId = uiState.categoryCovers.getOrNull(page)
            val bgDefaultRes = uiState.categoryDefaultCovers.getOrNull(page) ?: R.drawable.cover0
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                            setRenderEffect(
                                android.graphics.RenderEffect.createBlurEffect(
                                    50f, 50f, android.graphics.Shader.TileMode.CLAMP
                                )
                            )
                        }
                    }
                },
                update = { imageView ->
                    if (bgCoverId != null && bgCoverId > 0) {
                        Glide.with(imageView.context)
                            .load(com.meccael.tonal.artwork.ArtworkResolver.model(imageView.context, bgCoverId))
                            .signature(com.meccael.tonal.artwork.ArtworkResolver.signature(imageView.context, bgCoverId))
                            .error(bgDefaultRes)
                            .into(imageView)
                    } else {
                        Glide.with(imageView.context).clear(imageView)
                        imageView.setImageResource(bgDefaultRes)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Slight zoom so blurred edges never show.
                        scaleX = 1.2f
                        scaleY = 1.2f
                    }
            )
        }
        // Darken so text and controls stay readable.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x94121212))
        )

        // The three parts of the screen, arranged in a column (portrait) or side by side (landscape).
        val carouselArea: @Composable (Modifier) -> Unit = { areaMod ->
            Box(
                modifier = areaMod,
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // Keep the card's shape when the space is short (landscape): narrow it instead of squashing it.
                    val cardWidth = minOf(maxWidth * 0.74f, (maxHeight - 16.dp) / 1.25f)
                    val cardHeight = cardWidth * 1.25f
                    val stackPeek = 26.dp
                    val sideMargin = 24.dp
                    val scope = rememberCoroutineScope()

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = (maxWidth - cardWidth - sideMargin).coerceAtLeast(0.dp),
                            end = sideMargin
                        ),
                        pageSize = PageSize.Fixed(cardWidth),
                        pageSpacing = 0.dp,
                        beyondViewportPageCount = 3
                    ) { page ->
                        // d > 0: card waits in the stack behind the front card.
                        // d in (-1, 0): the front card is being swiped off to the left.
                        val d = page - pagerState.currentPage - pagerState.currentPageOffsetFraction
                        val depth = d.coerceIn(0f, 3f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex((pageCount - page).toFloat())
                                .graphicsLayer {
                                    if (d > 0f) {
                                        // Cancel the pager's own slide so cards stay stacked,
                                        // fanned out to the left by stackPeek per level.
                                        translationX = -d * (size.width + stackPeek.toPx())
                                        val s = 1f - 0.07f * depth
                                        scaleX = s
                                        scaleY = s
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        alpha = if (d > 3f) (4f - d).coerceIn(0f, 1f) else 1f
                                    } else {
                                        // Front card follows the finger, tilting slightly as it leaves.
                                        rotationZ = d * 6f
                                        alpha = (1f + d * 0.6f).coerceIn(0f, 1f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                                    .clickable {
                                        if (page == pagerState.currentPage) {
                                            onCategoryClicked(page)
                                        } else {
                                            scope.launch { pagerState.animateScrollToPage(page) }
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                                border = BorderStroke(1.dp, CreamAccent.copy(alpha = 0.14f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    val coverId = uiState.categoryCovers.getOrNull(page)
                                    val defaultCoverRes = uiState.categoryDefaultCovers.getOrNull(page) ?: R.drawable.cover0

                                    if (coverId != null && coverId > 0) {
                                        AndroidView(
                                            factory = { ctx ->
                                                ImageView(ctx).apply {
                                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                                }
                                            },
                                            update = { imageView ->
                                                Glide.with(imageView.context)
                                                    .load(com.meccael.tonal.artwork.ArtworkResolver.model(imageView.context, coverId))
                                                    .signature(com.meccael.tonal.artwork.ArtworkResolver.signature(imageView.context, coverId))
                                                    .error(defaultCoverRes)
                                                    .into(imageView)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(defaultCoverRes),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Light fade at the bottom edge; the name sits under the stack.
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.7f to Color.Transparent,
                                                    1f to Color(0x59000000)
                                                )
                                            )
                                    )

                                    // Cards deeper in the stack are dimmed.
                                    if (depth > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = (depth * 0.18f).coerceIn(0f, 0.6f)))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val labelBlock: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.categoryTitles.getOrNull(pagerState.currentPage) ?: "",
                    fontSize = 20.sp,
                    color = CreamText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0 until pageCount) {
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (i == pagerState.currentPage) 18.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (i == pagerState.currentPage) CreamAccent else CreamAccent.copy(alpha = 0.25f))
                        )
                    }
                }
            }
        }

        val recentsBlock: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(bottom = 20.dp)
            ) {
                // Header row: Recents label + title + Open Player button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                            .clickable { onOpenPlayer() },
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.recentSectionTitle.ifEmpty { stringResource(R.string.home_recently_added) }.uppercase(),
                            fontSize = 10.5.sp,
                            letterSpacing = 1.6.sp,
                            color = CreamMuted,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shownRecentTitle.ifEmpty { "Recent Track" },
                            fontSize = 15.sp,
                            color = CreamText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    RaisedPill(
                        text = stringResource(R.string.home_player).uppercase(),
                        onClick = onOpenPlayer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Recents: raised covers on a rail, the one on show gets the glowing dot.
                LazyRow(
                    state = recentsState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp)
                ) {
                    val recentsCount = uiState.recentCovers.size.coerceAtLeast(5)
                    items(recentsCount) { index ->
                        val coverId = uiState.recentCovers.getOrNull(index)
                        val shown = index == shownRecentIndex
                        Column(modifier = Modifier.width(90.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2C2C2C))
                                    .border(
                                        1.dp,
                                        if (shown) CreamAccent.copy(alpha = 0.7f) else CreamAccent.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onRecentClicked(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (coverId != null && coverId > 0) {
                                    AndroidView(
                                        factory = { ctx ->
                                            ImageView(ctx).apply {
                                                scaleType = ImageView.ScaleType.CENTER_CROP
                                            }
                                        },
                                        update = { imageView ->
                                            Glide.with(imageView.context)
                                                .load(com.meccael.tonal.artwork.ArtworkResolver.model(imageView.context, coverId))
                                                .signature(com.meccael.tonal.artwork.ArtworkResolver.signature(imageView.context, coverId))
                                                .error(R.drawable.empty)
                                                .into(imageView)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.empty),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(CreamAccent.copy(alpha = 0.14f))
                                )
                                val dot = if (shown) 14.dp else 9.dp
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = 38.dp - dot / 2)
                                        .size(dot)
                                        .clip(CircleShape)
                                        .background(if (shown) CreamAccent else Color(0xFF343434))
                                        .then(
                                            if (shown) Modifier else Modifier.border(1.5.dp, CreamAccent.copy(alpha = 0.5f), CircleShape)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth > maxHeight) {
                // Landscape: the stack takes the full height on the left; the Recently added block sits beside it.
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        carouselArea(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 52.dp)
                        )
                        labelBlock()
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .padding(top = 40.dp, end = 80.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        recentsBlock()
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    carouselArea(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    labelBlock()
                    recentsBlock()
                }
            }
        }

        // FAB Play/Pause button
        FloatingActionButton(
            onClick = onPlayPause,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .border(1.dp, CreamAccent.copy(alpha = 0.35f), CircleShape),
            containerColor = Color(0xFF353535),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(
                    if (uiState.isPlaying) R.drawable.ic_pause_white_24dp else R.drawable.ic_play_white_24dp
                ),
                contentDescription = stringResource(R.string.home_player),
                tint = CreamAccent
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HomeCarouselScreenPreview() {
    MaterialTheme {
        HomeCarouselScreen(
            uiState = HomeUiState(
                categoriesSize = 5,
                currentCardPosition = 0,
                recentMusicTitle = "Sample Track Title - Artist Name",
                recentSectionTitle = "Recently added (12)",
                categoryTitles = listOf("All Music", "Playlists", "Favorites", "Genres", "Albums")
            ),
            onCategorySelected = {},
            onCategoryClicked = {},
            onOpenPlayer = {},
            onPlayPause = {},
            onRecentClicked = {}
        )
    }
}
