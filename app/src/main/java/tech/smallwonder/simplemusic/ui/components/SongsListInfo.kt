package tech.smallwonder.simplemusic.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.models.Song
import tech.smallwonder.simplemusic.utils.FavoritesRepository
import tech.smallwonder.simplemusic.utils.PlaylistRepository

data class SongsListInfoState(
    val songs: List<Song>,
    val headerImage: @Composable (modifier: Modifier) -> Unit,
    val onSongSelected: (song: Song) -> Unit,
    val onFabClicked: () -> Unit,
    val options: List<Option> = listOf(),
    val onOptionSelected: (option: Option, song: Song) -> Unit = {_, _ -> },
)

@Composable
fun SongsListInfo(controller: NavHostController) {
    val model = LocalModel.current
    val primaryLight by model.primaryLight.observeAsState()
    val primaryDark by model.primaryDark.observeAsState()

    val state by model.songsListState

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            NowPlayingWidget(controller)

            state.headerImage(Modifier.height(300.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                items(state.songs.size) { index ->
                    SongsListItem(
                        song = state.songs[index],
                        onSelected = { state.onSongSelected(state.songs[index]) },
                        options = state.options,
                        onOptionSelected = state.onOptionSelected,
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { state.onFabClicked() },
            backgroundColor = primaryLight!!,
            modifier = Modifier
                .align(Alignment.BottomEnd),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play song playlist",
                tint = primaryDark!!,
            )
        }
    }
}
