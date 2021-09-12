package tech.smallwonder.simplemusic

import android.annotation.SuppressLint

sealed class Screens(val route: String, val args: Array<String>) {
    @SuppressLint("CustomSplashScreen")
    object SplashScreen : Screens("splash_screen", arrayOf())
    object HomeScreen : Screens("home_screen", arrayOf())
    object HomeScreen2 : Screens("home_screen2", arrayOf())
    object SettingsScreen : Screens("settings_screen", arrayOf())
    object NowPlayingScreen : Screens("now_playing", arrayOf())
    object SongsScreen : Screens("songs_screen", arrayOf())
    object AlbumsScreen : Screens("albums_screen", arrayOf())
    object ArtistsScreen : Screens("artists_screen", arrayOf())
    object PlaylistsScreen : Screens("playlists_screen", arrayOf())
    object SongsListInfoScreen : Screens("songs_list_info_screen", arrayOf())

    fun withArgs(vararg args : String) : String {
        return buildString {
            append(route)
            args.forEach {
                append("/$it")
            }
        }
    }

    fun routeWithArgs() : String {
        return buildString {
            append(route)
            args.forEach {
                append("/{$it}")
            }
        }
    }
}
