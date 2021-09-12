package tech.smallwonder.simplemusic.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.R
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.models.Album
import tech.smallwonder.simplemusic.utils.OR
import tech.smallwonder.simplemusic.utils.ThumbUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumsPage(navHostController: NavHostController) {
    val model = LocalModel.current

    val albums by model.albums

    val text by model.currentAlbumFilter

    val actual = model.filteredAlbums()

    Column {
        AlbumsAppBar(navHostController)

        val isRefreshing by model.refreshing.observeAsState()

        NowPlayingWidget(navHostController)

        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = isRefreshing OR false),
            onRefresh = {
                model.refresh()
            }
        ) {
            LazyVerticalGrid(
                modifier = Modifier
                    .weight(1f),
                cells = GridCells.Adaptive(120.dp),
            ) {
                items(actual.size) {
                    AlbumItem(actual[it], navHostController)
                }
            }
        }
    }
}

@Composable
fun AlbumItem(album: Album, controller: NavHostController) {
    // Get the first song
    val model = LocalModel.current
    val songs by model.songs
    val song = songs.find { it.albumId == album.id }
    val res by ThumbUtils.loadAlbumArt(song!!.uri, song.id)
    val path = if (res != null) "file://$res" else R.drawable.default_album_art

    val albumSongs = songs.filter { it.albumId == album.id }.sortedBy { it.trackNo }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(180.dp)
            .clickable {
                model.songsListState.value = SongsListInfoState(albumSongs, { modifier ->
                    Box(
                        modifier = modifier.fillMaxWidth()
                    ) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            painter = rememberImagePainter(path),
                            contentScale = ContentScale.Crop,
                            contentDescription = "Album Image",
                        )
                    }
                }, {song -> model.playAlbumSongs(album, albumSongs.indexOf(song))}, {
                    model.playAlbumSongs(album, 0)
                })

                controller.navigate(Screens.SongsListInfoScreen.routeWithArgs())
            }
    ) {
        // !(Image (Expanded)) Box --> Image, Rounded Btn (Play)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = rememberImagePainter(path),
                contentScale = ContentScale.Crop,
                contentDescription = "Album Image",
            )

            IconButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                onClick = {
                          model.playAlbumSongs(album, 0)
                },
            ) {
                Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Play Album ${album.title}")
            }
        }
        // Title
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = album.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
        )

        // Row (Artist - Num Songs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = album.artist.uppercase(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f),
            )
            Text(
                text = "${album.songCount} SONG(S)",
                maxLines = 1,
            )
        }
    }
}

@Composable
fun AlbumsAppBar(controller: NavHostController) {
    val model = LocalModel.current
    val text by model.currentAlbumFilter

    SearchBar(
        controller = controller,
        text = text,
        onTextChange = {
            model.currentAlbumFilter.value = it
        },
        onClearSelected = {
            model.currentAlbumFilter.value = ""
        },
        searchHint = "Search Albums",
        showMore = false,
    ) {}
}
