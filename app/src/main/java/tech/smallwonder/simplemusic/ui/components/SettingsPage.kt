package tech.smallwonder.simplemusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsPage() {
    Column {
        Text(
            text = "Settings",
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            style = MaterialTheme.typography.h6,
        )
        Divider()
        LazyColumn(
            modifier = Modifier
                .weight(1f),
        ) {
            item {
                SettingsTile("Pause On Headset Plugged/Unplugged", { Checkbox(checked = true, onCheckedChange = {}) }) {}
            }
            item {
                SettingsTile("Enable Experimental Features", { Checkbox(checked = true, onCheckedChange = {}) }) {}
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsTile(title: String, suffix: @Composable () -> Unit = {}, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier
                .weight(1f),
        )

        suffix()
    }
}