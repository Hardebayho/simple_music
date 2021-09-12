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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.R
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.models.Album
import tech.smallwonder.simplemusic.models.Artist
import tech.smallwonder.simplemusic.utils.OR
import tech.smallwonder.simplemusic.utils.ThumbUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistsPage(navHostController: NavHostController) {
    val model = LocalModel.current

    val artists by model.artists

    val text by model.currentArtistFilter

    val actual = model.filteredArtists()

    Column {
        ArtistsAppBar(navHostController)

        NowPlayingWidget(navHostController)

        val model = LocalModel.current
        val isRefreshing by model.refreshing.observeAsState()

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
                    ArtistItem(actual[it], navHostController)
                }
            }
        }
    }
}

@Composable
fun ArtistItem(artist: Artist, controller: NavHostController) {
    // Get the first song
    val model = LocalModel.current
    val songs by model.songs
    val song = songs.find { it.artistId == artist.id }
    val res by ThumbUtils.loadAlbumArt(song!!.uri, song.id)
    val path = if (res != null) "file://$res" else R.drawable.default_album_art

    val artistSongs = songs.filter { it.artistId == artist.id }.sortedBy { it.trackNo }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(180.dp)
            .clickable {
                model.songsListState.value = SongsListInfoState(artistSongs, { modifier ->
                    Box(
                        modifier = modifier.fillMaxWidth(),
                    ) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            painter = rememberImagePainter(path),
                            contentScale = ContentScale.Crop,
                            contentDescription = "Artist Album Art",
                        )
                    }
                }, {song -> model.playListSong(artistSongs, song)}, {
                    model.playListSong(artistSongs, artistSongs[0])
                })
                controller.navigate(Screens.SongsListInfoScreen.routeWithArgs())
            }
    ) {
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
                contentDescription = "Artist Album Art",
            )

            IconButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                onClick = {
                          model.playArtistSongs(artist, 0)
                },
            ) {
                Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Play Artist ${artist.title}")
            }
        }
        // Title
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = artist.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "${artist.songCount} SONG(S)",
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ArtistsAppBar(controller: NavHostController) {
    val model = LocalModel.current
    val text by model.currentArtistFilter
    val scope = rememberCoroutineScope()

    SearchBar(
        controller = controller,
        text = text,
        onTextChange = {
            model.currentArtistFilter.value = it
        },
        onClearSelected = {
            model.currentArtistFilter.value = ""
        },
        searchHint = "Search Artists",
        showMore = false,
    ) {
        model.showBottomSheet(scope) {}
    }
}
