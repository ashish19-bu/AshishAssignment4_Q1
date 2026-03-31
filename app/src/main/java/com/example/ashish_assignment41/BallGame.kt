package com.example.ashish_assignment41

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BallGame(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val gyroscope = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }

    var ballX by remember { mutableStateOf(100f) }
    var ballY by remember { mutableStateOf(100f) }
    var velocityX by remember { mutableStateOf(0f) }
    var velocityY by remember { mutableStateOf(0f) }

    val ballRadius = 30f
    
    // Game States
    var gameWon by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }

    // Maze Layout (Static obstacles)
    val obstacles = remember {
        listOf(
            Rect(0f, 300f, 600f, 350f),
            Rect(400f, 600f, 1080f, 650f),
            Rect(0f, 900f, 700f, 950f),
            Rect(300f, 1200f, 1080f, 1250f),
            Rect(0f, 1500f, 500f, 1550f)
        )
    }
    
    // Moving obstacles or traps could be added for uniqueness
    val traps = remember {
        listOf(
            Rect(700f, 1000f, 800f, 1100f),
            Rect(200f, 1350f, 300f, 1450f)
        )
    }

    val goal = remember { Rect(800f, 1600f, 1000f, 1800f) }

    // Reset Function
    fun resetGame() {
        ballX = 100f
        ballY = 100f
        velocityX = 0f
        velocityY = 0f
        gameWon = false
        gameOver = false
    }

    // Sensor Listener
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE && !gameWon && !gameOver) {
                    // Gyroscope values: [0] = X-axis rate, [1] = Y-axis rate
                    // We treat these as acceleration forces for the ball
                    velocityX += event.values[1] * 1.5f 
                    velocityY += event.values[0] * 1.5f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Game Loop
        LaunchedEffect(gameWon, gameOver) {
            while (!gameWon && !gameOver) {
                // Apply simple friction
                velocityX *= 0.98f
                velocityY *= 0.98f

                val nextX = ballX + velocityX
                val nextY = ballY + velocityY

                // Wall Collisions
                var updatedX = nextX
                var updatedY = nextY

                if (updatedX - ballRadius < 0) {
                    updatedX = ballRadius
                    velocityX = -velocityX * 0.4f
                } else if (updatedX + ballRadius > screenWidth) {
                    updatedX = screenWidth - ballRadius
                    velocityX = -velocityX * 0.4f
                }

                if (updatedY - ballRadius < 0) {
                    updatedY = ballRadius
                    velocityY = -velocityY * 0.4f
                } else if (updatedY + ballRadius > screenHeight) {
                    updatedY = screenHeight - ballRadius
                    velocityY = -velocityY * 0.4f
                }

                val ballRect = Rect(updatedX - ballRadius, updatedY - ballRadius, updatedX + ballRadius, updatedY + ballRadius)
                
                // Obstacle Collisions
                var hitObstacle = false
                for (obs in obstacles) {
                    if (ballRect.overlaps(obs)) {
                        hitObstacle = true
                        break
                    }
                }

                if (hitObstacle) {
                    // Stop or bounce
                    velocityX = -velocityX * 0.6f
                    velocityY = -velocityY * 0.6f
                } else {
                    ballX = updatedX
                    ballY = updatedY
                }
                
                // Trap Collisions
                for (trap in traps) {
                    if (ballRect.overlaps(trap)) {
                        gameOver = true
                    }
                }

                // Check Goal
                if (ballRect.overlaps(goal)) {
                    gameWon = true
                }

                delay(16)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Grid Background for a techy look
            val gridSize = 100f
            for (x in 0..(size.width / gridSize).toInt()) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(x * gridSize, 0f), Offset(x * gridSize, size.height))
            }
            for (y in 0..(size.height / gridSize).toInt()) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y * gridSize), Offset(size.width, y * gridSize))
            }

            // Draw Obstacles (Walls)
            obstacles.forEach { obs ->
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))),
                    topLeft = obs.topLeft,
                    size = obs.size
                )
                drawRect(
                    color = Color(0xFF64748B),
                    topLeft = obs.topLeft,
                    size = obs.size,
                    style = Stroke(width = 2f)
                )
            }
            
            // Draw Traps
            traps.forEach { trap ->
                drawRect(
                    brush = Brush.radialGradient(listOf(Color.Red.copy(alpha = 0.6f), Color.Transparent), center = trap.center),
                    topLeft = trap.topLeft,
                    size = trap.size
                )
                drawRect(Color.Red, topLeft = trap.topLeft, size = trap.size, style = Stroke(width = 4f))
            }

            // Draw Goal
            drawRect(
                brush = Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF166534))),
                topLeft = goal.topLeft,
                size = goal.size
            )
            drawRect(Color.Green, topLeft = goal.topLeft, size = goal.size, style = Stroke(width = 4f))

            // Draw Ball with Neon Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8), Color.Transparent),
                    center = Offset(ballX, ballY),
                    radius = ballRadius * 2
                )
            )
            drawCircle(
                color = Color(0xFF0EA5E9),
                radius = ballRadius,
                center = Offset(ballX, ballY)
            )
        }

        // UI Overlay
        if (gameWon || gameOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (gameWon) "MISSION COMPLETE" else "SYSTEM FAILURE",
                        color = if (gameWon) Color.Green else Color.Red,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { resetGame() }) {
                        Text("REBOOT SYSTEM")
                    }
                }
            }
        }
    }
}
