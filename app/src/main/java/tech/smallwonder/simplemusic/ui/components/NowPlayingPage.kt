package tech.smallwonder.simplemusic.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.R
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.models.MusicModel
import tech.smallwonder.simplemusic.models.Song
import tech.smallwonder.simplemusic.utils.*

@Composable
fun NowPlayingPage(navController: NavHostController) {
    val model = LocalModel.current
    val currentSong by model.currentSong.observeAsState()

    val playlist by model.playlist
    val duration by model.duration

    val lightColor by model.primaryLight.observeAsState()
    val darkColor by model.primaryDark.observeAsState()

    currentSong?.let {
        model.isFav.value = FavoritesRepository.instance.isInFavorites(it)
        val index = playlist.indexOf(it)
        val size = playlist.size
        Column {
            NowPlayingHeader(navController, index, size)
            NowPlayingAlbumArt(
                modifier = Modifier.weight(1f),
                it,
            )
            NowPlayingTitle(it.title, "${it.artist}  •  ${it.album}", model.isFav.value, lightColor!!) {
                model.toggleFavorite(it)
                model.isFav.value = !model.isFav.value
            }
            NowPlayingProgress(duration, lightColor!!)
            NowPlayingControls(lightColor!!, darkColor!!)
        }
    }
}

@Composable
fun NowPlayingHeader(navController: NavHostController, index: Int, playlistSize: Int) {

    val model = LocalModel.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {

        val scope = rememberCoroutineScope()
        val playlist by model.playlist

        IconButton(onClick = {
            navController.popBackStack()
        }) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = "Now Playing (${index + 1}/$playlistSize)",
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = {
            model.showBottomSheet(scope) {
                LazyColumn(
                    modifier = Modifier
                        .aspectRatio(1f),
                    state = rememberLazyListState(playlist.indexOf(model.currentSong.value)),
                ) {
                    items(playlist.size) {
                        SongsListItem(song = playlist[it], onSelected = {
                            model.playPlaylistSong(it)
                            model.hideBottomSheet(scope)
                        }, options = listOf(), onOptionSelected = {option, song -> })
                    }
                }
            }
        }) {
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = "Show Playlist",
            )
        }
    }
}

@Composable
fun NowPlayingAlbumArt(modifier: Modifier = Modifier, song: Song) {
    val path by ThumbUtils.loadAlbumArt(song.uri, song.id)

    var processedPath = path.getOrAny(R.drawable.default_album_art)
    if (processedPath is String) {
        processedPath = "file://$processedPath"
    }

    Card(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .aspectRatio(1f, true),
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize(),
            painter = rememberImagePainter(processedPath),
            contentDescription = "Song Album Art",
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun NowPlayingTitle(title: String, artistAlbum: String, favorite: Boolean = false, lightColor: Color, onFavoritesSelected: () -> Unit) {
    val model = LocalModel.current

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h4.copy(
                    color = model.properLight,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = artistAlbum,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.h6.copy(
                    color = model.properLight,
                ),
            )
        }
        IconButton(
            modifier = Modifier.size(48.dp),
            onClick = onFavoritesSelected
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite Icon",
                tint = if (favorite) Color.Red.darker() else model.properLight
            )
        }
    }
}

@Composable
fun NowPlayingProgress(duration: Int, lightColor: Color) {
    val model = LocalModel.current

    val time by model.currentTime
    val timeFloat = time!!.toFloat()
    val durationFloat = duration.toFloat()
    val progress: Float = if (timeFloat <= 0 || durationFloat <= 0) 0f else timeFloat / durationFloat

    val col = if (lightColor.darkness() >= 0.8) lightColor.invert() else lightColor.manipulate(0.6f)

    Column(
        modifier = Modifier
            .padding(top = 8.dp),
    ) {
        Slider(
            value = progress, onValueChange = {
                val pos = (duration * it).toInt()
                model.seek(pos)
            },
            colors = SliderDefaults.colors(
                thumbColor = col,
                activeTrackColor = col,
                inactiveTrackColor = model.properLight,
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                DateUtils.formatElapsedTime(time!!.toLong().div(1000)),
                style = MaterialTheme.typography.body1.copy(
                    color = model.properLight,
                )
            )

            Text(
                DateUtils.formatElapsedTime(duration.toLong().div(1000)),
                style = MaterialTheme.typography.body1.copy(
                    color = model.properLight,
                )
            )
        }
    }
}

@Composable
fun NowPlayingControls(lightColor: Color, darkColor: Color) {
    val model = LocalModel.current

    val playing by model.isPlaying
    val shuffle by model.shuffle
    val repeatMode by model.repeatMode

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {
            model.toggleShuffle()
        }) {
            Icon(
                tint = model.properLight,
                imageVector = if (shuffle!!) Icons.Rounded.ShuffleOn else Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
            )
        }

        IconButton(onClick = {
            model.prev()
        }) {
            Icon(
                tint = model.properLight,
                imageVector = Icons.Rounded.FastRewind,
                contentDescription = "Rewind",
            )
        }

        IconButton(
            onClick = {
                  model.setPlaying(!(playing OR false))
            },
            Modifier
                .size(70.dp)
                .background(
                    color = lightColor,
                    shape = RoundedCornerShape(percent = 50),
                )
        ) {
            Icon(
                imageVector = if (playing OR false) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing OR false) "Play Button" else "Pause Button",
                modifier = Modifier.size(48.dp),
                tint = if (lightColor.darkness() >= 0.8) Color.White else darkColor
            )
        }

        IconButton(onClick = {
            model.next()
        }) {
            Icon(
                imageVector = Icons.Rounded.FastForward,
                contentDescription = "Skip Next Button",
                tint = model.properLight,
            )
        }

        IconButton(onClick = {
            model.toggleRepeat()
        }) {
            Icon(
                imageVector = when (repeatMode!!) {
                    RepeatMode.All -> Icons.Rounded.RepeatOn
                    RepeatMode.One -> Icons.Rounded.RepeatOneOn
                    RepeatMode.None -> Icons.Rounded.Repeat
                },
                contentDescription = "Repeat Icon",
                tint = model.properLight,
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NowPlayingWidget(navController: NavHostController) {
    // Album Art (Img) | Title & Artist (Column & Text) | Play/Pause | Next | Playlist
    val model = LocalModel.current

    val currentSong by model.currentSong.observeAsState()

    if (currentSong == null) return

    val path by ThumbUtils.loadAlbumArt(currentSong!!.uri, currentSong!!.id)

    var processedPath = path.getOrAny(R.drawable.default_album_art)
    if (processedPath is String) {
        processedPath = "file://$processedPath"
    }

    val playing by model.isPlaying

    AnimatedVisibility(visible = currentSong != null) {
        Row(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .clickable {
                    navController.navigate(Screens.NowPlayingScreen.routeWithArgs())
                }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                modifier = Modifier
                    .height(80.dp)
                    .padding(8.dp)
                    .aspectRatio(1f, true),
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxSize(),
                    painter = rememberImagePainter(processedPath),
                    contentDescription = "Song Album Art",
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = currentSong!!.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentSong!!.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = {
                model.prev()
            }) {
                Icon(Icons.Default.SkipPrevious, "Previous Song")
            }

            IconButton(onClick = {
                model.setPlaying(!playing)
            }) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause")
            }

            IconButton(onClick = {
                model.next()
            }) {
                Icon(Icons.Default.SkipNext, "Next Song")
            }
        }
    }
}