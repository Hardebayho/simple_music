package tech.smallwonder.simplemusic.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.calculateCurrentOffsetForPage
import com.google.accompanist.pager.rememberPagerState
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.R
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.utils.PlaylistRepository
import kotlin.math.absoluteValue

data class HomePageEntry(val name: String, @DrawableRes val resource: Int)

@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun HomePage(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            val pagerState = rememberPagerState(
                pageCount = 4,
                initialOffscreenLimit = 2,
            )

            AppBar(
                modifier = Modifier.fillMaxWidth()
            )

            val songs by LocalModel.current.songs
            val songsCount = songs.size

            val albums by LocalModel.current.albums
            val albumsCount = albums.size

            val artists by LocalModel.current.artists
            val artistsCount = artists.size

            val playlist = remember { PlaylistRepository.instance.playlists }

            val entries = listOf(
                HomePageEntry("$songsCount\nSongs", R.drawable.songs_img),
                HomePageEntry("$albumsCount\nAlbums", R.drawable.album_iicon),
                HomePageEntry("$artistsCount\nArtists", R.drawable.artist_icon),
                HomePageEntry("${playlist.size}\nPlaylists", R.drawable.songs_img),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth(),
            ) { page ->
                BigCard(
                    modifier = Modifier
                        .graphicsLayer {
                            val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue

                            lerp(
                                start = 0.85f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            ).also { scale ->
                                scaleX = scale
                                scaleY = scale
                            }

                            alpha = lerp(
                                start = 0.5f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                        }
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f),
                    icon = entries[page].resource,
                    title = entries[page].name,
                    onClick = {
                        when (page) {
                            0 -> goTo(navController, Screens.SongsScreen.routeWithArgs())
                            1 -> goTo(navController, Screens.AlbumsScreen.routeWithArgs())
                            2 -> goTo(navController, Screens.ArtistsScreen.routeWithArgs())
                            3 -> goTo(navController, Screens.PlaylistsScreen.routeWithArgs())
                        }
                    },
                )
            }

            NowPlayingWidget(navController)
        }
    }
}

private fun goTo(navController: NavHostController, route: String) {
    navController.navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

@ExperimentalMaterialApi
@Composable
fun BigCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes icon: Int,
) {
    HomeCard(
        onClick = onClick,
        imageResource = icon,
        modifier = modifier,
        content = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    text = title,
                    style = MaterialTheme.typography.h4.copy(
                        shadow = Shadow(blurRadius = 4.0f)
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    )
}

@ExperimentalMaterialApi
@Composable
fun SmallCard(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
) {
    HomeCard(
        onClick = onClick,
        imageResource = icon,
        modifier = modifier
            .aspectRatio(1.0f, true)
            .padding(8.dp),
        content = {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.h6.copy(
                        shadow = Shadow(
                            blurRadius = 8.0f,
                            offset = Offset(10f, 10f),
                        )
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    )
}

@ExperimentalMaterialApi
@Composable
fun HomeCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    @DrawableRes imageResource: Int,
    onClick: () -> Unit,
) {
    val primaryLight by LocalModel.current.primaryLight.observeAsState()
    val primaryDark by LocalModel.current.primaryDark.observeAsState()
    Card(
        elevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        onClick = onClick,
        backgroundColor = primaryLight!!,
    ) {
        Image(
            painter = rememberImagePainter(imageResource),
            contentDescription = "",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(primaryDark!!),
        )

        content()
    }
}
