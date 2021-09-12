package tech.smallwonder.simplemusic.ui.components

import android.os.CountDownTimer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.delay
import tech.smallwonder.simplemusic.R
import tech.smallwonder.simplemusic.Screens

@Composable
fun SplashPage(navController: NavHostController ) {
    LaunchedEffect(true) {
        delay(2000)
        navController.popBackStack("", true)
        navController.navigate(Screens.HomeScreen.routeWithArgs()) {
            popUpTo(Screens.SplashScreen.routeWithArgs()) {
                inclusive = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = "Application Logo",
            modifier = Modifier
                .size(120.dp),
            colorFilter = ColorFilter.tint(Color.White),
        )
        Text(
            text = "Simple Music",
            style = MaterialTheme.typography.h3,
            textAlign = TextAlign.Center,
        )
    }
}