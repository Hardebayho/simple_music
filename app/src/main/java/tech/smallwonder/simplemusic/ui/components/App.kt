package tech.smallwonder.simplemusic.ui.components

import android.Manifest
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.MainActivity
import tech.smallwonder.simplemusic.R
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.models.BottomNavItem
import tech.smallwonder.simplemusic.models.MusicModel

@ExperimentalPermissionsApi
@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun App() {
    var doNotShowRationale by rememberSaveable {
        mutableStateOf(false)
    }

    val storagePermissionState = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    PermissionRequired(
        permissionState = storagePermissionState,
        permissionNotGrantedContent = {
              if (doNotShowRationale) {
                  Text(
                      text = "Storage Permission Denied. Storage permission is needed to allow SimpleMusic read music files from your storage. Please grant us access on the Settings screen.",
                      modifier = Modifier
                          .fillMaxSize(),
                      textAlign = TextAlign.Center,
                  )
              } else {
                  Column(
                      verticalArrangement = Arrangement.Center,
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier
                          .fillMaxSize(),
                  ) {
                      Text(
                          text = "The Storage Permission is important for this app. Please grant the permission.",
                          modifier = Modifier
                              .fillMaxWidth(),
                          textAlign = TextAlign.Center,
                      )

                      Spacer(modifier = Modifier.height(8.dp))
                      Row {
                          Button(
                              onClick = {
                                  storagePermissionState.launchPermissionRequest()
                              }
                          ) {
                              Text("Okay")
                          }
                          Spacer(Modifier.width(8.dp))
                          Button(onClick = {
                              doNotShowRationale = true
                          }) {
                              Text("Nope")
                          }
                      }
                  }
              }
        },
        permissionNotAvailableContent = {
            Column {
                Text(
                    "Feature Not Available"
                )
            }
        }) {
        AppContent()
    }

}

@OptIn(ExperimentalAnimationApi::class)
@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun AppContent() {
    val navController = rememberNavController()
    var validRoute by remember { mutableStateOf(isValidRoute(navController)) }

    navController.addOnDestinationChangedListener { _, _, _ ->
        validRoute = isValidRoute(navController)
    }

    val model = LocalModel.current

    val primaryDark by model.primaryDark.observeAsState()

    val sheetContent by model.sheetContent

    ModalBottomSheetLayout(
        sheetContent = sheetContent,
        sheetState = model.sheetState,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        sheetShape = RoundedCornerShape(16.dp),
        sheetElevation = 4.dp,
    ) {
        Scaffold(
            bottomBar = {
                if (validRoute) {
                    BottomNav(
                        items = listOf(
                            BottomNavItem(
                                Screens.HomeScreen.routeWithArgs(),
                                "Home",
                                Icons.Rounded.Home,
                            )
                        ),
                        navController = navController,
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(primaryDark!!)
                    .padding(it)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screens.SplashScreen.routeWithArgs(),
                ) {
                    composable(Screens.SplashScreen.routeWithArgs()) {
                        SplashPage(navController)
                    }
                    composable(Screens.HomeScreen.routeWithArgs()) {
                        AppScaffold()
                    }
                    composable(Screens.SettingsScreen.routeWithArgs()) {
                        SettingsPage()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@ExperimentalPagerApi
@Composable
fun AppScaffold() {
    val controller = rememberNavController()

    val startDestination = Screens.HomeScreen2

    LaunchedEffect(key1 = MainActivity.currentIntent!!.action == "OPEN_NOW_PLAYING") {
        if (MainActivity.currentIntent!!.action == "OPEN_NOW_PLAYING") {
            controller.navigate(
                Screens.NowPlayingScreen.routeWithArgs(),
            ) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(
        navController = controller,
        startDestination = startDestination.routeWithArgs(),
    ) {
        composable(Screens.HomeScreen2.routeWithArgs()) {
            HomePage(controller)
        }

        composable(Screens.SongsScreen.routeWithArgs()) {
            SongsPage(controller)
        }

        composable(Screens.NowPlayingScreen.routeWithArgs()) {
            NowPlayingPage(controller)
        }

        composable(Screens.AlbumsScreen.routeWithArgs()) {
            AlbumsPage(controller)
        }

        composable(Screens.ArtistsScreen.routeWithArgs()) {
            ArtistsPage(controller)
        }

        composable(Screens.PlaylistsScreen.routeWithArgs()) {
            PlaylistsPage(controller)
        }

        composable(Screens.SongsListInfoScreen.routeWithArgs()) {
            SongsListInfo(controller)
        }
    }
}

fun isValidRoute(navController: NavHostController) : Boolean {
    return navController.currentBackStackEntry?.destination?.route != Screens.SplashScreen.route
}