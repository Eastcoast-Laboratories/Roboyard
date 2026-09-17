package roboyard.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import driftingdroids.model.Board
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.ic_check_green
import roboyard.composeapp.generated.resources.ic_lock
import roboyard.composeapp.generated.resources.ic_play_arrow
import roboyard.composeapp.generated.resources.ic_user_profile
import roboyard.composeapp.generated.resources.star
import roboyard.logic.core.Constants
import roboyard.logic.core.GameHistoryEntry
import roboyard.logic.core.LevelLoader
import roboyard.logic.managers.GameHistoryManager
import roboyard.logic.managers.GameSession
import roboyard.logic.managers.LevelCompletionManager
import roboyard.logic.storage.PlatformStorage
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.getStringProvider
import roboyard.ui.graphics.MinimapGenerator
import kotlin.math.roundToInt

private const val ZOOM_DURATION_MS = 199

/** Grid entry: either a section header or a level card (matches LevelAdapter). */
private sealed interface LevelGridEntry {
    data class Header(val text: String) : LevelGridEntry
    data class Level(val id: Int) : LevelGridEntry
}

/** Snapshot of a clicked card for the zoom-to-board animation overlay. */
private data class ZoomCard(
    val levelId: Int,
    val startBounds: Rect,
    val isCompleted: Boolean,
    val starsEarned: Int,
    val board: Board?
)

/**
 * Level selection screen — Compose port of Android LevelSelectionFragment.
 * 3/6-column card grid (gold=completed, blue=playable, gray=locked), progress
 * bar with stars, auto-scroll to the last played level, scroll-up arrow,
 * "Custom Levels" section header, zoom animation on selection and a level
 * editor button once 140 stars are earned.
 */
@Composable
fun LevelSelectionScreen(
    session: GameSession? = null,
    onBack: () -> Unit = {},
    onLevelSelected: (Int) -> Unit = {},
    onProfile: () -> Unit = {},
    profileInitial: String? = null,
    onLevelEditor: () -> Unit = {}
) {
    val storage = getPlatformStorage()
    val stringProvider = getStringProvider()
    fun s(key: String, fallback: String, vararg args: Any): String =
        stringProvider.getString(key, *args) ?: formatArgs(fallback, *args)

    // Stop map regeneration while in level selection (Android onViewCreated parity)
    LaunchedEffect(Unit) { session?.stopRegeneration() }

    val completionManager = remember { LevelCompletionManager.getInstance(storage) }
    val availableLevels = remember { LevelLoader.listAvailableLevelIds(storage) }

    // Android calculateTotalStars: sum stars over *available* levels only
    val totalStars = remember(availableLevels) {
        availableLevels.sumOf { completionManager.getLevelCompletionData(it)?.getStars() ?: 0 }
    }
    val completedLevelCount = remember(availableLevels) {
        GameHistoryManager.getUniqueCompletedLevelCount(storage)
    }
    val historyByMapName = remember(availableLevels) {
        GameHistoryManager.getHistoryByLevelKey(storage)
    }
    val lastPlayedLevel = remember { completionManager.lastPlayedLevel }
    val showLevelEditor = totalStars >= 140

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var infoEntry by remember { mutableStateOf<GameHistoryEntry?>(null) }
    var zoomCard by remember { mutableStateOf<ZoomCard?>(null) }
    val zoomProgress = remember { Animatable(0f) }

    LaunchedEffect(zoomCard) {
        val card = zoomCard ?: return@LaunchedEffect
        zoomProgress.snapTo(0f)
        zoomProgress.animateTo(1f, tween(ZOOM_DURATION_MS))
        zoomCard = null
        onLevelSelected(card.levelId)
    }

    // Auto-scroll to the last played level, centered (Android scrollToLastPlayedLevel)
    LaunchedEffect(availableLevels) {
        val position = availableLevels.indexOf(lastPlayedLevel)
        if (position >= 0) {
            withFrameNanos { }
            val viewportHeight = gridState.layoutInfo.viewportSize.height
            val itemHeight = gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0
            val offset = if (viewportHeight > 0 && itemHeight > 0)
                -((viewportHeight - itemHeight) / 2).coerceAtLeast(0) else 0
            gridState.scrollToItem(position, offset)
        }
    }

    // Mixed grid entries: hidden "Standard Levels" header is skipped (Android
    // sets it GONE), "Custom Levels" header precedes the first custom level.
    val gridEntries = remember(availableLevels) {
        buildList {
            var customHeaderAdded = false
            for (id in availableLevels) {
                if (!customHeaderAdded && id >= Constants.CUSTOM_LEVEL_START_ID) {
                    add(LevelGridEntry.Header("custom_levels"))
                    customHeaderAdded = true
                }
                add(LevelGridEntry.Level(id))
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val spanCount = if (isLandscape) 6 else 3

        // Background matching bg_level_screen drawable (green gradient)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF7CB342), Color(0xFFCCDB44), Color(0xFF848436))
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header: title + profile button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp, start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s("level_selection_title", "Level Selection"),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(Color(0x80000000), Offset(1f, 1f), 3f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconCircularButton(
                    text = profileInitial,
                    icon = if (profileInitial == null) Res.drawable.ic_user_profile else null,
                    color = CircularButtonColor.TURQUOISE,
                    contentDescription = profileInitial
                        ?: s("profile_a11y", "User profile"),
                    onClick = onProfile,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Progress: total stars + star icon + completion bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalStars",
                    color = Color(0xFFFFD700),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        shadow = Shadow(Color(0x80000000), Offset(1f, 1f), 2f)
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Image(
                    painter = painterResource(Res.drawable.star),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LevelProgressBar(
                    completed = completedLevelCount,
                    total = availableLevels.size,
                    completedLabel = s("level_progress_completed", "Level completed"),
                    modifier = Modifier.weight(1f)
                )
            }

            // Scroll-up arrow (Android scroll_up_arrow): scrolls up one row
            val showScrollUp = gridState.firstVisibleItemIndex > 0
            if (showScrollUp) {
                FancyButton(
                    text = "▲",
                    color = FancyButtonColor.BLUE,
                    onClick = {
                        scope.launch {
                            val target =
                                (gridState.firstVisibleItemIndex - spanCount).coerceAtLeast(0)
                            gridState.animateScrollToItem(target)
                        }
                    },
                    modifier = Modifier
                        .width(220.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 2.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(spanCount),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(
                    items = gridEntries,
                    key = {
                        when (it) {
                            is LevelGridEntry.Header -> "header_${it.text}"
                            is LevelGridEntry.Level -> it.id
                        }
                    },
                    span = {
                        when (it) {
                            is LevelGridEntry.Header -> GridItemSpan(spanCount)
                            is LevelGridEntry.Level -> GridItemSpan(1)
                        }
                    }
                ) { entry ->
                    when (entry) {
                        is LevelGridEntry.Header -> Text(
                            text = s(entry.text, "Custom Levels"),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                        is LevelGridEntry.Level -> {
                            val levelId = entry.id
                            val isCompleted = completionManager.isLevelCompleted(levelId)
                            val starsEarned = if (isCompleted)
                                completionManager.getLevelCompletionData(levelId)?.getStars() ?: 0
                            else 0
                            val isUnlocked = levelId >= Constants.CUSTOM_LEVEL_START_ID ||
                                    Constants.STARS_PER_LEVEL * (levelId - 1) <= totalStars
                            val historyEntry = historyByMapName[
                                if (levelId < Constants.CUSTOM_LEVEL_START_ID)
                                    "level_$levelId" else "custom_level_$levelId"
                            ]
                            LevelCard(
                                levelId = levelId,
                                isCompleted = isCompleted,
                                starsEarned = starsEarned,
                                isUnlocked = isUnlocked,
                                isLastPlayed = levelId == lastPlayedLevel,
                                historyEntry = historyEntry,
                                storage = storage,
                                cardA11y = s("level_card_a11y", "Level {0}", levelId),
                                onInfoClick = { infoEntry = it },
                                onClick = { bounds, board ->
                                    // Locked cards are not clickable (Android parity:
                                    // levelCard.setClickable(isUnlocked))
                                    if (isUnlocked && zoomCard == null) {
                                        zoomCard = ZoomCard(
                                            levelId, bounds, isCompleted, starsEarned, board
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom row: Back + Level Editor (>=140 stars, Android parity)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FancyButton(
                    text = s("back", "Back"),
                    color = FancyButtonColor.GRAY,
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
                if (showLevelEditor) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FancyButton(
                        text = s("level_editor_button_short", "Editor"),
                        color = FancyButtonColor.BLUE,
                        onClick = onLevelEditor
                    )
                }
            }
        }

        // Zoom overlay: the card zooms toward the game board position, then
        // navigation happens (Android animateLevelZoom)
        zoomCard?.let { card ->
            val p = zoomProgress.value
            val start = card.startBounds
            // Target: match game board position — portrait: full-width square at
            // top; landscape: left/right half depending on card position
            val rootW = maxWidth.value * LocalDensity.current.density
            val rootH = maxHeight.value * LocalDensity.current.density
            val target = if (isLandscape) {
                val halfW = rootW / 2f
                if (start.center.x < halfW)
                    Rect(0f, 0f, halfW, rootH)
                else
                    Rect(halfW, 0f, rootW, rootH)
            } else {
                Rect(0f, 0f, rootW, rootW)
            }
            val left = start.left + (target.left - start.left) * p
            val top = start.top + (target.top - start.top) * p
            val w = start.width + (target.width - start.width) * p
            val h = start.height + (target.height - start.height) * p
            val density = LocalDensity.current
            Box(
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(
                        with(density) { w.toDp() },
                        with(density) { h.toDp() }
                    )
            ) {
                LevelCardVisual(
                    levelId = card.levelId,
                    isCompleted = card.isCompleted,
                    starsEarned = card.starsEarned,
                    isUnlocked = true,
                    isLastPlayed = false,
                    board = card.board,
                    showInfo = false,
                    onInfoClick = {}
                )
            }
        }
    }

    // Info popup (shared with SaveLoadScreen, matches Android showMapInfoPopup)
    infoEntry?.let { entry ->
        HistoryInfoDialog(entry = entry, onDismiss = { infoEntry = null })
    }
}

/**
 * Progress bar matching Android's fragment_level_selection progress container:
 * blue background, yellow/orange fill, diagonal separator at the fill edge and
 * text positioned left/right depending on completion percentage.
 */
@Composable
private fun LevelProgressBar(
    completed: Int,
    total: Int,
    completedLabel: String,
    modifier: Modifier = Modifier
) {
    val fraction = if (total > 0) completed.toFloat() / total else 0f
    val numbersText = "$completed / $total"
    // Android updateProgressUI: 0-30% all right, 30-70% split, 70-100% all left
    val leftText = when {
        fraction < 0.3f -> ""
        fraction < 0.7f -> completedLabel
        else -> "$numbersText $completedLabel"
    }
    val rightText = when {
        fraction < 0.3f -> "$numbersText $completedLabel"
        fraction < 0.7f -> numbersText
        else -> ""
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E88E5))
    ) {
        // Fill (bg_progress_fill gradient)
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFFA726), Color(0xFFFFB74D))
                        )
                    )
            )
        }
        // Diagonal separator at the fill edge (progress_diagonal_separator)
        if (fraction > 0f && fraction < 1f) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                val diagonalWidth = 20.dp.toPx()
                val x = (size.width * fraction - diagonalWidth / 2f)
                    .coerceIn(0f, size.width - diagonalWidth)
                val leftPath = Path().apply {
                    moveTo(x, 0f)
                    lineTo(x, size.height)
                    lineTo(x + diagonalWidth, size.height)
                    close()
                }
                val rightPath = Path().apply {
                    moveTo(x, 0f)
                    lineTo(x + diagonalWidth, size.height)
                    lineTo(x + diagonalWidth, 0f)
                    close()
                }
                drawPath(leftPath, Color(0xFFFFB74D))
                drawPath(rightPath, Color(0xFF1E88E5))
            }
        }
        // Texts
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = leftText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = rightText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Level card wrapper: resolves the minimap board (history file first, then
 * bundled asset) and reports its bounds for the zoom animation.
 */
@Composable
private fun LevelCard(
    levelId: Int,
    isCompleted: Boolean,
    starsEarned: Int,
    isUnlocked: Boolean,
    isLastPlayed: Boolean,
    historyEntry: GameHistoryEntry?,
    storage: PlatformStorage,
    cardA11y: String,
    onInfoClick: (GameHistoryEntry) -> Unit,
    onClick: (Rect, Board?) -> Unit
) {
    // Minimap resolution matches Android LevelViewHolder.bind:
    // completed -> history file first, then asset fallback (built-ins only)
    var board by remember(levelId) { mutableStateOf<Board?>(null) }
    LaunchedEffect(levelId, isCompleted) {
        if (isCompleted && historyEntry != null) {
            val saveData = try {
                storage.readFile(historyEntry.getMapPath())
            } catch (e: Exception) {
                ""
            }
            board = boardFromSaveData(saveData)
        }
        if (board == null && levelId < Constants.CUSTOM_LEVEL_START_ID) {
            board = try {
                LevelLoader.loadLevel(levelId)
            } catch (e: Exception) {
                null
            }
        }
    }

    var bounds by remember { mutableStateOf(Rect.Zero) }
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .semantics {
                testTag = "levelItem_$levelId"
                contentDescription = cardA11y
            }
            .clickable(enabled = isUnlocked) { onClick(bounds, board) }
    ) {
        LevelCardVisual(
            levelId = levelId,
            isCompleted = isCompleted,
            starsEarned = starsEarned,
            isUnlocked = isUnlocked,
            isLastPlayed = isLastPlayed,
            board = board,
            showInfo = isCompleted && historyEntry != null,
            onInfoClick = { historyEntry?.let(onInfoClick) }
        )
    }
}

/**
 * Visual content of a level card in its three states (matches Android
 * LevelViewHolder.bind): gold gradient + minimap/stars/info when completed,
 * blue gradient + big number + play arrow when unlocked, gray + lock when
 * locked. Green border highlights the last played level.
 */
@Composable
private fun LevelCardVisual(
    levelId: Int,
    isCompleted: Boolean,
    starsEarned: Int,
    isUnlocked: Boolean,
    isLastPlayed: Boolean,
    board: Board?,
    showInfo: Boolean,
    onInfoClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val brush = when {
        isCompleted -> Brush.linearGradient(
            listOf(Color(0xFFC8A415), Color(0xFFE8C840), Color(0xFFA07A10))
        )
        isUnlocked -> Brush.linearGradient(
            listOf(Color(0xFF1565C0), Color(0xFF1E88E5), Color(0xFF0D47A1))
        )
        else -> Brush.linearGradient(
            listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFF757575))
        )
    }
    val borderColor = when {
        isCompleted -> Color(0xFFFFD700)
        isUnlocked -> Color(0xFF42A5F5)
        else -> Color(0xFFA0A0A0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(brush)
            .border(2.dp, borderColor, shape)
            .alpha(if (!isUnlocked && !isCompleted) 0.8f else 1f)
            .then(
                // bg_level_card_last_played: green border overlay
                if (isLastPlayed) Modifier.border(4.dp, Color(0xFF72FF56), shape)
                else Modifier
            )
    ) {
        when {
            isCompleted -> {
                // Gold card: minimap with level number overlay (or number fallback)
                if (board != null) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                        MinimapGenerator.drawMinimap(this, board, size.width, size.height)
                    }
                    Text(
                        text = levelId.toString(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(Color.Black, Offset(1f, 1f), 2f)
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color(0x80000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp)
                    )
                } else {
                    Text(
                        text = levelId.toString(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                // Stars row (or green check when 0 stars)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                ) {
                    if (starsEarned == 0) {
                        Image(
                            painter = painterResource(Res.drawable.ic_check_green),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        repeat(starsEarned.coerceAtMost(4)) {
                            Image(
                                painter = painterResource(Res.drawable.star),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                // Info button (bottom-end, only with a history entry)
                if (showInfo) {
                    Text(
                        text = "ⓘ",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clickable(onClick = onInfoClick)
                            .padding(4.dp)
                    )
                }
            }
            isUnlocked -> {
                // Blue card: large number + play arrow
                Text(
                    text = levelId.toString(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Image(
                    painter = painterResource(Res.drawable.ic_play_arrow),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(20.dp)
                )
            }
            else -> {
                // Gray card: lock icon + "Level X" label
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_lock),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Level $levelId",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
