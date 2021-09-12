package tech.smallwonder.simplemusic.ui.components

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.models.BottomNavItem

@ExperimentalMaterialApi
@Composable
fun BottomNav(
    modifier: Modifier = Modifier,
    items: List<BottomNavItem>,
    navController: NavHostController,
) {
    val backstackEntry = navController.currentBackStackEntryAsState()
    BottomNavigation(
        modifier = modifier,
        elevation = 0.dp,
        backgroundColor = Color.Transparent,
        contentColor = Color.Transparent,
    ) {
        items.forEach { item ->
            val selected = item.route == backstackEntry.value?.destination?.route

            val primaryLight = LocalModel.current.properLight

            BottomNavigationItem(
                selectedContentColor = Color.White,
                unselectedContentColor = Color.Gray,
                selected = selected,
                alwaysShowLabel = false,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(backstackEntry.value?.destination?.route!!) {
                                saveState = true
                                inclusive = backstackEntry.value?.destination?.route != Screens.HomeScreen.routeWithArgs()
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        tint = primaryLight,
                        modifier = if (selected) {
                            Modifier
                                .shadow(elevation = 8.dp)
                        } else {
                            Modifier
                        },
                        imageVector = item.icon,
                        contentDescription = item.name,
                    )
                },
                label = {
                    Text(
                        item.name,
                        textAlign = TextAlign.Center,
                        color = primaryLight,
                    )
                }
            )
        }
    }
}