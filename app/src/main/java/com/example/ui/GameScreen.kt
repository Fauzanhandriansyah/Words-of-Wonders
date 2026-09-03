package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.game.LevelData
import com.example.game.WordPlacement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val levelData = if (viewModel.isDailyPuzzleMode) viewModel.dailyPuzzleLevelData else viewModel.levelData
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        if (viewModel.isHomeScreen) {
            HomeScreen(
                viewModel = viewModel,
                gameState = gameState,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 1. Immersive full-screen background image matching level theme
            if (levelData != null) {
                Image(
                    painter = painterResource(id = levelData.themeBackgroundResId),
                    contentDescription = "Scenic Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // 2. Dark readability overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // 3. Main Gameplay Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // Header: Top stats bar
                TopStatsBar(
                    gems = gameState.gems,
                    dailyHints = gameState.dailyHintsCount,
                    levelNumber = gameState.currentLevel,
                    isDailyMode = viewModel.isDailyPuzzleMode,
                    themeName = levelData?.themeName ?: "",
                    onShopClicked = { viewModel.showIapShop = true },
                    onSettingsClicked = { viewModel.showSettings = true },
                    onBackClicked = { viewModel.isHomeScreen = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

            // Crossword puzzle grid
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (levelData != null) {
                    CrosswordGrid(
                        levelData = levelData,
                        solvedWords = viewModel.solvedWords,
                        customRevealedCells = viewModel.customRevealedCells,
                        onCellClicked = { x, y ->
                            // Open hammer reveal modal for this specific clicked cell
                            viewModel.triggerHammerHint(x, y)
                        }
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // Word spelling real-time preview display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.selectedLetters.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF1E293B).copy(alpha = 0.9f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color(0xFF64FFDA),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        viewModel.selectedLetters.forEach { char ->
                            Text(
                                text = char.toString(),
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            // Bottom controls & letter wheel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hammer Hint Button (Palu)
                    HintButtonWithBadge(
                        icon = Icons.Default.Build, // Representing hammer
                        label = "Palu",
                        badgeCount = gameState.dailyHintsCount,
                        badgeCost = 2,
                        costLabel = "100💎",
                        onClick = {
                            viewModel.showTemporaryMessage("Sentuh kotak huruf di grid untuk mengungkapnya dengan Palu! 🔨")
                        }
                    )

                    // Letter Wheel in the center
                    if (levelData != null) {
                        WordWheel(
                            letters = viewModel.activeWheelLetters,
                            selectedIndices = viewModel.selectedIndices,
                            onLetterSelected = { viewModel.selectLetter(it) },
                            onSelectionSubmitted = { viewModel.submitSelection() },
                            onShuffleClicked = {
                                viewModel.shuffleLetters()
                            },
                            dragLineStartPoint = viewModel.dragLineStartPoint,
                            dragLineCurrentPoint = viewModel.dragLineCurrentPoint,
                            onUpdateDragPoints = { start, current ->
                                viewModel.dragLineStartPoint = start
                                viewModel.dragLineCurrentPoint = current
                            },
                            modifier = Modifier.size(230.dp)
                        )
                    }

                    // Bulb Hint Button (Bohlam)
                    HintButtonWithBadge(
                        icon = Icons.Default.Lightbulb,
                        label = "Petunjuk",
                        badgeCount = gameState.dailyHintsCount,
                        badgeCost = 1,
                        costLabel = "50💎",
                        onClick = { viewModel.triggerRandomHint() }
                    )
                }
            }
            
            // Bottom banner / daily puzzle launcher
            if (!viewModel.isDailyPuzzleMode) {
                DailyPuzzleBanner(
                    lastClaimedDate = gameState.lastClaimedDailyPuzzleDate,
                    onLaunchClicked = { viewModel.launchDailyPuzzle() }
                )
            } else {
                Button(
                    onClick = { viewModel.exitDailyPuzzle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .height(42.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Exit", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Keluar Mode Harian", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        }

        // 4. Floating Feedback Toast Overlay
        AnimatedVisibility(
            visible = viewModel.gameMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .border(1.dp, Color(0xFF64FFDA).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = viewModel.gameMessage ?: "",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        // 5. Level Complete popup with progress to next 50-level Scenic Area!
        if (viewModel.showLevelComplete) {
            LevelCompleteDialog(
                levelNum = gameState.currentLevel, // Solved level number
                gemsAward = if (gameState.currentLevel <= 300) 25 else if (gameState.currentLevel <= 700) 50 else 100,
                onNextClicked = { viewModel.proceedToNextLevel() }
            )
        }

        // 6. Daily Complete popup
        if (viewModel.showDailyComplete) {
            DailyCompleteDialog(
                onCloseClicked = {
                    viewModel.showDailyComplete = false
                    viewModel.exitDailyPuzzle()
                }
            )
        }

        // 7. Simulated IAP Shop popup
        if (viewModel.showIapShop) {
            IapShopDialog(
                currentGems = gameState.gems,
                onCloseClicked = { viewModel.showIapShop = false },
                onBuyClicked = { viewModel.buyGems(it) }
            )
        }

        // 8. Settings Modal
        if (viewModel.showSettings) {
            SettingsDialog(
                onCloseClicked = { viewModel.showSettings = false },
                onBackToHomeClicked = {
                    viewModel.showSettings = false
                    viewModel.isHomeScreen = true
                }
            )
        }
    }
}

// ---------------------------------------------------------
// COMPONENT: TOP STATS BAR
// ---------------------------------------------------------
@Composable
fun TopStatsBar(
    gems: Int,
    dailyHints: Int,
    levelNumber: Int,
    isDailyMode: Boolean,
    themeName: String,
    onShopClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. LEFT CONTAINER (Back Button and Small Gems Indicator)
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Kembali ke Menu",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Small Gems Indicator (atas kiri kecil)
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                    .clickable { onShopClicked() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Gems",
                    tint = Color(0xFFFBBF24), // Gold accent
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = gems.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // 2. CENTER CONTAINER (Level Title and Destination - Absolutely Centered)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isDailyMode) "TANTANGAN HARIAN" else "LEVEL $levelNumber",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.shadow(4.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = themeName.uppercase(),
                color = Color(0xFF38BDF8), // Sky blue theme accent
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.shadow(2.dp)
            )
        }

        // 3. RIGHT CONTAINER (Settings Button)
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSettingsClicked,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------
// COMPONENT: CROSSWORD GRID
// ---------------------------------------------------------
@Composable
fun CrosswordGrid(
    levelData: LevelData,
    solvedWords: Set<String>,
    customRevealedCells: Set<String>,
    onCellClicked: (Int, Int) -> Unit
) {
    // Generate crossword metrics
    val placements = levelData.wordPlacements
    if (placements.isEmpty()) return

    // Find grid bounding box
    var maxX = 0
    var maxY = 0
    for (p in placements) {
        val wordLen = p.word.length
        val endX = if (p.isHorizontal) p.startX + wordLen - 1 else p.startX
        val endY = if (p.isHorizontal) p.startY else p.startY + wordLen - 1
        if (endX > maxX) maxX = endX
        if (endY > maxY) maxY = endY
    }

    val cols = maxX + 1
    val rows = maxY + 1

    // Map letters of words to positions
    val gridLetters = remember(levelData, placements) {
        val map = mutableMapOf<Pair<Int, Int>, Char>()
        for (p in placements) {
            for (idx in p.word.indices) {
                val cx = if (p.isHorizontal) p.startX + idx else p.startX
                val cy = if (p.isHorizontal) p.startY else p.startY + idx
                map[Pair(cx, cy)] = p.word[idx]
            }
        }
        map
    }

    // Measure screen sizes to fit cells dynamically
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxCellSize = 42.dp
    val spacing = 3.dp

    val calculatedCellSize = minOf(
        maxCellSize,
        (screenWidth - 32.dp - (spacing * (cols + 1))) / cols
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        for (y in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (x in 0 until cols) {
                    val char = gridLetters[Pair(x, y)]
                    if (char != null) {
                        // Determine if this cell's letter is solved
                        val solved = isCellSolved(x, y, placements, solvedWords)
                        val revealed = solved || customRevealedCells.contains("$x,$y")

                        Box(
                            modifier = Modifier
                                .size(calculatedCellSize)
                                .padding(spacing / 2)
                                .shadow(if (revealed) 2.dp else 4.dp, RoundedCornerShape(8.dp))
                                .background(
                                    color = if (revealed) Color(0xFF64FFDA) else Color.White.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (!revealed) {
                                        onCellClicked(x, y)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (revealed) {
                                Text(
                                    text = char.toString(),
                                    color = Color(0xFF0F172A),
                                    fontSize = (calculatedCellSize.value * 0.55f).sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    } else {
                        // Empty spacer cell
                        Box(
                            modifier = Modifier
                                .size(calculatedCellSize)
                                .padding(spacing / 2)
                        )
                    }
                }
            }
        }
    }
}

private fun isCellSolved(
    x: Int,
    y: Int,
    placements: List<WordPlacement>,
    solvedWords: Set<String>
): Boolean {
    for (p in placements) {
        for (idx in p.word.indices) {
            val cx = if (p.isHorizontal) p.startX + idx else p.startX
            val cy = if (p.isHorizontal) p.startY else p.startY + idx
            if (cx == x && cy == y && solvedWords.contains(p.word.uppercase())) {
                return true
            }
        }
    }
    return false
}

// ---------------------------------------------------------
// COMPONENT: WORD WHEEL (SWIPABLE DRAG CONTROLLER)
// ---------------------------------------------------------
@Composable
fun WordWheel(
    letters: List<Char>,
    selectedIndices: List<Int>,
    onLetterSelected: (Int) -> Unit,
    onSelectionSubmitted: () -> Unit,
    onShuffleClicked: () -> Unit,
    dragLineStartPoint: Pair<Float, Float>?,
    dragLineCurrentPoint: Pair<Float, Float>?,
    onUpdateDragPoints: (Pair<Float, Float>?, Pair<Float, Float>?) -> Unit,
    modifier: Modifier = Modifier
) {
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val letterCenters = remember(letters, sizePx) {
        if (sizePx == IntSize.Zero) return@remember emptyList<Offset>()
        val n = letters.size
        val center = Offset(sizePx.width / 2f, sizePx.height / 2f)
        val radius = min(sizePx.width, sizePx.height) * 0.38f
        
        List(n) { i ->
            val angleRad = Math.toRadians((i * (360f / n) - 90f).toDouble())
            val lx = center.x + radius * cos(angleRad).toFloat()
            val ly = center.y + radius * sin(angleRad).toFloat()
            Offset(lx, ly)
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { sizePx = it.size }
            .background(Color.White.copy(alpha = 0.15f), CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .pointerInput(letters, letterCenters) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onLetterSelectedAtOffset(offset, letterCenters, letters, onLetterSelected, onUpdateDragPoints)
                    },
                    onDragEnd = {
                        onSelectionSubmitted()
                    },
                    onDragCancel = {
                        onSelectionSubmitted()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onLetterSelectedAtOffset(change.position, letterCenters, letters, onLetterSelected, onUpdateDragPoints)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Draw the connecting lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (selectedIndices.isNotEmpty() && letterCenters.isNotEmpty()) {
                val selectedCoords = selectedIndices.mapNotNull { idx ->
                    if (idx != -1 && idx < letterCenters.size) letterCenters[idx] else null
                }

                // 1. Draw established lines between already connected nodes
                for (i in 0 until selectedCoords.size - 1) {
                    drawLine(
                        color = Color(0xFF64FFDA),
                        start = selectedCoords[i],
                        end = selectedCoords[i + 1],
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                }

                // 2. Draw trailing line from last connected node to dragging finger
                val currentFinger = dragLineCurrentPoint
                if (currentFinger != null && selectedCoords.isNotEmpty()) {
                    drawLine(
                        color = Color(0xFF64FFDA).copy(alpha = 0.6f),
                        start = selectedCoords.last(),
                        end = Offset(currentFinger.first, currentFinger.second),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Draw letter nodes
        if (sizePx != IntSize.Zero && letterCenters.size == letters.size) {
            letters.forEachIndexed { i, char ->
                val centerOffset = letterCenters[i]
                val dpX = with(density) { centerOffset.x.toDp() }
                val dpY = with(density) { centerOffset.y.toDp() }
                val isSelected = selectedIndices.contains(i)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = dpX - 25.dp, y = dpY - 25.dp) // center the box on coordinate
                        .size(50.dp)
                        .background(
                            color = if (isSelected) Color(0xFF64FFDA) else Color.White,
                            shape = CircleShape
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF0F172A) else Color.Gray.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .shadow(if (isSelected) 6.dp else 2.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.toString(),
                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFF1E293B),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Shuffle button inside the wheel center
        IconButton(
            onClick = onShuffleClicked,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
                .shadow(3.dp, CircleShape)
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = Color(0xFF0F172A),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun onLetterSelectedAtOffset(
    touchPos: Offset,
    centers: List<Offset>,
    letters: List<Char>,
    onSelected: (Int) -> Unit,
    onUpdatePoints: (Pair<Float, Float>?, Pair<Float, Float>?) -> Unit
) {
    val selectionRadiusPx = 100f // Swipe proximity threshold
    
    // Find closest node
    var closestIdx = -1
    var minDist = Float.MAX_VALUE
    
    centers.forEachIndexed { idx, center ->
        val dx = touchPos.x - center.x
        val dy = touchPos.y - center.y
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (dist < minDist) {
            minDist = dist
            closestIdx = idx
        }
    }

    if (closestIdx != -1 && minDist < selectionRadiusPx) {
        onSelected(closestIdx)
        onUpdatePoints(
            Pair(centers[closestIdx].x, centers[closestIdx].y),
            Pair(touchPos.x, touchPos.y)
        )
    } else {
        onUpdatePoints(null, Pair(touchPos.x, touchPos.y))
    }
}

// ---------------------------------------------------------
// COMPONENT: BADGED HINT BUTTONS
// ---------------------------------------------------------
@Composable
fun HintButtonWithBadge(
    icon: ImageVector,
    label: String,
    badgeCount: Int,
    badgeCost: Int,
    costLabel: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            // Rounded glass action button
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // High-contrast badge counter (Daily free count)
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 36.dp, y = (-4).dp)
                        .background(Color(0xFFE11D48), CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Show premium cost underneath if free count exhausted
        Text(
            text = if (badgeCount >= badgeCost) "GRATIS" else costLabel,
            color = if (badgeCount >= badgeCost) Color(0xFF64FFDA) else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------------------------------------------------------
// COMPONENT: DAILY BANNER
// ---------------------------------------------------------
@Composable
fun DailyPuzzleBanner(
    lastClaimedDate: String,
    onLaunchClicked: () -> Unit
) {
    val today = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }
    val completed = lastClaimedDate == today

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (completed) {
                        listOf(Color(0xFF334155), Color(0xFF1E293B))
                    } else {
                        listOf(Color(0xFF4338CA), Color(0xFF312E81))
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (completed) Color.White.copy(alpha = 0.1f) else Color(0xFF818CF8).copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !completed) { onLaunchClicked() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Daily Icon",
                tint = if (completed) Color.Gray else Color(0xFFFBBF24),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Tantangan Harian",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (completed) "Tantangan selesai! Klaim besok." else "Mainkan & Dapatkan +150 Gems!",
                    color = if (completed) Color.LightGray else Color(0xFFC7D2FE),
                    fontSize = 12.sp
                )
            }
        }
        
        Text(
            text = if (completed) "SELESAI" else "MAIN",
            color = if (completed) Color.Gray else Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            modifier = Modifier
                .background(
                    color = if (completed) Color.Black.copy(alpha = 0.2f) else Color(0xFF10B981),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// ---------------------------------------------------------
// DIALOGS: LEVEL COMPLETE POPUP
// ---------------------------------------------------------
@Composable
fun LevelCompleteDialog(
    levelNum: Int,
    gemsAward: Int,
    onNextClicked: () -> Unit
) {
    // Destinations cycle every 50 levels
    val solvedInTheme = ((levelNum - 1) % 50) + 1
    val currentThemeIndex = ((levelNum - 1) / 50) % 4
    
    val currentThemeName = when (currentThemeIndex) {
        0 -> "Mesir"
        1 -> "Jepang"
        2 -> "Paris"
        else -> "Venesia"
    }
    
    val nextThemeName = when ((currentThemeIndex + 1) % 4) {
        0 -> "Mesir"
        1 -> "Jepang"
        2 -> "Paris"
        else -> "Venesia"
    }

    // Animation transition for stars and falling confetti
    val infiniteTransition = rememberInfiniteTransition(label = "level_complete_anims")
    
    // Confetti position animation progress
    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    // Pulsing scales for three star badges
    val starScale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star1"
    )
    val starScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star2"
    )
    val starScale3 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {}, // absorb clicks
        contentAlignment = Alignment.Center
    ) {
        // Drifting / Falling Confetti Canvas Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rand = java.util.Random(99)
            for (i in 0 until 45) {
                val startX = rand.nextFloat() * size.width
                val fallSpeed = 200f + rand.nextFloat() * 250f
                val horizontalDrift = sin(confettiProgress * PI * 2 + i) * 40f
                val currentY = (fallSpeed * confettiProgress * 3.5f) % size.height
                
                val confettiColor = when (i % 6) {
                    0 -> Color(0xFFFBBF24) // Gold/Yellow
                    1 -> Color(0xFFEF4444) // Coral Red
                    2 -> Color(0xFF3B82F6) // Electric Blue
                    3 -> Color(0xFF10B981) // Emerald Green
                    4 -> Color(0xFFA855F7) // Purple
                    else -> Color(0xFFEC4899) // Hot Pink
                }
                
                drawCircle(
                    color = confettiColor.copy(alpha = 0.85f),
                    radius = 8f + rand.nextFloat() * 10f,
                    center = Offset((startX + horizontalDrift).toFloat(), currentY.toFloat())
                )
            }
        }

        // Inner Dialog Container Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp)
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFFBBF24))
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Glowing Stars Row at top
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer(scaleX = starScale1, scaleY = starScale1)
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(54.dp)
                            .graphicsLayer(scaleX = starScale2, scaleY = starScale2)
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .size(36.dp)
                            .graphicsLayer(scaleX = starScale3, scaleY = starScale3)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Heading text
                Text(
                    text = "LUAR BIASA!",
                    color = Color(0xFF10B981),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "LEVEL $levelNum SELESAI",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Reward representation with a beautiful badge
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Gems",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "+$gemsAward GEMS!",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                
                // Scenic Area Progress
                Text(
                    text = "PERJALANAN MENUJU $nextThemeName",
                    color = Color(0xFF3B82F6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Visual Progress Bar
                val progress = solvedInTheme / 50f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF3B82F6))
                                ),
                                shape = CircleShape
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = currentThemeName, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$solvedInTheme/50", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = nextThemeName, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                if (solvedInTheme == 50) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF78350F), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "🎉 DESTINASI BARU DIBUKA: $nextThemeName!",
                            color = Color(0xFFFBBF24),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Next Level Button with high visual contrast
                Button(
                    onClick = onNextClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = if (solvedInTheme == 50) "JELAJAHI DESTINASI 🚀" else "LANJUT LEVEL ➜",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------
// DIALOG: DAILY COMPLETE
// ---------------------------------------------------------
@Composable
fun DailyCompleteDialog(onCloseClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(2.dp, Color(0xFFFBBF24), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Selamat! 🎉",
                    color = Color(0xFFFBBF24),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tantangan Harian Selesai!",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Gems",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+150 Gems!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onCloseClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("KLAIM HADIAH", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------------------------------------------
// DIALOG: GEMS SHOP (IAP SHOP)
// ---------------------------------------------------------
@Composable
fun IapShopDialog(
    currentGems: Int,
    onCloseClicked: () -> Unit,
    onBuyClicked: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Toko Gems 💎", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = onCloseClicked) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Mata Uang Premium Anda: $currentGems Gems",
                    color = Color(0xFF64FFDA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pack 1: Basic Pack
                ShopItemRow(
                    title = "Kantung Gems Kecil",
                    desc = "Membantu Anda melewati kata yang sulit",
                    gemsCount = 100,
                    price = "Rp 5.000",
                    onBuy = { onBuyClicked(0) }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Pack 2: Medium Pack
                ShopItemRow(
                    title = "Kantung Gems Sedang",
                    desc = "Sangat direkomendasikan untuk Pemula",
                    gemsCount = 500,
                    price = "Rp 15.000",
                    onBuy = { onBuyClicked(1) }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Pack 3: Large Pack
                ShopItemRow(
                    title = "Peti Gems Besar",
                    desc = "Paling Berharga! Nilai gila!",
                    gemsCount = 1500,
                    price = "Rp 39.000",
                    onBuy = { onBuyClicked(2) }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Pack 4: Vault Pack
                ShopItemRow(
                    title = "Brankas Gems Legendaris",
                    desc = "Selesaikan game tanpa batas!",
                    gemsCount = 4000,
                    price = "Rp 99.000",
                    onBuy = { onBuyClicked(3) }
                )
            }
        }
    }
}

@Composable
fun ShopItemRow(
    title: String,
    desc: String,
    gemsCount: Int,
    price: String,
    onBuy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Gems", tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("$gemsCount Gems", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        
        Button(
            onClick = onBuy,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(price, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// ---------------------------------------------------------
// DIALOG: SETTINGS MODAL
// ---------------------------------------------------------
@Composable
fun SettingsDialog(
    onCloseClicked: () -> Unit,
    onBackToHomeClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pengaturan Game ⚙️", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                // Lanjutkan Game
                Button(
                    onClick = onCloseClicked,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lanjutkan Game", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Kembali ke Beranda
                OutlinedButton(
                    onClick = onBackToHomeClicked,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF475569)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembali ke Beranda", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
