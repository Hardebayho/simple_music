package tech.smallwonder.simplemusic.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.smallwonder.simplemusic.ui.theme.LobsterFontFamily

@Composable
fun AppBar(modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        elevation = 0.dp,
        backgroundColor = Color.Transparent,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(1f),
            text = "Simple Music",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5,
            fontFamily = LobsterFontFamily,
        )
    }
}