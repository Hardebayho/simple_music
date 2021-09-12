package tech.smallwonder.simplemusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class Option(val icon: ImageVector, val title: String)

@Composable
fun OptionTile(
    option: Option,
    trailingIcon: @Composable () -> Unit = {},
    onClick: () -> Unit,
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .padding(16.dp),
    ) {
        Icon(option.icon, option.title)

        Text(
            text = option.title,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            maxLines = 1,
        )

        trailingIcon()
    }
}