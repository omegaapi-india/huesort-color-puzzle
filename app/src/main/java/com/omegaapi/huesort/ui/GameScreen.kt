package com.omegaapi.huesort.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omegaapi.huesort.AdManager
import com.omegaapi.huesort.GameEngine
import com.omegaapi.huesort.GameState
import com.omegaapi.huesort.ui.theme.GameColors
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(adManager: AdManager) {
    val activity = checkNotNull(LocalActivity.current) {
        "GameScreen requires an Activity context."
    }
    var state by remember { mutableStateOf(GameEngine.generateLevel(1)) }
    var history by remember { mutableStateOf(emptyList<GameState>()) }
    var selected by remember { mutableStateOf(-1) }
    var showWin by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            message = null
        }
    }

    fun useRewarded(action: () -> Unit) {
        if (!adManager.showRewarded(activity, action)) {
            message = "Rewarded ad is still loading. Try again in a moment."
        }
    }

    fun performPour(from: Int, to: Int) {
        val result = GameEngine.pour(state, from, to)
        if (!result.valid) {
            message = "That pour is not allowed."
            selected = -1
            return
        }
        history = history + state
        state = result.state
        selected = -1
        if (GameEngine.isSolved(state)) showWin = true
    }

    fun onTubeClick(index: Int) {
        if (selected == -1) {
            if (state.tubes[index].isEmpty()) {
                message = "Pick a tube that has colour in it."
            } else {
                selected = index
            }
        } else if (selected == index) {
            selected = -1
        } else {
            performPour(selected, index)
        }
    }

    fun undo() {
        if (history.isEmpty()) return
        state = history.last()
        history = history.dropLast(1)
        selected = -1
        showWin = false
    }

    fun askHint() {
        val move = GameEngine.hint(state)
        if (move == null) {
            message = "No hint available right now."
            return
        }
        useRewarded {
            selected = move.first
            message = "Hint: pour tube ${move.first + 1} into tube ${move.second + 1}."
        }
    }

    fun askShuffle() {
        useRewarded {
            state = GameEngine.shuffle(state, state.seed + System.currentTimeMillis())
            history = emptyList()
            selected = -1
            message = "Board shuffled. It is still solvable."
        }
    }

    fun askExtraTube() {
        if (state.bonusTubes >= GameEngine.MAX_BONUS_TUBES) {
            message = "Maximum extra tubes reached."
            return
        }
        useRewarded {
            state = GameEngine.addTube(state)
            message = "Extra tube added."
        }
    }

    fun nextLevel() {
        val completedLevel = state.level
        val next = GameEngine.generateLevel(completedLevel + 1, state.seed)
        adManager.showInterstitial(activity, shouldShow = completedLevel % 3 == 0)
        state = next
        history = emptyList()
        selected = -1
        showWin = false
    }

    fun replayLevel() {
        state = GameEngine.generateLevel(state.level, state.seed)
        history = emptyList()
        selected = -1
        showWin = false
    }

    val solvedCount = state.tubes.count { tube ->
        tube.isNotEmpty() && tube.size == state.capacity && tube.all { it == tube[0] }
    }
    val progress = solvedCount.toFloat() / state.colorCount.coerceAtLeast(1)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0x00000000),
                    titleContentColor = Color(0xFFF7F2FF),
                    actionIconContentColor = Color(0xFFC6B4FF)
                ),
                title = {
                    Column {
                        Text(
                            text = "HUE SORT",
                            style = MaterialTheme.typography.titleLarge.copy(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFFFFFF), Color(0xFFC6B4FF), Color(0xFFFFB86C))
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = "COLOR CASCADE  ·  LEVEL ${state.level}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFBFAEE6),
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showPrivacy = true }) {
                        Text("Privacy", color = Color(0xFFC6B4FF))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Backdrop3D()
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0x66B69CFF), Color(0x669B5DE5))
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(1.dp, Color(0x88FFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$solvedCount / ${state.colorCount} COMPLETE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "MOVES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFBFAEE6),
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = state.moves.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                        .height(8.dp),
                    color = Color(0xFFB69CFF),
                    trackColor = Color(0x22FFFFFF),
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = "Pour matching colours together. Fill every tube with one colour.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE7DFFF),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0x22FFFFFF), Color(0x08FFFFFF))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.tubes.indices.toList()) { index ->
                            TubeView3D(
                                index = index,
                                tube = state.tubes[index],
                                capacity = state.capacity,
                                selected = selected == index,
                                onClick = { onTubeClick(index) }
                            )
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        GlassButton(
                            "↶  UNDO",
                            ::undo,
                            history.isNotEmpty(),
                            Color(0xFF9B5DE5)
                        )
                    }
                    item { GlassButton("✦  HINT", ::askHint, true, Color(0xFF00BBF9)) }
                    item { GlassButton("⟳  SHUFFLE", ::askShuffle, true, Color(0xFFF9C74F)) }
                    item { GlassButton("+  TUBE", ::askExtraTube, true, Color(0xFF43AA8B)) }
                }

                Text(
                    text = "★ Rewarded ads unlock in-game helpers only. No real-world rewards.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFBFAEE6),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (showWin) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    "🏆  LEVEL ${state.level} CLEARED",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFFFFB86C),
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            },
            text = {
                Text("Solved in ${state.moves} moves. Ready for a slightly harder board?")
            },
            confirmButton = {
                Button(onClick = ::nextLevel) {
                    Text("Next level")
                }
            },
            dismissButton = {
                TextButton(onClick = ::replayLevel) {
                    Text("Replay")
                }
            }
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy & ads") },
            text = {
                Column {
                    Text(
                        "Hue Sort does not ask for signup, location, contacts, photos, or personal details. " +
                            "Your puzzle progress stays on this device only."
                    )
                    Text(
                        "Ads are served by Google AdMob. Ad identifiers may be used to show relevant ads. " +
                            "Rewarded ads unlock only in-game helpers such as hints and extra tubes.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Support: info@omegaapi.com",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun GlassButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    accent: Color
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.75f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = accent.copy(alpha = 0.16f),
            contentColor = Color(0xFFFFFFFF),
            disabledContentColor = Color(0x66FFFFFF),
            disabledContainerColor = Color(0x0AFFFFFF)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
private fun Backdrop3D() {
    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF44208A), Color(0xFF1A0D36), Color(0xFF080512)),
                center = Offset(w * 0.5f, h * 0.16f + drift * 90f),
                radius = size.maxDimension * 1.2f
            )
        )

        for (star in 0 until 36) {
            val x = ((star * 97) % 100) / 100f * w
            val y = ((star * 53) % 100) / 100f * h * 0.62f
            val wave = (sin(twinkle * 2f * PI.toFloat() + star) + 1f) / 2f
            drawCircle(
                color = Color(0xFFFFFFFF).copy(alpha = 0.16f + wave * 0.5f),
                radius = 1.2f + (star % 3) * 0.7f,
                center = Offset(x, y)
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x4DFF7BAC), Color(0x00000000)),
                center = Offset(w * (0.12f + drift * 0.04f), h * 0.70f),
                radius = w * 0.62f
            ),
            radius = w * 0.62f,
            center = Offset(w * (0.12f + drift * 0.04f), h * 0.70f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x4D43AA8B), Color(0x00000000)),
                center = Offset(w * (0.94f - drift * 0.05f), h * 0.38f),
                radius = w * 0.56f
            ),
            radius = w * 0.56f,
            center = Offset(w * (0.94f - drift * 0.05f), h * 0.38f)
        )

        val vanishing = Offset(w * 0.5f, h * 0.30f)
        for (line in -7..7) {
            val bottomX = w * 0.5f + line * (w / 9f)
            drawLine(
                color = Color(0x1AB69CFF),
                start = vanishing,
                end = Offset(bottomX, h),
                strokeWidth = 1f
            )
        }
        for (line in 0..9) {
            val y = h * (0.32f + line * 0.075f) + drift * 24f
            drawLine(
                color = Color(0x1AB69CFF),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3DB69CFF), Color(0x00000000)),
                center = Offset(w * 0.5f, h * 0.93f),
                radius = w * 0.7f
            ),
            topLeft = Offset(-w * 0.2f, h * 0.83f),
            size = Size(w * 1.4f, h * 0.2f)
        )
    }
}

@Composable
private fun TubeView3D(
    index: Int,
    tube: List<Int>,
    capacity: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val reflectionShift = if (index % 2 == 0) 8f else 12f
    val lift by animateFloatAsState(
        targetValue = if (selected) 14f else 0f,
        label = "tubeLift"
    )
    val pulseTransition = rememberInfiniteTransition(label = "tubePulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .width(76.dp)
            .height((capacity * 52 + 56).dp)
            .clickable(onClick = onClick)
            .graphicsLayer {
                cameraDistance = 14f * density
                rotationX = if (selected) -7f else 2f
                rotationY = if (index % 2 == 0) -4f else 4f
                translationY = -lift
                shadowElevation = if (selected) 34f else 14f
                shape = RoundedCornerShape(28.dp)
                clip = false
            }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val top = 26f
            val bottom = h - 18f
            val tubeW = w * 0.58f
            val left = (w - tubeW) / 2f
            val rimHeight = 18f
            val innerTop = top + 10f
            val segmentHeight = (bottom - innerTop - 6f) / capacity

            drawOval(
                color = Color.Black.copy(alpha = 0.38f),
                topLeft = Offset(left - 10f, bottom - 4f),
                size = Size(tubeW + 20f, 20f)
            )

            if (selected) {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xB3B69CFF).copy(alpha = 0.35f + pulse * 0.35f),
                            Color(0x00000000)
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = h * 0.6f
                    ),
                    topLeft = Offset(left - 18f, top - 18f),
                    size = Size(tubeW + 36f, bottom - top + 36f),
                    cornerRadius = CornerRadius(30f)
                )
            }

            val glassBody = Path().apply {
                moveTo(left, top)
                lineTo(left, bottom - 14f)
                quadraticTo(left, bottom, left + tubeW / 2f, bottom)
                quadraticTo(left + tubeW, bottom, left + tubeW, bottom - 14f)
                lineTo(left + tubeW, top)
                close()
            }

            drawPath(
                path = glassBody,
                brush = Brush.linearGradient(
                    listOf(Color(0x3DFFFFFF), Color(0x14FFFFFF), Color(0x2BFFFFFF))
                )
            )
            drawPath(
                path = glassBody,
                brush = Brush.verticalGradient(listOf(Color(0x26000000), Color(0x05000000)))
            )

            tube.forEachIndexed { position, colorIndex ->
                val base = GameColors.tubes[colorIndex % GameColors.tubes.size]
                val light = blend(base, Color.White, 0.32f)
                val dark = blend(base, Color.Black, 0.42f)
                val segmentTop = bottom - (position + 1) * segmentHeight

                drawRect(
                    brush = Brush.verticalGradient(listOf(light, base, dark)),
                    topLeft = Offset(left + 3f, segmentTop + 1f),
                    size = Size(tubeW - 6f, segmentHeight - 2f)
                )
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(light, base),
                        center = Offset(left + tubeW / 2f, segmentTop + 5f),
                        radius = tubeW / 2f
                    ),
                    topLeft = Offset(left + 3f, segmentTop - 6f),
                    size = Size(tubeW - 6f, 12f)
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.25f),
                    start = Offset(left + 3f, segmentTop + segmentHeight - 2f),
                    end = Offset(left + tubeW - 3f, segmentTop + segmentHeight - 2f),
                    strokeWidth = 1.5f
                )
            }

            drawRect(
                color = Color(0x33000000),
                topLeft = Offset(left + 3f, innerTop),
                size = Size(7f, bottom - innerTop - 6f)
            )
            drawRect(
                color = Color(0x1AFFFFFF),
                topLeft = Offset(left + tubeW - 10f, innerTop),
                size = Size(7f, bottom - innerTop - 6f)
            )

            drawPath(
                path = glassBody,
                brush = Brush.linearGradient(
                    listOf(Color(0x2EFFFFFF), Color(0x00000000), Color(0x12FFFFFF))
                )
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0x66FFFFFF), Color(0x00FFFFFF))
                ),
                topLeft = Offset(left + reflectionShift, top + 10f),
                size = Size(7f, bottom - top - 26f),
                cornerRadius = CornerRadius(4f)
            )

            drawOval(
                brush = Brush.linearGradient(
                    listOf(Color(0xB3FFFFFF), Color(0x33FFFFFF), Color(0x99FFFFFF))
                ),
                topLeft = Offset(left - 3f, top - 6f),
                size = Size(tubeW + 6f, rimHeight),
                style = Stroke(width = 3f)
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66000000), Color(0x00000000)),
                    center = Offset(left + tubeW / 2f, top),
                    radius = tubeW / 2f
                ),
                topLeft = Offset(left, top - 3f),
                size = Size(tubeW, 12f)
            )

            if (selected) {
                drawOval(
                    color = Color(0xFFC6B4FF).copy(alpha = 0.65f + pulse * 0.35f),
                    topLeft = Offset(left - 11f, top - 12f),
                    size = Size(tubeW + 22f, h - 26f),
                    style = Stroke(width = 3f + pulse * 2f)
                )
            }
        }
    }
}

private fun blend(first: Color, second: Color, amount: Float): Color = Color(
    alpha = first.alpha,
    red = first.red + (second.red - first.red) * amount,
    green = first.green + (second.green - first.green) * amount,
    blue = first.blue + (second.blue - first.blue) * amount
)
