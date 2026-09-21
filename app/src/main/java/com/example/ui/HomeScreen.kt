package com.example.ui

import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.GameState
import com.example.game.LevelGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Represents a falling leaf state
data class FallingLeaf(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val swingSpeed: Float,
    var phase: Float,
    val color: Color,
    val rotationSpeed: Float,
    var rotation: Float
)

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var cameraTimerText by remember { mutableStateOf("07:11:37") }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Start a timer countdown effect
    LaunchedEffect(Unit) {
        var seconds = 7 * 3600 + 11 * 60 + 37
        while (true) {
            delay(1000)
            seconds = if (seconds > 0) seconds - 1 else 7 * 3600
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            cameraTimerText = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        }
    }

    // Interactive falling leaves simulation
    val leaves = remember {
        mutableStateListOf<FallingLeaf>().apply {
            repeat(15) {
                add(
                    FallingLeaf(
                        x = Random.nextFloat(),
                        y = Random.nextFloat() * -1.5f, // start offscreen above
                        speed = Random.nextFloat() * 0.005f + 0.003f,
                        size = Random.nextFloat() * 12f + 8f,
                        swingSpeed = Random.nextFloat() * 2f + 1f,
                        phase = Random.nextFloat() * 2f * PI.toFloat(),
                        color = when (Random.nextInt(4)) {
                            0 -> Color(0xFFE28743) // Warm orange
                            1 -> Color(0xFFC34A2C) // Rust red
                            2 -> Color(0xFFEAB308) // Maple gold
                            else -> Color(0xFFD97706) // Brownish-amber
                        },
                        rotationSpeed = Random.nextFloat() * 45f + 15f,
                        rotation = Random.nextFloat() * 360f
                    )
                )
            }
        }
    }

    // Ticker to move leaves
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                leaves.forEach { leaf ->
                    leaf.y += leaf.speed
                    leaf.phase += 0.02f * leaf.swingSpeed
                    leaf.rotation += leaf.rotationSpeed * 0.016f
                    
                    // Wind sway offset
                    leaf.x += sin(leaf.phase) * 0.0015f
                    
                    // Reset if fallen offscreen
                    if (leaf.y > 1.1f) {
                        leaf.y = -0.1f
                        leaf.x = Random.nextFloat()
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF7C2D12)) // Fallback deep rust
    ) {
        // 1. Scenic Autumn Photo Background
        Image(
            painter = painterResource(id = R.drawable.img_bg_autumn_1784193946736),
            contentDescription = "Scenic Autumn Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Sophisticated gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // 3. Falling Leaves Particle Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            leaves.forEach { leaf ->
                val pxX = leaf.x * canvasW + (sin(leaf.phase) * 30f)
                val pxY = leaf.y * canvasH
                
                rotate(degrees = leaf.rotation, pivot = Offset(pxX, pxY)) {
                    // Draw a cute styled maple leaf shape
                    val path = Path().apply {
                        moveTo(pxX, pxY - leaf.size)
                        // Top tip
                        lineTo(pxX + leaf.size * 0.3f, pxY - leaf.size * 0.3f)
                        lineTo(pxX + leaf.size, pxY - leaf.size * 0.4f)
                        lineTo(pxX + leaf.size * 0.4f, pxY)
                        lineTo(pxX + leaf.size * 0.7f, pxY + leaf.size * 0.5f)
                        // Bottom center
                        lineTo(pxX, pxY + leaf.size * 0.2f)
                        lineTo(pxX - leaf.size * 0.7f, pxY + leaf.size * 0.5f)
                        lineTo(pxX - leaf.size * 0.4f, pxY)
                        lineTo(pxX - leaf.size, pxY - leaf.size * 0.4f)
                        lineTo(pxX - leaf.size * 0.3f, pxY - leaf.size * 0.3f)
                        close()
                    }
                    drawPath(path = path, color = leaf.color)
                }
            }
        }

        // 4. Main Home Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. Top Bar: Statistics and Settings
            HomeTopBar(
                gems = gameState.gems,
                onSettingsClicked = { viewModel.showSettings = true }
            )

            // B. Logo Section (Upper-Center)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .animateContentSize()
            ) {
                Text(
                    text = "WORDS OF",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White.copy(alpha = 0.95f),
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.shadow(8.dp)
                )
                Text(
                    text = "WONDERS",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.shadow(8.dp)
                )
            }

            val currentLevelData = remember(gameState.currentLevel) {
                LevelGenerator.generateLevel(gameState.currentLevel)
            }
            val locationName = currentLevelData.themeName.uppercase()

            // Spacer to keep layout balanced
            Spacer(modifier = Modifier.weight(0.4f))

            // Big gorgeous glowing circular level display
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .shadow(16.dp, CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        ),
                        CircleShape
                    )
                    .border(
                        androidx.compose.foundation.BorderStroke(
                            4.dp, 
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706))
                            )
                        ),
                        CircleShape
                    )
                    .clickable {
                        viewModel.isHomeScreen = false
                        viewModel.exitDailyPuzzle()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "${gameState.currentLevel}",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24),
                        letterSpacing = (-1).sp,
                        modifier = Modifier.shadow(4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = locationName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // D. Middle-Bottom Column containing play actions & floating tools
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Red Difficulty Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFC34A2C), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "HARD",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                // Play Button (LEVEL XXX)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(64.dp)
                        .shadow(8.dp, RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF047857))
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .clickable {
                            viewModel.isHomeScreen = false
                            viewModel.exitDailyPuzzle() // ensure we are in regular level
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LEVEL ${gameState.currentLevel}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Daily Puzzle Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(58.dp)
                        .shadow(6.dp, RoundedCornerShape(29.dp))
                        .background(Color(0xFF1E293B), RoundedCornerShape(29.dp))
                        .clickable {
                            viewModel.launchDailyPuzzle()
                            viewModel.isHomeScreen = false
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "DAILY PUZZLE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp
                    )

                    // Calendar card badge
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val calendar = Calendar.getInstance()
                        val monthFmt = SimpleDateFormat("MMM", Locale.US).format(calendar.time).uppercase()
                        val dayFmt = SimpleDateFormat("d", Locale.US).format(calendar.time)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = monthFmt,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                            Text(
                                text = dayFmt,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        // Small exclamation notification badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(16.dp)
                                .background(Color.Red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Bottom row with: profile (left), lucky spin (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Button (Left)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showProfileDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Lucky Spin Wheel (Right)
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Red)
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { viewModel.showLuckyWheel = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Lucky Wheel",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        // Badge count "6"
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(18.dp)
                                .background(Color.Red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("6", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 5. Active Lucky Wheel Overlay Dialog
    if (viewModel.showLuckyWheel) {
        LuckyWheelDialog(
            viewModel = viewModel,
            onClose = { viewModel.showLuckyWheel = false }
        )
    }

    // 6. Active Profile Dialog Overlay
    if (showProfileDialog) {
        ProfileDialog(
            gameState = gameState,
            onClose = { showProfileDialog = false }
        )
    }
}

@Composable
fun HomeTopBar(
    gems: Int,
    onSettingsClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gems Indicator
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(20.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
                    .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shiny green gem
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF34D399), Color(0xFF059669))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Gem",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = gems.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        }

        // Settings gear icon
        IconButton(
            onClick = onSettingsClicked,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(38.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// LUCKY WHEEL SPIN DIALOG WITH DETAILED SECTIONS & REWARDS
// -------------------------------------------------------------
@Composable
fun LuckyWheelDialog(
    viewModel: GameViewModel,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var rotationAngle by remember { mutableStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    
    val prizes = listOf(
        "50💎" to { viewModel.awardGems(50) },
        "1🔨" to { viewModel.awardDailyHints(1) },
        "100💎" to { viewModel.awardGems(100) },
        "2🔨" to { viewModel.awardDailyHints(2) },
        "150💎" to { viewModel.awardGems(150) },
        "3🔨" to { viewModel.awardDailyHints(3) },
        "250💎" to { viewModel.awardGems(250) },
        "500💎" to { viewModel.awardGems(500) }
    )
    val colors = listOf(
        Color(0xFFEF4444), // Red
        Color(0xFFF59E0B), // Orange
        Color(0xFF10B981), // Green
        Color(0xFF3B82F6), // Blue
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFF14B8A6)  // Teal
    )

    Dialog(onDismissRequest = { if (!isSpinning) onClose() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "LUCKY SPIN WHEEL",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                
                Text(
                    text = "Putar dan dapatkan hadiah instan secara gratis!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animatable wheel graphics
                    val rotationAnimatable = remember { Animatable(0f) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(6.dp, Color.White, CircleShape)
                            .shadow(8.dp, CircleShape)
                    ) {
                        val angleSize = 360f / prizes.size
                        rotate(degrees = rotationAngle) {
                            for (i in prizes.indices) {
                                drawArc(
                                    color = colors[i % colors.size],
                                    startAngle = i * angleSize,
                                    sweepAngle = angleSize,
                                    useCenter = true,
                                    size = size
                                )
                                
                                // Text inside sector
                                val textAngle = (i * angleSize + angleSize / 2) * PI / 180f
                                val textDistance = size.width * 0.32f
                                val labelX = (size.width / 2 + textDistance * cos(textAngle)).toFloat()
                                val labelY = (size.height / 2 + textDistance * sin(textAngle)).toFloat()
                                
                                drawContext.canvas.nativeCanvas.drawText(
                                    prizes[i].first,
                                    labelX,
                                    labelY,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 32f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isFakeBoldText = true
                                    }
                                )
                            }
                        }
                        
                        // Draw central pin
                        drawCircle(
                            color = Color.White,
                            radius = 24f,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = 16f,
                            center = center
                        )
                    }

                    // Ticker Needle pointing down
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (-14).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = Path().apply {
                                moveTo(size.width / 2, size.height)
                                lineTo(size.width * 0.2f, 0f)
                                lineTo(size.width * 0.8f, 0f)
                                close()
                            }
                            drawPath(path = path, color = Color.White)
                            drawPath(path = path, color = Color.Red, style = Stroke(width = 3f))
                        }
                    }
                }

                if (resultText != null) {
                    Text(
                        text = resultText ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (!isSpinning) onClose() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = borderStroke()
                    ) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }

                    val cooldownRemaining = viewModel.luckySpinCooldownRemaining
                    val cooldownActive = cooldownRemaining > 0L
                    val buttonText = when {
                        isSpinning -> "Memutar..."
                        cooldownActive -> {
                            val totalSeconds = cooldownRemaining / 1000
                            val minutes = totalSeconds / 60
                            val seconds = totalSeconds % 60
                            String.format("TUNGGU (%02d:%02d)", minutes, seconds)
                        }
                        else -> "SPIN NOW!"
                    }

                    Button(
                        onClick = {
                            if (!isSpinning && !cooldownActive) {
                                isSpinning = true
                                resultText = null
                                coroutineScope.launch {
                                    val targetSectors = Random.nextInt(24, 48) // multi spin rotations
                                    val targetAngle = rotationAngle + (targetSectors * (360f / prizes.size)) + Random.nextFloat() * 40f
                                    
                                    // Smooth decelerating spin simulation
                                    val steps = 80
                                    for (step in 1..steps) {
                                        val t = step.toFloat() / steps
                                        val easeOut = 1f - (1f - t) * (1f - t) // cubic ease out
                                        rotationAngle = rotationAngle + (targetAngle - rotationAngle) * (easeOut - (step - 1f) / steps)
                                        delay(10 + (step * 2).toLong())
                                    }
                                    
                                    // Calculate winner index precisely pointing to 270 degrees (Top center)
                                    val sectorSize = 360f / prizes.size
                                    val pointerAngle = 270f
                                    val targetPointerAngle = (pointerAngle - rotationAngle) % 360f
                                    val positiveAngle = if (targetPointerAngle < 0) targetPointerAngle + 360f else targetPointerAngle
                                    val winnerIndex = (positiveAngle / sectorSize).toInt() % prizes.size
                                    
                                    val wonPrize = prizes[winnerIndex]
                                    wonPrize.second() // execute reward action
                                    viewModel.recordLuckySpin() // Start 5-minute cooldown
                                    resultText = "Selamat! Anda Mendapatkan:\n🎉 ${wonPrize.first} 🎉"
                                    
                                    isSpinning = false
                                }
                            }
                        },
                        enabled = !isSpinning && !cooldownActive,
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cooldownActive) Color.Gray else Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

private fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))

// -------------------------------------------------------------
// PROFILE & ACHIVEMENT OVERLAY DIALOG
// -------------------------------------------------------------
@Composable
fun ProfileDialog(
    gameState: GameState,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Avatar Card
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF334155), CircleShape)
                        .border(3.dp, Color(0xFFFB923C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "VOYAGER #23891",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "Pemain Terdaftar Sejak: Juli 2026",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Divider(color = Color.White.copy(alpha = 0.15f))

                // Stats rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileStatRow("Tingkat Level", "${gameState.currentLevel} / 1000")
                    ProfileStatRow("Total Gems Dimiliki", "${gameState.gems} 💎")
                    ProfileStatRow("Jumlah Palu", "${gameState.dailyHintsCount} 🔨")
                    ProfileStatRow("Area Terbuka", "Musim Gugur (Autumn)")
                }

                Divider(color = Color.White.copy(alpha = 0.15f))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
