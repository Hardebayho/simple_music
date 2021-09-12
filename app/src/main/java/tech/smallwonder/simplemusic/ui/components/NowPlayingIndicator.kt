package tech.smallwonder.simplemusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import tech.smallwonder.simplemusic.LocalModel

@Composable
fun NowPlayingIndicator() {
    val transition = rememberInfiniteTransition()
    val rate by transition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    val model = LocalModel.current
    val playing by model.isPlaying

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.5f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .width(8.dp)
                .height(if (playing) (rate * 50).dp else (0.5f * 50).dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
        )

        Box(
            modifier = Modifier
                .padding(2.dp)
                .width(8.dp)
                .height(if (playing) ((1f - rate) * 50).dp else (0.5f * 50).dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
        )

        Box(
            modifier = Modifier
                .padding(2.dp)
                .width(8.dp)
                .height(if (playing) (rate * 50).dp else (0.5f * 50).dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
        )
    }
}