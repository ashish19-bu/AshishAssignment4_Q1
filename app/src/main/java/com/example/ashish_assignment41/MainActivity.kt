package com.example.ashish_assignment41

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ashish_assignment41.ui.theme.Ashish_Assignment41Theme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fix 3: Lock the screen to Portrait mode so tilting doesn't rotate the UI
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        enableEdgeToEdge()
        setContent {
            Ashish_Assignment41Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GyroBallGame(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun GyroBallGame(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val gyroscope = remember { sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }

    var ballX by remember { mutableStateOf(100f) }
    var ballY by remember { mutableStateOf(100f) }
    var velX by remember { mutableStateOf(0f) }
    var velY by remember { mutableStateOf(0f) }

    val ballRadius = 30f
    var isGameWon by remember { mutableStateOf(false) }

    val walls = remember {
        listOf(
            Rect(0f, 400f, 700f, 450f),
            Rect(300f, 850f, 1080f, 900f),
            Rect(0f, 1300f, 800f, 1350f)
        )
    }

    val traps = remember {
        listOf(
            Rect(800f, 500f, 950f, 650f),
            Rect(100f, 1000f, 250f, 1150f)
        )
    }

    val goal = remember { Rect(850f, 1600f, 1050f, 1800f) }

    // Fix 1: Safer Sensor Registration (Check for null)
    DisposableEffect(Unit) {
        if (gyroscope == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null && event.sensor.type == Sensor.TYPE_GYROSCOPE && !isGameWon) {
                        velX += event.values[1] * 1.5f 
                        velY += event.values[0] * 1.5f
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFFF0F0F0))) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        LaunchedEffect(isGameWon) {
            while (!isGameWon) {
                velX *= 0.96f
                velY *= 0.96f

                var nextX = ballX + velX
                var nextY = ballY + velY

                if (nextX < ballRadius) { nextX = ballRadius; velX = 0f }
                if (nextX > width - ballRadius) { nextX = width - ballRadius; velX = 0f }
                if (nextY < ballRadius) { nextY = ballRadius; velY = 0f }
                if (nextY > height - ballRadius) { nextY = height - ballRadius; velY = 0f }

                // Check collision with walls using the predicted position
                val predictionRect = Rect(nextX - ballRadius, nextY - ballRadius, nextX + ballRadius, nextY + ballRadius)
                var wallHit = false
                for (wall in walls) {
                    if (predictionRect.overlaps(wall)) { wallHit = true; break }
                }

                if (!wallHit) {
                    ballX = nextX
                    ballY = nextY
                } else {
                    velX = -velX * 0.4f
                    velY = -velY * 0.4f
                }

                // Fix 2: Use the updated ball position for Trap and Goal checks
                val currentBallRect = Rect(
                    ballX - ballRadius,
                    ballY - ballRadius,
                    ballX + ballRadius,
                    ballY + ballRadius
                )

                for (trap in traps) {
                    if (currentBallRect.overlaps(trap)) {
                        ballX = 100f
                        ballY = 100f
                        velX = 0f
                        velY = 0f
                    }
                }

                if (currentBallRect.overlaps(goal)) {
                    isGameWon = true
                }

                delay(16)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFF2ECC71), topLeft = goal.topLeft, size = goal.size)
            traps.forEach { trap ->
                drawRect(color = Color(0xFFE67E22), topLeft = trap.topLeft, size = trap.size)
            }
            walls.forEach { wall ->
                drawRect(color = Color(0xFF34495E), topLeft = wall.topLeft, size = wall.size)
            }
            drawCircle(
                color = if (isGameWon) Color.Yellow else Color(0xFF3498DB),
                radius = ballRadius,
                center = Offset(ballX, ballY)
            )
        }

        if (isGameWon) {
            Column(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("MAZE COMPLETED!", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { 
                    ballX = 100f; ballY = 100f; velX = 0f; velY = 0f; isGameWon = false 
                }) {
                    Text("Restart Level")
                }
            }
        }
    }
}
