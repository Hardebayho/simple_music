package tech.smallwonder.simplemusic.ui.components

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.*
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.R
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.models.MusicModel
import tech.smallwonder.simplemusic.models.Song
import tech.smallwonder.simplemusic.utils.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SongsPage(navController: NavHostController) {
    val model = LocalModel.current
    val scrollState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SongsAppBar(controller = navController)
            NowPlayingWidget(navController)
            SongsList(
                modifier = Modifier.weight(1f),
                onSongSelected = {
                    model.playSongsSong(it)
                    navController.navigate(Screens.NowPlayingScreen.routeWithArgs()) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                scrollState,
            )
        }

        val currentSong by model.currentSong.observeAsState()
        val scope = rememberCoroutineScope()

        AnimatedVisibility(
            visible = currentSong != null,
            modifier = Modifier
                .align(Alignment.BottomEnd),
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        val groupedSongs = model.songsSortGroup()
                        // Add the header indices to the index
                        var idx = 0
                        for ((_, value) in groupedSongs) {
                            if (value.contains(model.currentSong.value!!)) {
                                break
                            }
                            idx++
                        }
                        scrollState.scrollToItem(model.songs.value.indexOf(model.currentSong.value!!) + idx)
                    }
                }
            ) {
                Icon(Icons.Default.GpsFixed, "Scroll to current song")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongsList(
    modifier: Modifier = Modifier,
    onSongSelected: (Song) -> Unit,
    state: LazyListState,
) {
    val model = LocalModel.current
    val isRefreshing by model.refreshing.observeAsState()

    val groupedSongs = model.songsSortGroup()

    val filter by model.currentSongFilter
    val scope = rememberCoroutineScope()

    Log.d("SongsList", "Current filter: $filter")

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing = isRefreshing OR false),
        onRefresh = {
            model.refresh()
        }
    ) {
        LazyColumn(
            modifier = modifier,
            state = state,
        ) {
            groupedSongs.forEach { (initial, songs) ->
                stickyHeader {
                    CharacterHeader(
                        char = initial,
                        modifier = Modifier.fillParentMaxWidth()
                    )
                }
                items(songs.size) {
                    val song = songs[it]
                    SongsListItem(song = song, onSelected = onSongSelected, MusicModel.songOptions, onOptionSelected = { option, _ ->
                        model.onSongOptionSelected(option, song, scope)
                    })

                }
            }
        }
    }
}

@Composable
fun CharacterHeader(char: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = Color.Black)
    ) {
        Text(
            char,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SongsListItem(song: Song, onSelected: (song: Song) -> Unit, options: List<Option>, onOptionSelected: (option: Option, song: Song) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelected(song)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val path by ThumbUtils.loadAlbumArt(song.uri, song.id)

        val res = if (path == null) R.drawable.default_album_art else "file://$path"
        val model = LocalModel.current
        val scope = rememberCoroutineScope()

        val currentSong by model.currentSong.observeAsState()

        val isCurrent = currentSong == song

        Box(
            modifier = Modifier
                .size(50.dp),
        ) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                painter = rememberImagePainter(
                    res,
                    builder = {
                        crossfade(true)
                    }
                ),
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
            )

            if (isCurrent) {
                NowPlayingIndicator()
            }
        }

        Column(modifier = Modifier
            .weight(1f)
            .padding(8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                song.title,
                style = MaterialTheme.typography.h6,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                "${song.artist}  •  ${song.album}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (options.isNotEmpty()) {
            MediaIcon(
                vector = Icons.Filled.MoreHoriz,
                onClick = {
                    model.showBottomSheet(scope) {
                        options.forEach {
                            OptionTile(it) {
                                model.hideBottomSheet(scope)
                                onOptionSelected(it, song)
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun MediaIcon(vector: ImageVector, onClick: () -> Unit, contentDescription: String = "", tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = vector,
            contentDescription = contentDescription,
            tint = tint,
        )
    }
}

@Composable
fun SongsAppBar(controller: NavHostController) {
    val model = LocalModel.current
    val text by model.currentSongFilter
    val scope = rememberCoroutineScope()

    SearchBar(
        controller = controller,
        text = text,
        onTextChange = {
            model.currentSongFilter.value = it
        },
        onClearSelected = {
            model.currentSongFilter.value = ""
        },
        searchHint = "Search Songs",
    ) {
        model.showBottomSheet(scope) {
            SongSortOption(scope, model)
        }
    }
}

@Composable
fun SongSortOption(scope: CoroutineScope, model: MusicModel) {
    val sortOrder by model.sortOrder.observeAsState()

    OptionTile(Option(Icons.Default.Sort, "Sort")) {
        model.showBottomSheet(scope) {
            OptionTile(Option(Icons.Default.Sort, "Title"), if (sortOrder is SortOrder.Title) {
                { Icon(Icons.Default.Check, "") }
            } else {{}}) {
                model.setSortOrder(SortOrder.Title())
                model.hideBottomSheet(scope)
            }
            OptionTile(Option(Icons.Default.Sort, "Album"), if (sortOrder is SortOrder.Album) {
                { Icon(Icons.Default.Check, "") }
            } else {{}}) {
                model.setSortOrder(SortOrder.Album())
                model.hideBottomSheet(scope)
            }
            OptionTile(Option(Icons.Default.Sort, "Artist"), if (sortOrder is SortOrder.Artist) {
                { Icon(Icons.Default.Check, "") }
            } else {{}}) {
                model.setSortOrder(SortOrder.Artist())
                model.hideBottomSheet(scope)
            }
            OptionTile(Option(Icons.Default.Sort, "Track"), if (sortOrder is SortOrder.Track) {
                { Icon(Icons.Default.Check, "") }
            } else {{}}) {
                model.setSortOrder(SortOrder.Track())
                model.hideBottomSheet(scope)
            }
        }
    }
}
