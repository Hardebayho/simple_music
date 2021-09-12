package tech.smallwonder.simplemusic.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.SpaceAround
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.models.MusicModel
import tech.smallwonder.simplemusic.models.Playlist
import tech.smallwonder.simplemusic.utils.FavoritesRepository
import tech.smallwonder.simplemusic.utils.PlaylistRepository
import kotlin.random.Random

@Composable
fun PlaylistsPage(controller: NavHostController) {
    val model = LocalModel.current
    val text by model.currentPlaylistFilter

    val playlists = PlaylistRepository.instance.playlists
    val scope = rememberCoroutineScope()

    val primaryLight by model.primaryLight.observeAsState()
    val primaryDark by model.primaryDark.observeAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                model.showBottomSheet(scope) {
                    PlaylistTextLayout { value ->
                        if (value.trim().isNotEmpty()) {
                            if (!PlaylistRepository.instance.exists(value.trim())) {
                                PlaylistRepository.instance.updatePlaylist(Playlist(
                                    value.trim(),
                                    listOf()
                                ))
                                model.hideBottomSheet(scope)
                            } else {
                                model.showToast("Playlist already exists!", Toast.LENGTH_SHORT)
                            }
                        }
                    }
                }
            }, backgroundColor = primaryLight!!) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Playlist", tint = primaryDark!!)
            }
        },
        backgroundColor = Color.Transparent,
    ) {
        Column {
            SearchBar(
                controller = controller,
                text = text,
                onTextChange = {
                    model.currentPlaylistFilter.value = it
                },
                onClearSelected = {
                    model.currentPlaylistFilter.value = ""
                },
                searchHint = "Search Playlists",
                showMore = false,
            ) {}

            NowPlayingWidget(controller)

            LazyColumn(
                modifier = Modifier
                    .weight(1f),
            ) {
                item {
                    val favoriteSongs = FavoritesRepository.instance.getSongs()
                    PlaylistItem(icon = Icons.Default.Favorite, title = "Favorites", numSongs = favoriteSongs.size) {
                        model.songsListState.value = SongsListInfoState(
                            favoriteSongs,
                            { modifier ->
                                Box(
                                    modifier = modifier
                                        .fillMaxWidth()
                                        .background(Color.Red),
                                ) {
                                    Icon(
                                        modifier = Modifier.fillMaxSize(),
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Favorite Icon",
                                    )
                                }
                            },
                            { song -> model.playListSong(favoriteSongs, song) },
                            onFabClicked = {
                                model.playListSong(favoriteSongs, favoriteSongs[0])
                            },
                            options = MusicModel.favoriteOptions,
                            onOptionSelected = {option, song ->
                                model.onFavoriteSongOptionSelected(option, song)
                            }
                        )
                        controller.navigate(Screens.SongsListInfoScreen.routeWithArgs())
                    }
                }

                items(playlists.size) {
                    val playlist = playlists[it]
                    val numSongs = playlist.songs.size

                    val colors = listOf(Color.Green, Color.Black, Color.Blue, Color.Magenta, Color.DarkGray)

                    val randomColor = colors[Random.nextInt(colors.size)]

                    PlaylistItem(icon = Icons.Default.PlaylistPlay, title = playlist.title, numSongs = numSongs, color = randomColor, optionClick = {
                        model.showBottomSheet(scope) {
                            OptionTile(option = Option(Icons.Default.DeleteForever, "Delete")) {
                                model.showBottomSheet(scope) {
                                    YesOrNoDialog {
                                        model.hideBottomSheet(scope)
                                        if (it) {
                                            PlaylistRepository.instance.removePlaylist(playlist)
                                        }
                                    }
                                }
                            }
                            OptionTile(option = Option(Icons.Default.Info, "Rename")) {
                                model.showBottomSheet(scope) {
                                    PlaylistTextLayout(playlist.title) { value ->
                                        if (value.trim().isEmpty()) return@PlaylistTextLayout
                                        model.hideBottomSheet(scope)
                                        PlaylistRepository.instance.renamePlaylist(playlist, value.trim())
                                    }
                                }
                            }
                        }
                    }, showOptions = true) {
                        model.songsListState.value = SongsListInfoState(
                            songs = playlist.songs, headerImage = { modifier ->
                                Box(
                                    modifier = modifier
                                        .fillMaxWidth(),
                                ) {
                                    Icon(
                                        modifier = Modifier.fillMaxSize(),
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Favorite Icon",
                                    )
                                }
                        }, onSongSelected = {song -> model.playListSong(playlist.songs, song)}, onFabClicked = {
                                if (playlist.songs.isNotEmpty()) {
                                    model.playListSong(playlist.songs, playlist.songs[0])
                                }
                        }, options = MusicModel.playlistOptions, onOptionSelected = {option, song ->
                                model.onPlaylistSongOptionSelected(playlist, song, option)
                            })
                        controller.navigate(Screens.SongsListInfoScreen.routeWithArgs())
                    }
                }
            }
        }
    }
}

@Composable
fun YesOrNoDialog(onResult: (affirmative: Boolean) -> Unit) {
    Text(
        modifier = Modifier
            .padding(8.dp),
        text = "Are you sure you want to delete this playlist?",
        textAlign = TextAlign.Center,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Button(onClick = {
            onResult(true)
        }) {
            Text("Yes")
        }

        Button(onClick = {
            onResult(false)
        }) {
            Text("No")
        }
    }
}

@Composable
fun PlaylistTextLayout(text: String = "", onFinished: (text: String) -> Unit) {
    var value by remember { mutableStateOf(text) }

    Column(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = { value = it }
        )
        Box(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onFinished(value)
            },
            modifier = Modifier.align(CenterHorizontally),
        ) {
            Text("Okay")
        }
    }
}

@Composable
fun PlaylistItem(icon: ImageVector, title: String, numSongs: Int = -1, color: Color = Color.Red, showOptions: Boolean = false, optionClick: () -> Unit = {}, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(60.dp)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .background(color, RoundedCornerShape(8.dp)),
        ) {
            Icon(
                modifier = Modifier
                    .align(Center),
                imageVector = icon,
                contentDescription = "",
                tint = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.h6,
                maxLines = 1,
            )
            if (numSongs >= 0) {
                Text(
                    "$numSongs song(s)",
                    maxLines = 1,
                )
            }
        }

        if (showOptions) {
            IconButton(onClick = { optionClick() }) {
                Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "")
            }
        }
    }
}